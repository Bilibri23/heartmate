"""FastAPI entrypoint for the RoomBay AI Orchestrator.

Exposes:
  GET  /health      — liveness + provider/config summary
  POST /orchestrate — run the LangGraph agent, return an AiChatResponse-shaped body

Spring's `AiOrchestratorClient` calls /orchestrate when the orchestrator feature
flag is on, and falls back to its own pipeline if this service errors or times out.
"""

from __future__ import annotations

import logging
import time
import uuid

from fastapi import Depends, FastAPI

from .auth import require_internal_token
from .config import get_settings
from .graph import build_graph
from .llm import get_llm
from .nodes import State
from .roombay_client import get_tools
from .schemas import (
    Citation,
    OrchestrateMeta,
    OrchestrateRequest,
    OrchestrateResponse,
    ToolExecution,
)

logging.basicConfig(level=get_settings().log_level)
logger = logging.getLogger("roombay.orchestrator")

app = FastAPI(title="RoomBay AI Orchestrator", version="0.1.0")

# Build once at startup; the compiled graph is stateless across requests.
_settings = get_settings()
_graph = build_graph(get_tools(), get_llm(), _settings)


@app.get("/health")
def health() -> dict:
    return {
        "status": "ok",
        "provider": _settings.provider,
        "backend": _settings.roombay_api_base_url,
        "loops": {
            "groundingRetryCap": _settings.grounding_retry_cap,
            "listingRetryCap": _settings.listing_retry_cap,
            "selfCheckRegenCap": _settings.self_check_regen_cap,
        },
    }


@app.post("/orchestrate", response_model=OrchestrateResponse, dependencies=[Depends(require_internal_token)])
def orchestrate(req: OrchestrateRequest) -> OrchestrateResponse:
    started = time.monotonic()
    thread_id = req.threadId or str(uuid.uuid4())
    request_id = req.requestId or str(uuid.uuid4())

    initial: State = {
        "message": req.message,
        "persona": req.persona,
        "user_id": req.userId,
        "role": req.userRole,
        "request_id": request_id,
        "thread_id": thread_id,
        "grounding_retries": 0,
        "listing_retries": 0,
        "relax_level": 0,
        "regenerated": False,
    }

    final: State = _graph.invoke(initial)

    latency_ms = int((time.monotonic() - started) * 1000)
    response = _to_response(final, thread_id, latency_ms)
    logger.info(
        "orchestrate intent=%s grounded=%s listings=%d retries(g=%d,l=%d) regen=%s %dms",
        final.get("intent"),
        response.ragGrounded,
        len(response.listingResults),
        final.get("grounding_retries", 0),
        final.get("listing_retries", 0),
        final.get("regenerated", False),
        latency_ms,
    )
    return response


def _to_response(state: State, thread_id: str, latency_ms: int) -> OrchestrateResponse:
    citations = state.get("citations") or []
    if citations and not isinstance(citations[0], Citation):  # safety for raw dicts
        citations = [Citation(**c) for c in citations]
    return OrchestrateResponse(
        answer=state.get("draft") or "",
        threadId=thread_id,
        citations=citations,
        suggestedActions=state.get("suggested_actions") or [],
        listingResults=state.get("listings") or [],
        ragGrounded=bool(state.get("grounded", False)),
        meta=OrchestrateMeta(
            intent=state.get("intent"),
            groundingRetries=state.get("grounding_retries", 0),
            listingRetries=state.get("listing_retries", 0),
            regenerated=state.get("regenerated", False),
            safetyBlocked=state.get("safety_blocked", False),
            latencyMs=latency_ms,
        ),
        toolExecutions=_tool_executions(state),
    )


def _tool_executions(state: State) -> list[ToolExecution]:
    raw = state.get("tool_executions") or []
    out: list[ToolExecution] = []
    for item in raw:
        if isinstance(item, ToolExecution):
            out.append(item)
        elif isinstance(item, dict):
            out.append(ToolExecution(**item))
    return out
