"""Inbound authentication for the orchestrator HTTP API.

Spring's AiOrchestratorClient must present the same shared secret used for
outbound /internal/ai/* tool calls. Without a matching token, /orchestrate
rejects the request so arbitrary callers cannot spoof userId/role context.
"""

from __future__ import annotations

import secrets

from fastapi import Header, HTTPException, status

from .config import get_settings

INTERNAL_TOKEN_HEADER = "X-RoomBay-Internal-Token"


def require_internal_token(
    x_roombay_internal_token: str | None = Header(default=None, alias=INTERNAL_TOKEN_HEADER),
) -> None:
    """FastAPI dependency: closed by default when ROOMBAY_INTERNAL_TOKEN is unset."""
    expected = get_settings().roombay_internal_token
    if not expected or not expected.strip():
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Orchestrator internal token not configured",
        )
    if not x_roombay_internal_token or not secrets.compare_digest(
        expected, x_roombay_internal_token
    ):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid internal token",
        )
