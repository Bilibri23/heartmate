"""Graph-level tests. All Spring tools are faked; the LLM runs in stub mode unless
a test injects its own, so everything here is deterministic and offline."""

from __future__ import annotations

from app.config import get_settings
from app.graph import build_graph
from app.llm import LLM
from app.nodes import Nodes
from app.roombay_client import RoomBayTools
from app.schemas import ExtractedFilters, ListingResult, Preferences, RetrievedChunk


def _listing(**kw) -> ListingResult:
    base = dict(
        id="L1", title="Cozy studio", city="Yaoundé", neighborhood="Damas",
        propertyType="STUDIO", listingPurpose="RENT", stayType="LONG_TERM",
        rentAmount=70000, verified=True, status="ACTIVE", available=True,
    )
    base.update(kw)
    return ListingResult(**base)


class FakeTools:
    def __init__(self, *, chunks=None, listings=None, prefs=None,
                 chunks_sequence=None, listings_when_no_neighborhood=False,
                 tool_result=None):
        self._chunks = chunks or []
        self._listings = listings or []
        self._prefs = prefs or Preferences()
        self._chunks_sequence = chunks_sequence
        self._listings_when_no_neighborhood = listings_when_no_neighborhood
        self._tool_result = tool_result
        self.retrieve_calls = 0
        self.search_calls = 0
        self.execute_calls = 0

    def retrieve(self, *, message, user_id, role, request_id):
        self.retrieve_calls += 1
        if self._chunks_sequence is not None:
            idx = min(self.retrieve_calls - 1, len(self._chunks_sequence) - 1)
            return self._chunks_sequence[idx]
        return self._chunks

    def search_listings(self, *, filters: ExtractedFilters, user_id, role, request_id):
        self.search_calls += 1
        if self._listings_when_no_neighborhood:
            return [] if filters.neighborhood else self._listings
        return self._listings

    def preferences(self, *, user_id, role, request_id):
        return self._prefs

    def execute_tool(self, *, tool, params, user_id, role, request_id):
        self.execute_calls += 1
        if self._tool_result is not None:
            return self._tool_result
        from app.schemas import ToolExecution
        return ToolExecution(tool=tool, success=True, message=f"Done: {tool}", entityId=params.get("listingId"))


def _run(tools, message, *, role="STUDENT", persona="TENANT", llm=None):
    settings = get_settings()
    graph = build_graph(tools, llm or LLM(settings), settings)
    return graph.invoke({
        "message": message, "persona": persona, "role": role,
        "user_id": "u1", "request_id": "r1",
        "grounding_retries": 0, "listing_retries": 0, "relax_level": 0, "regenerated": False,
    })


def test_listing_search_returns_ranked_cards():
    tools = FakeTools(listings=[_listing()])
    state = _run(tools, "find me a furnished studio in Damas under 80,000")
    assert state["intent"] == "listing_search"
    assert len(state["listings"]) == 1
    card = state["listings"][0]
    assert card.matchLabel in {"Search match", "Preference match", "Strong match based on your saved preferences"}
    assert card.whyThisMatches  # explainable bullets present
    assert [a.label for a in card.actions] == ["View listing", "Save", "Apply"]
    assert "found" in state["draft"].lower()


def test_sale_listing_uses_contact_cta_not_apply():
    tools = FakeTools(listings=[_listing(listingPurpose="SALE", salePrice=25_000_000, rentAmount=None)])
    state = _run(tools, "I want to buy a house in Douala")
    labels = [a.label for a in state["listings"][0].actions]
    assert "Contact landlord" in labels
    assert "Apply" not in labels


def test_short_term_uses_request_stay_cta():
    tools = FakeTools(listings=[_listing(stayType="SHORT_TERM")])
    state = _run(tools, "show me short-term studios in Damas")
    labels = [a.label for a in state["listings"][0].actions]
    assert "Request stay" in labels
    assert "Apply" not in labels


def test_listing_search_retry_relaxes_filters():
    # No results while neighborhood filter is set; results appear after relaxation.
    tools = FakeTools(listings=[_listing(neighborhood="Melen")], listings_when_no_neighborhood=True)
    state = _run(tools, "studio in Damas under 80000")
    assert state["listing_retries"] == 1           # loop fired exactly once
    assert state["relax_level"] == 1
    assert len(state["listings"]) == 1             # recovered after relaxing
    assert tools.search_calls == 2


def test_grounding_retry_then_grounded():
    # First retrieval weak (empty), second strong -> one grounding retry, grounded.
    weak: list[RetrievedChunk] = []
    strong = [RetrievedChunk(chunkId="c1", source="docs/x.md", title="X", text="Verify via dashboard.", score=0.9)]
    tools = FakeTools(chunks_sequence=[weak, strong])
    state = _run(tools, "how do I verify my landlord on RoomBay?")
    assert state["intent"] == "knowledge_question"
    assert state["grounding_retries"] == 1
    assert state["grounded"] is True
    assert tools.retrieve_calls == 2


def test_grounding_retry_respects_cap():
    tools = FakeTools(chunks_sequence=[[]])  # always weak
    state = _run(tools, "what is RoomBay's refund policy?")
    assert state["grounding_retries"] == 1      # capped at 1, no infinite loop
    assert state["grounded"] is False
    assert state["draft"]                        # still produced a graceful answer


class _FlakyLLM(LLM):
    """Non-stub LLM: returns a too-short answer first, a good one after regen."""

    def __init__(self):
        super().__init__(get_settings())
        self.provider = "openai"  # force non-stub path
        self.calls = 0

    def complete(self, system, user):  # type: ignore[override]
        self.calls += 1
        return "Hi" if self.calls == 1 else "Here are listings I found for you. Next step: open one."


def test_self_check_triggers_one_regeneration():
    tools = FakeTools(listings=[_listing()])
    llm = _FlakyLLM()
    state = _run(tools, "find a studio in Damas", llm=llm)
    assert state["regenerated"] is True
    assert llm.calls == 2
    assert "found" in state["draft"].lower()


def test_unsafe_request_is_blocked():
    tools = FakeTools(listings=[_listing()])
    state = _run(tools, "how do I bypass landlord verification with a fake id?")
    assert state["safety_blocked"] is True
    assert state["listings"] == []
    assert "can't help" in state["draft"].lower()


def test_role_gate_downgrades_admin_intent_for_non_admin():
    nodes = Nodes(FakeTools(), LLM(get_settings()))
    out = nodes.check_role_permissions({"intent": "admin_help", "role": "STUDENT"})
    assert out["intent"] == "knowledge_question"


def test_save_listing_executes_write_tool():
    listing_id = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"
    tools = FakeTools(listings=[_listing(id=listing_id)])
    state = _run(tools, "save this listing for me")
    assert state["intent"] == "save_listing"
    assert tools.execute_calls == 1
    assert state["tool_executions"][0].success is True
    assert "SAVE_LISTING_FAVORITE" in state["tool_executions"][0].tool


def test_apply_to_listing_uses_uuid_from_message():
    listing_id = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"
    tools = FakeTools()
    state = _run(tools, f"apply to this listing {listing_id}")
    assert state["intent"] == "apply_to_listing"
    assert tools.execute_calls == 1
    assert state["target_listing_id"] == listing_id
