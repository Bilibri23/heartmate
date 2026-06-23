"""LangGraph assembly for the RoomBay orchestrator.

Linear backbone with three capped feedback loops:

  classify_intent → extract_filters → check_role_permissions → retrieve_docs
    ↺ grounding retry (retrieve_docs → broaden_for_grounding → retrieve_docs)
  → fetch_user_preferences → search_listings
    ↺ listing-search retry (search_listings → relax_and_retry_listings → search_listings)
  → rank_results → execute_agent_tools → generate_response
    ↺ self-check reflect (self_check → regenerate → generate_response)
  → safety_check → prepare_actions → END
"""

from __future__ import annotations

from langgraph.graph import END, START, StateGraph

from .config import Settings, get_settings
from .llm import LLM
from .nodes import Nodes, State
from .roombay_client import RoomBayTools

_GROUNDING_INTENTS = {"knowledge_question", "landlord_help", "admin_help", "application_help"}


def build_graph(tools: RoomBayTools, llm: LLM, settings: Settings | None = None):
    settings = settings or get_settings()
    n = Nodes(tools, llm, settings)
    g = StateGraph(State)

    for name, fn in {
        "classify_intent": n.classify_intent,
        "extract_filters": n.extract_filters,
        "check_role_permissions": n.check_role_permissions,
        "retrieve_docs": n.retrieve_docs,
        "broaden_for_grounding": n.broaden_for_grounding,
        "fetch_user_preferences": n.fetch_user_preferences,
        "search_listings": n.search_listings,
        "relax_and_retry_listings": n.relax_and_retry_listings,
        "rank_results": n.rank_results,
        "execute_agent_tools": n.execute_agent_tools,
        "generate_response": n.generate_response,
        "regenerate": n.regenerate,
        "self_check": n.self_check,
        "safety_check": n.safety_check,
        "prepare_actions": n.prepare_actions,
    }.items():
        g.add_node(name, fn)

    g.add_edge(START, "classify_intent")
    g.add_edge("classify_intent", "extract_filters")
    g.add_edge("extract_filters", "check_role_permissions")
    g.add_edge("check_role_permissions", "retrieve_docs")

    # Loop 1: grounding retry
    def after_retrieve(state: State) -> str:
        weak = not state.get("grounded", False)
        retryable = state.get("intent") in _GROUNDING_INTENTS
        under_cap = state.get("grounding_retries", 0) < settings.grounding_retry_cap
        if weak and retryable and under_cap:
            return "broaden_for_grounding"
        return "fetch_user_preferences"

    g.add_conditional_edges(
        "retrieve_docs",
        after_retrieve,
        {"broaden_for_grounding": "broaden_for_grounding", "fetch_user_preferences": "fetch_user_preferences"},
    )
    g.add_edge("broaden_for_grounding", "retrieve_docs")

    g.add_edge("fetch_user_preferences", "search_listings")

    # Loop 2: listing-search retry
    def after_search(state: State) -> str:
        is_search = state.get("intent") == "listing_search"
        empty = not state.get("listings")
        under_cap = state.get("listing_retries", 0) < settings.listing_retry_cap
        if is_search and empty and under_cap:
            return "relax_and_retry_listings"
        return "rank_results"

    g.add_conditional_edges(
        "search_listings",
        after_search,
        {"relax_and_retry_listings": "relax_and_retry_listings", "rank_results": "rank_results"},
    )
    g.add_edge("relax_and_retry_listings", "search_listings")

    g.add_edge("rank_results", "execute_agent_tools")
    g.add_edge("execute_agent_tools", "generate_response")
    g.add_edge("generate_response", "self_check")

    # Loop 3: self-check / reflect
    def after_self_check(state: State) -> str:
        ok = state.get("self_check_ok", True)
        under_cap = not state.get("regenerated", False)
        if not ok and under_cap:
            return "regenerate"
        return "safety_check"

    g.add_conditional_edges(
        "self_check",
        after_self_check,
        {"regenerate": "regenerate", "safety_check": "safety_check"},
    )
    g.add_edge("regenerate", "generate_response")

    g.add_edge("safety_check", "prepare_actions")
    g.add_edge("prepare_actions", END)

    return g.compile()
