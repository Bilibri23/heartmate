"""Typed HTTP client for RoomBay's Spring internal tool endpoints.

Every call carries the internal service token plus the security context
(userId / userRole / requestId). Spring re-enforces role on each endpoint, so the
orchestrator cannot widen its own access. Read tools fetch context; write tools
are allowlisted and scoped to the supplied tenant userId.

On any error the client returns an empty result rather than raising: a degraded
tool must not crash the graph (Spring's own fallback + output guard remain).
"""

from __future__ import annotations

import logging
from typing import Protocol

import httpx

from .config import Settings, get_settings
from .schemas import ExtractedFilters, ListingResult, Preferences, RetrievedChunk, ToolExecution

logger = logging.getLogger("roombay.orchestrator.tools")


class RoomBayTools(Protocol):
    def retrieve(
        self, *, message: str, user_id: str | None, role: str, request_id: str | None
    ) -> list[RetrievedChunk]: ...

    def search_listings(
        self,
        *,
        filters: ExtractedFilters,
        user_id: str | None,
        role: str,
        request_id: str | None,
    ) -> list[ListingResult]: ...

    def preferences(
        self, *, user_id: str | None, role: str, request_id: str | None
    ) -> Preferences: ...

    def execute_tool(
        self,
        *,
        tool: str,
        params: dict,
        user_id: str | None,
        role: str,
        request_id: str | None,
    ) -> ToolExecution: ...


class HttpRoomBayClient:
    def __init__(self, settings: Settings | None = None) -> None:
        self.settings = settings or get_settings()
        self._timeout = self.settings.tool_timeout_ms / 1000

    def _headers(self, request_id: str | None) -> dict[str, str]:
        headers = {"X-RoomBay-Internal-Token": self.settings.roombay_internal_token}
        if request_id:
            headers["X-RoomBay-Request-Id"] = request_id
        return headers

    def _url(self, path: str) -> str:
        return f"{self.settings.roombay_api_base_url.rstrip('/')}{path}"

    def _post(self, path: str, body: dict, request_id: str | None) -> dict:
        resp = httpx.post(
            self._url(path),
            headers=self._headers(request_id),
            json=body,
            timeout=self._timeout,
        )
        resp.raise_for_status()
        return resp.json() or {}

    def retrieve(
        self, *, message: str, user_id: str | None, role: str, request_id: str | None
    ) -> list[RetrievedChunk]:
        try:
            data = self._post(
                "/internal/ai/retrieve",
                {"message": message, "userId": user_id, "role": role, "requestId": request_id},
                request_id,
            )
            return [RetrievedChunk(**c) for c in data.get("chunks", [])]
        except Exception as exc:
            logger.warning("retrieve tool failed: %s", exc)
            return []

    def search_listings(
        self,
        *,
        filters: ExtractedFilters,
        user_id: str | None,
        role: str,
        request_id: str | None,
    ) -> list[ListingResult]:
        try:
            data = self._post(
                "/internal/ai/listings/search",
                {
                    "filters": filters.model_dump(exclude_none=True),
                    "userId": user_id,
                    "role": role,
                    "requestId": request_id,
                },
                request_id,
            )
            return [ListingResult(**l) for l in data.get("listings", [])]
        except Exception as exc:
            logger.warning("listing search tool failed: %s", exc)
            return []

    def preferences(
        self, *, user_id: str | None, role: str, request_id: str | None
    ) -> Preferences:
        try:
            data = self._post(
                "/internal/ai/preferences",
                {"userId": user_id, "role": role, "requestId": request_id},
                request_id,
            )
            return Preferences(**data)
        except Exception as exc:
            logger.warning("preferences tool failed: %s", exc)
            return Preferences()

    def execute_tool(
        self,
        *,
        tool: str,
        params: dict,
        user_id: str | None,
        role: str,
        request_id: str | None,
    ) -> ToolExecution:
        try:
            data = self._post(
                "/internal/ai/tools/execute",
                {
                    "tool": tool,
                    "params": params,
                    "userId": user_id,
                    "role": role,
                    "requestId": request_id,
                },
                request_id,
            )
            return ToolExecution(
                tool=str(data.get("tool") or tool),
                success=bool(data.get("success")),
                message=str(data.get("message") or ""),
                entityId=data.get("entityId") or None,
            )
        except Exception as exc:
            logger.warning("execute_tool %s failed: %s", tool, exc)
            return ToolExecution(tool=tool, success=False, message="Action could not be completed")


def get_tools() -> RoomBayTools:
    return HttpRoomBayClient()
