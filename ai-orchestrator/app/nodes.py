"""LangGraph nodes for the RoomBay orchestrator.

Intent classification and filter extraction are rule-based (mirroring the existing
Java `AiCopilotToolService` regex approach) so the graph is deterministic and
testable without a live model. The LLM is used only for free-text generation and
self-critique; in stub mode generation is composed deterministically from tool
results.

The three feedback loops live here as state flags the graph's conditional edges
read: grounding retry, listing-search retry, and self-check/safety reflect.
"""

from __future__ import annotations

import re
from typing import Any, TypedDict

from .config import Settings, get_settings
from .llm import LLM
from .roombay_client import RoomBayTools
from .schemas import (
    Citation,
    ExtractedFilters,
    ListingResult,
    Preferences,
    RetrievedChunk,
    SuggestedAction,
)

# --- Lightweight Cameroon geography + taxonomy lexicons for rule-based parsing ---

CITIES = {
    "yaounde": "Yaoundé", "yaoundé": "Yaoundé", "douala": "Douala", "buea": "Buea",
    "bamenda": "Bamenda", "bafoussam": "Bafoussam", "limbe": "Limbe", "kribi": "Kribi",
    "garoua": "Garoua", "maroua": "Maroua", "bertoua": "Bertoua", "ngaoundere": "Ngaoundéré",
}
NEIGHBORHOODS = [
    "damas", "melen", "ngoa-ekelle", "ngoa ekelle", "bastos", "biyem-assi", "biyem assi",
    "mvan", "mvog-ada", "nsam", "essos", "mendong", "etoa-meki", "obili", "nkolbisson",
    "bonamoussadi", "bonapriso", "akwa", "bonaberi", "deido", "makepe", "logpom", "ndogbong",
]
PROPERTY_TYPES = {
    "studio": "STUDIO",
    "apartment": "APARTMENT", "appartement": "APARTMENT", "flat": "APARTMENT",
    "house": "HOUSE", "maison": "HOUSE", "villa": "HOUSE", "duplex": "HOUSE",
    "chambre": "ROOM", "room": "ROOM",
}


class State(TypedDict, total=False):
    # inputs
    message: str
    persona: str
    user_id: str | None
    role: str
    request_id: str | None
    thread_id: str | None
    # working
    intent: str
    filters: ExtractedFilters
    chunks: list[RetrievedChunk]
    grounded: bool
    listings: list[ListingResult]
    prefs: Preferences
    draft: str
    citations: list[Citation]
    suggested_actions: list[SuggestedAction]
    # loop control
    grounding_retries: int
    listing_retries: int
    regenerated: bool
    relax_level: int
    safety_blocked: bool
    self_check_ok: bool


def _lc(text: str) -> str:
    return (text or "").lower()


