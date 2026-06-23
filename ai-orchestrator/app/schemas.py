"""Pydantic models mirroring RoomBay's stable AI contract.

These intentionally match the Java DTOs so Spring can deserialize the orchestrator
response straight into `AiChatResponse` without a translation layer:

  AiChatRequest  -> OrchestrateRequest
  AiChatResponse -> OrchestrateResponse (answer, threadId, citations,
                    suggestedActions, listingResults, ragGrounded)
"""

from __future__ import annotations

from typing import Any, Optional

from pydantic import BaseModel, Field

Persona = str  # "TENANT" | "LANDLORD" | "ADMIN"
Role = str  # "STUDENT" | "LANDLORD" | "ADMIN"


class OrchestrateRequest(BaseModel):
    message: str
    persona: Persona = "TENANT"
    threadId: Optional[str] = None
    # Security context, supplied and trusted by Spring (never the client).
    userId: Optional[str] = None
    userRole: Role = "STUDENT"
    requestId: Optional[str] = None


class Citation(BaseModel):
    chunkId: Optional[str] = None
    source: Optional[str] = None
    title: Optional[str] = None


class SuggestedAction(BaseModel):
    id: Optional[str] = None
    label: str
    type: str = "NAVIGATE"  # NAVIGATE | COPY_TEXT
    actionUrl: Optional[str] = None
    copyText: Optional[str] = None


class ListingResult(BaseModel):
    id: Optional[str] = None
    title: Optional[str] = None
    rentAmount: Optional[int] = None
    city: Optional[str] = None
    neighborhood: Optional[str] = None
    propertyType: Optional[str] = None
    listingPurpose: Optional[str] = None
    stayType: Optional[str] = None
    pricePeriod: Optional[str] = None
    salePrice: Optional[int] = None
    furnishingStatus: Optional[str] = None
    isSharedAccommodation: Optional[bool] = None
    roommatesWanted: Optional[bool] = None
    availableRoommateSlots: Optional[int] = None
    roomPreviewEnabled: Optional[bool] = None
    roomPreviewStatus: Optional[str] = None
    verified: Optional[bool] = None
    status: Optional[str] = None
    available: Optional[bool] = None
    thumbnailUrl: Optional[str] = None
    landlordId: Optional[str] = None
    matchLabel: Optional[str] = None
    matchReason: Optional[str] = None
    whyThisMatches: list[str] = Field(default_factory=list)
    actions: list[SuggestedAction] = Field(default_factory=list)


class OrchestrateMeta(BaseModel):
    """Observability for the feedback loop. Spring may persist this (V54 log)."""

    intent: Optional[str] = None
    groundingRetries: int = 0
    listingRetries: int = 0
    regenerated: bool = False
    safetyBlocked: bool = False
    latencyMs: Optional[int] = None


class ToolExecution(BaseModel):
    tool: str
    success: bool = False
    message: str = ""
    entityId: Optional[str] = None


class OrchestrateResponse(BaseModel):
    answer: str
    threadId: Optional[str] = None
    citations: list[Citation] = Field(default_factory=list)
    suggestedActions: list[SuggestedAction] = Field(default_factory=list)
    listingResults: list[ListingResult] = Field(default_factory=list)
    ragGrounded: bool = False
    meta: OrchestrateMeta = Field(default_factory=OrchestrateMeta)
    toolExecutions: list[ToolExecution] = Field(default_factory=list)


# --- Internal tool payloads (orchestrator -> Spring /internal/ai/*) ---


class RetrievedChunk(BaseModel):
    chunkId: Optional[str] = None
    source: Optional[str] = None
    title: Optional[str] = None
    text: str = ""
    score: float = 0.0


class Preferences(BaseModel):
    minBudget: Optional[int] = None
    maxBudget: Optional[int] = None
    preferredLocations: list[str] = Field(default_factory=list)
    propertyTypes: list[str] = Field(default_factory=list)


class ExtractedFilters(BaseModel):
    """Listing-search filters parsed from the user message (relaxable on retry)."""

    propertyType: Optional[str] = None
    city: Optional[str] = None
    neighborhood: Optional[str] = None
    minBudget: Optional[int] = None
    maxBudget: Optional[int] = None
    furnishingStatus: Optional[str] = None
    stayType: Optional[str] = None
    listingPurpose: Optional[str] = None
    sharedAccommodation: Optional[bool] = None
    query: Optional[str] = None
