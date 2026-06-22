"""Minimal LLM provider abstraction.

Supports OpenAI-compatible and Ollama chat endpoints, plus a "stub" provider that
returns deterministic text with no network calls. The orchestrator only needs the
model for free-text generation and self-critique; intent classification and filter
extraction are rule-based (see nodes), so the whole graph stays runnable — and
tests stay deterministic — in stub mode.
"""

from __future__ import annotations

import logging

import httpx

from .config import Settings, get_settings

logger = logging.getLogger("roombay.orchestrator.llm")


class LLM:
    def __init__(self, settings: Settings | None = None) -> None:
        self.settings = settings or get_settings()
        self.provider = self.settings.provider

    @property
    def is_stub(self) -> bool:
        return self.provider == "stub"

    def complete(self, system: str, user: str) -> str:
        """Return a model completion, or a safe fallback string on any error."""
        try:
            if self.provider == "openai":
                return self._openai(system, user)
            if self.provider == "ollama":
                return self._ollama(system, user)
            return self._stub(system, user)
        except Exception as exc:  # never let a model error crash the graph
            logger.warning("LLM completion failed (%s): %s", self.provider, exc)
            return self._stub(system, user)

    # --- providers ---

    def _openai(self, system: str, user: str) -> str:
        timeout = self.settings.llm_timeout_ms / 1000
        resp = httpx.post(
            f"{self.settings.openai_base_url.rstrip('/')}/chat/completions",
            headers={"Authorization": f"Bearer {self.settings.openai_api_key}"},
            json={
                "model": self.settings.openai_chat_model,
                "temperature": 0.2,
                "messages": [
                    {"role": "system", "content": system},
                    {"role": "user", "content": user},
                ],
            },
            timeout=timeout,
        )
        resp.raise_for_status()
        return resp.json()["choices"][0]["message"]["content"].strip()

    def _ollama(self, system: str, user: str) -> str:
        timeout = self.settings.llm_timeout_ms / 1000
        resp = httpx.post(
            f"{self.settings.ollama_base_url.rstrip('/')}/api/chat",
            json={
                "model": self.settings.ollama_chat_model,
                "stream": False,
                "options": {"temperature": 0.2},
                "messages": [
                    {"role": "system", "content": system},
                    {"role": "user", "content": user},
                ],
            },
            timeout=timeout,
        )
        resp.raise_for_status()
        return resp.json()["message"]["content"].strip()

    def _stub(self, system: str, user: str) -> str:
        # Deterministic, offline. Callers that need grounded output compose it
        # themselves from tool results; this is only a last-resort generator.
        return (
            "I can help with that using RoomBay's information. "
            "Next step: tell me your city, budget, and what kind of place you need."
        )


def get_llm() -> LLM:
    return LLM()
