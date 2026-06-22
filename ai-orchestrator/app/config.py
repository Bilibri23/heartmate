"""Environment-driven configuration for the orchestrator.

All settings are optional with safe defaults so the service can boot (and tests can
run) without a live model or backend. Provider credentials are read from the same
env names Spring uses, so the sidecar talks to the same OpenAI/Ollama backend.
"""

from __future__ import annotations

from functools import lru_cache

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", extra="ignore")

    # --- RoomBay Spring backend (tool calls go here; Spring stays the boundary) ---
    roombay_api_base_url: str = "http://localhost:8082"
    # Shared secret echoed on every internal tool call. Spring rejects mismatches.
    roombay_internal_token: str = ""
    tool_timeout_ms: int = 8000

    # --- LLM provider (mirrors Spring's AiModelRouter selection) ---
    # "openai" | "ollama" | "stub". "stub" produces deterministic text with no
    # network calls — used by tests and as a safe default when nothing is set.
    ai_provider: str = "stub"
    openai_api_key: str = ""
    openai_base_url: str = "https://api.openai.com/v1"
    openai_chat_model: str = "gpt-4o-mini"
    ollama_base_url: str = "http://localhost:11434"
    ollama_chat_model: str = "llama3.1"
    llm_timeout_ms: int = 60000

    # --- Feedback-loop tuning (each retry capped to keep latency bounded) ---
    grounding_retry_cap: int = 1
    listing_retry_cap: int = 1
    self_check_regen_cap: int = 1
    # Minimum best chunk score (0..1) below which retrieval is "weak".
    grounding_min_score: float = 0.2

    # --- Service ---
    port: int = 8131
    log_level: str = "INFO"

    @property
    def provider(self) -> str:
        """Effective provider, auto-detecting OpenAI when only a key is present."""
        p = (self.ai_provider or "").strip().lower()
        if p in {"openai", "ollama", "stub"}:
            return p
        if self.openai_api_key:
            return "openai"
        return "stub"


@lru_cache
def get_settings() -> Settings:
    return Settings()