class Nodes:
    """Graph nodes bound to a tool client + LLM (injectable for tests)."""

    def __init__(self, tools: RoomBayTools, llm: LLM, settings: Settings | None = None) -> None:
        self.tools = tools
        self.llm = llm
        self.settings = settings or get_settings()

    # -- classify_intent --------------------------------------------------
    def classify_intent(self, state: State) -> dict[str, Any]:
        msg = _lc(state["message"])
        persona = state.get("persona", "TENANT")
        intent = "knowledge_question"
        if re.search(r"\b(scam|fraud|bypass|fake id|launder|hack)\b", msg):
            intent = "unsafe_or_restricted_request"
        elif re.search(r"room preview|preview (this |the )?room|visualize", msg):
            intent = "room_preview_help"
        elif re.search(r"roommate|colocataire|compatib|shared room|colocation", msg) and not re.search(
            r"\b(find|show|need|want|looking)\b", msg
        ):
            intent = "roommate_matching_help"
        elif re.search(r"\bapply|application|how (do|to) i apply\b", msg):
            intent = "application_help"
        elif persona == "ADMIN":
            intent = "admin_help"
        elif self._looks_like_listing_search(msg):
            intent = "listing_search"
        elif persona == "LANDLORD" and re.search(r"listing|views|improve|performance", msg):
            intent = "landlord_help"
        return {"intent": intent}

    @staticmethod
    def _looks_like_listing_search(msg: str) -> bool:
        triggers = (
            r"\b(studio|apartment|appartement|house|maison|room|chambre|villa|flat)\b",
            r"\b(rent|buy|sale|louer|acheter|find|show|near|budget|shared|furnished|meubl)\b",
        )
        return any(re.search(t, msg) for t in triggers)

    # -- extract_filters --------------------------------------------------
    def extract_filters(self, state: State) -> dict[str, Any]:
        return {"filters": self._extract(state["message"], state.get("relax_level", 0))}

    def _extract(self, message: str, relax: int) -> ExtractedFilters:
        msg = _lc(message)
        f = ExtractedFilters(query=message)

        for kw, val in PROPERTY_TYPES.items():
            if re.search(rf"\b{re.escape(kw)}\b", msg):
                f.propertyType = val
                break
        for hood in NEIGHBORHOODS:
            if hood in msg:
                f.neighborhood = hood.title()
                break
        for key, name in CITIES.items():
            if key in msg:
                f.city = name
                break

        if re.search(r"\b(sale|buy|acheter|vendre|for sale)\b", msg):
            f.listingPurpose = "SALE"
        else:
            f.listingPurpose = "RENT"
        if re.search(r"short[- ]?term|nightly|courte dur|short stay", msg):
            f.stayType = "SHORT_TERM"
        elif re.search(r"long[- ]?term|longue dur", msg):
            f.stayType = "LONG_TERM"
        if re.search(r"semi[- ]?furnished|semi[- ]?meubl", msg):
            f.furnishingStatus = "SEMI_FURNISHED"
        elif re.search(r"unfurnished|non[- ]?meubl", msg):
            f.furnishingStatus = "UNFURNISHED"
        elif re.search(r"furnished|meubl", msg):
            f.furnishingStatus = "FURNISHED"
        if re.search(r"shared|colocation|roommate|colocataire", msg):
            f.sharedAccommodation = True
            if not f.propertyType:
                f.propertyType = "SHARED_ROOM"

        self._parse_budget(msg, f)

        # Relaxation on listing-search retry: drop the most restrictive filters.
        if relax >= 1:
            f.neighborhood = None
            f.furnishingStatus = None
        if relax >= 2:
            f.maxBudget = None
            f.minBudget = None
            f.stayType = None
        return f

    @staticmethod
    def _parse_budget(msg: str, f: ExtractedFilters) -> None:
        def to_int(num: str) -> int:
            return int(float(num))

        # explicit range e.g. "50000-120000" or "50k to 120k"
        rng = re.search(r"(\d[\d,\.]*)\s*k?\s*(?:-|to|–|à)\s*(\d[\d,\.]*)\s*k?", msg)
        if rng:
            lo = to_int(rng.group(1).replace(",", ""))
            hi = to_int(rng.group(2).replace(",", ""))
            if "k" in msg[rng.start():rng.end()]:
                lo, hi = lo * 1000, hi * 1000
            f.minBudget, f.maxBudget = min(lo, hi), max(lo, hi)
            return
        for m in re.finditer(r"(under|below|max|less than|<|above|over|min|more than|>)\s*(\d[\d,\.]*)\s*(k)?", msg):
            word, num, k = m.group(1), to_int(m.group(2).replace(",", "")), m.group(3)
            if k:
                num *= 1000
            if word in {"above", "over", "min", "more than", ">"}:
                f.minBudget = num
            else:
                f.maxBudget = num

    # -- check_role_permissions ------------------------------------------
    def check_role_permissions(self, state: State) -> dict[str, Any]:
        # Spring re-enforces on every tool call; this blocks obviously cross-role
        # intents early (e.g. a tenant asking for the admin queue).
        role = state.get("role", "STUDENT")
        intent = state.get("intent")
        if intent == "admin_help" and role != "ADMIN":
            return {"intent": "knowledge_question"}
        return {}

    # -- retrieve_docs (RAG/GraphRAG via Spring) -------------------------
    def retrieve_docs(self, state: State) -> dict[str, Any]:
        chunks = self.tools.retrieve(
            message=state["message"],
            user_id=state.get("user_id"),
            role=state.get("role", "STUDENT"),
            request_id=state.get("request_id"),
        )
        best = max((c.score for c in chunks), default=0.0)
        grounded = bool(chunks) and best >= self.settings.grounding_min_score
        citations = [
            Citation(chunkId=c.chunkId, source=c.source, title=c.title) for c in chunks[:6]
        ]
        return {"chunks": chunks, "grounded": grounded, "citations": citations}

    def broaden_for_grounding(self, state: State) -> dict[str, Any]:
        return {"grounding_retries": state.get("grounding_retries", 0) + 1}

    # -- search_listings --------------------------------------------------
    def search_listings(self, state: State) -> dict[str, Any]:
        if state.get("intent") != "listing_search":
            return {"listings": []}
        listings = self.tools.search_listings(
            filters=state.get("filters", ExtractedFilters()),
            user_id=state.get("user_id"),
            role=state.get("role", "STUDENT"),
            request_id=state.get("request_id"),
        )
        return {"listings": listings}

    def relax_and_retry_listings(self, state: State) -> dict[str, Any]:
        new_relax = state.get("relax_level", 0) + 1
        return {
            "listing_retries": state.get("listing_retries", 0) + 1,
            "relax_level": new_relax,
            "filters": self._extract(state["message"], new_relax),
        }

    # -- fetch_user_preferences ------------------------------------------
    def fetch_user_preferences(self, state: State) -> dict[str, Any]:
        if state.get("role") not in {"STUDENT", "TENANT"}:
            return {"prefs": Preferences()}
        prefs = self.tools.preferences(
            user_id=state.get("user_id"),
            role=state.get("role", "STUDENT"),
            request_id=state.get("request_id"),
        )
        return {"prefs": prefs}

    # -- rank_results -----------------------------------------------------
    def rank_results(self, state: State) -> dict[str, Any]:
        listings = state.get("listings", [])
        if not listings:
            return {}
        prefs = state.get("prefs", Preferences())
        filters = state.get("filters", ExtractedFilters())
        scored = [(self._score_listing(l, prefs, filters), l) for l in listings]
        scored.sort(key=lambda t: t[0][0], reverse=True)
        ranked: list[ListingResult] = []
        for (score, why, used_pref), listing in scored:
            listing.whyThisMatches = why
            listing.matchReason = "; ".join(why) if why else "Matches your search"
            if used_pref:
                listing.matchLabel = "Strong match based on your saved preferences" if score >= 4 else "Preference match"
            else:
                listing.matchLabel = "Search match"
            listing.actions = self._listing_ctas(listing)
            ranked.append(listing)
        return {"listings": ranked[:5]}

    @staticmethod
    def _score_listing(
        l: ListingResult, prefs: Preferences, f: ExtractedFilters
    ) -> tuple[int, list[str], bool]:
        score = 0
        why: list[str] = []
        used_pref = False
        ptype = f.propertyType or (prefs.propertyTypes[0] if prefs.propertyTypes else None)
        if ptype and l.propertyType == ptype:
            score += 2
            why.append(f"Property type matches ({l.propertyType})")
            if prefs.propertyTypes and l.propertyType in prefs.propertyTypes:
                used_pref = True
        target_loc = (f.neighborhood or f.city or "").lower()
        if target_loc and (target_loc in _lc(l.neighborhood or "") or target_loc in _lc(l.city or "")):
            score += 2
            why.append(f"Location matches ({l.neighborhood or l.city})")
        elif prefs.preferredLocations and any(
            loc.lower() in _lc(l.neighborhood or "") or loc.lower() in _lc(l.city or "")
            for loc in prefs.preferredLocations
        ):
            score += 2
            used_pref = True
            why.append("In one of your preferred areas")
        budget_max = f.maxBudget or prefs.maxBudget
        if budget_max and l.rentAmount and l.rentAmount <= budget_max:
            score += 1
            why.append("Within budget")
            if prefs.maxBudget and not f.maxBudget:
                used_pref = True
        if l.verified:
            score += 1
            why.append("Verified listing")
        if l.roomPreviewEnabled and l.roomPreviewStatus == "APPROVED":
            score += 1
            why.append("Room Preview available")
        return score, why, used_pref

    @staticmethod
    def _listing_ctas(l: ListingResult) -> list[SuggestedAction]:
        url = f"/listings/{l.id}" if l.id else None
        save = SuggestedAction(id="save", label="Save", type="NAVIGATE", actionUrl=url)
        if l.listingPurpose == "SALE":
            return [
                SuggestedAction(id="contact", label="Contact landlord", type="NAVIGATE", actionUrl=url),
                save,
                SuggestedAction(id="view", label="View details", type="NAVIGATE", actionUrl=url),
            ]
        if l.stayType == "SHORT_TERM":
            return [
                SuggestedAction(id="view", label="View listing", type="NAVIGATE", actionUrl=url),
                save,
                SuggestedAction(id="request", label="Request stay", type="NAVIGATE", actionUrl=url),
            ]
        return [
            SuggestedAction(id="view", label="View listing", type="NAVIGATE", actionUrl=url),
            save,
            SuggestedAction(id="apply", label="Apply", type="NAVIGATE", actionUrl=url),
        ]

    # -- generate_response ------------------------------------------------
    def generate_response(self, state: State) -> dict[str, Any]:
        if self.llm.is_stub:
            return {"draft": self._compose_grounded(state)}
        system = (
            "You are RoomBay's assistant for Cameroon housing. Answer ONLY from the "
            "provided RoomBay context and listings. Be concise (2-5 sentences) and end "
            "with a line starting 'Next step:'. Never invent prices, contacts, or policies. "
            "Never call a SALE listing a rental."
        )
        ctx = self._context_block(state)
        draft = self.llm.complete(system, f"User: {state['message']}\n\n{ctx}")
        return {"draft": draft or self._compose_grounded(state)}

    def regenerate(self, state: State) -> dict[str, Any]:
        # Mark a regeneration; generate_response will run again via the edge.
        return {"regenerated": True}

    def _context_block(self, state: State) -> str:
        parts = []
        chunks = state.get("chunks", [])
        if chunks:
            parts.append("RoomBay context:\n" + "\n".join(f"- {c.text}" for c in chunks[:5]))
        listings = state.get("listings", [])
        if listings:
            parts.append(
                "Matching listings:\n"
                + "\n".join(f"- {l.title} ({l.neighborhood or l.city})" for l in listings[:5])
            )
        return "\n\n".join(parts) if parts else "No RoomBay context was found."

    @staticmethod
    def _compose_grounded(state: State) -> str:
        listings = state.get("listings", [])
        chunks = state.get("chunks", [])
        if listings:
            top = ", ".join(f"{l.title} in {l.neighborhood or l.city or 'Cameroon'}" for l in listings[:3])
            return (
                f"I found {len(listings)} RoomBay listing(s) matching your search: {top}. "
                "Next step: open a listing to view details, save it, or message the landlord."
            )
        if chunks:
            snippet = (chunks[0].text or "").strip().replace("\n", " ")
            snippet = (snippet[:240] + "…") if len(snippet) > 240 else snippet
            return f"Based on RoomBay's information: {snippet} Next step: ask me for specifics or browse listings."
        return (
            "I don't have specific RoomBay information for that yet. "
            "Next step: try a city, budget, and property type (e.g. 'studio in Damas under 80,000')."
        )

    # -- self_check (reflect) --------------------------------------------
    def self_check(self, state: State) -> dict[str, Any]:
        """Critique the draft. Sets self_check_ok; the graph edge decides on regen."""
        draft = (state.get("draft") or "").strip()
        ok = len(draft) >= 15
        # If we have listings but the answer ignored them, that's a weak answer.
        if state.get("listings") and "listing" not in draft.lower() and "found" not in draft.lower():
            ok = False
        return {"self_check_ok": ok}

    # -- safety_check (terminal gate; Spring also guards) ----------------
    def safety_check(self, state: State) -> dict[str, Any]:
        draft = state.get("draft") or ""
        if state.get("intent") == "unsafe_or_restricted_request":
            return {
                "draft": (
                    "I can't help with that. RoomBay won't assist with bypassing verification, "
                    "payments outside the platform, or anything unsafe. "
                    "Next step: ask me about finding or comparing verified listings."
                ),
                "safety_blocked": True,
                "listings": [],
                "citations": [],
            }
        # Light PII backstop; Spring's AiOutputGuard is the authoritative check.
        if re.search(r"\b(\+?237)?6\d{8}\b", draft) or re.search(r"[\w.+-]+@[\w-]+\.[\w.-]+", draft):
            cleaned = re.sub(r"\b(\+?237)?6\d{8}\b", "[contact hidden]", draft)
            cleaned = re.sub(r"[\w.+-]+@[\w-]+\.[\w.-]+", "[contact hidden]", cleaned)
            return {"draft": cleaned}
        return {}

    # -- prepare_actions --------------------------------------------------
    def prepare_actions(self, state: State) -> dict[str, Any]:
        actions = list(state.get("suggested_actions", []))
        intent = state.get("intent")
        if intent == "application_help":
            actions.append(
                SuggestedAction(id="apps", label="View my applications", type="NAVIGATE", actionUrl="/applications")
            )
        elif intent == "room_preview_help":
            actions.append(
                SuggestedAction(id="search", label="Browse listings", type="NAVIGATE", actionUrl="/search")
            )
        return {"suggested_actions": actions}
