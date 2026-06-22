"""RoomBay AI Orchestrator — a lightweight LangGraph sidecar.

Orchestrates RoomBay's existing RAG/GraphRAG and listing tools (reached through
Spring Boot internal endpoints) with an agentic feedback loop. Spring Boot remains
the security boundary; this service never touches the database directly and only
suggests actions.
"""

__version__ = "0.1.0"
