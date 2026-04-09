# Platform AI brain (RoomBay)

**Goal:** One coherent “brain” that answers user and operator questions with **grounded** context (RAG), optional **local LLM** (Ollama), and clear boundaries so the product stays trustworthy.

## Layers

1. **Retrieval (pgvector)** — Listing copy, policies, Cameroon rental context, and curated docs are embedded and stored in PostgreSQL/pgvector. Queries return chunks with citations.
2. **Generation** — Chat completion via **Ollama** (self-hosted) or OpenAI when configured; prompts inject retrieved chunks to reduce hallucinations.
3. **Orchestration** — Existing services (e.g. assistant endpoints) validate auth, rate-limit, and log; future work can add tool-calling (e.g. “summarize my applications”) behind explicit permissions.

## Operations

- **Ingest** — Post-deploy or on doc changes, run ingestion so vectors stay fresh (see `docs/NEXT-TODOS.md` and `docs/rag-roombay-platform-usage.md`).
- **Secrets** — API keys only in environment; never commit provider tokens.
- **Observability** — Log query latency and failures; surface “I don’t know” when retrieval is empty.

## First-time tour (frontend)

The Shepherd onboarding tour highlights the floating **RoomBay Assistant** button (`data-tour="roombay-ai-assistant"` in `components/ai/assistant-widget.tsx`) and explains doc-grounded answers and typical questions (verification, applications, Search/Reels, landlord workflows). See `lib/tour/build-tours.ts` steps `tt-ai-brain` and `ll-ai-brain`.

## Relation to the codebase

- Configuration placeholders live under `application.properties` (AI / Ollama / OpenAI sections).
- Product-specific behavior should stay in services/controllers; this doc is the narrative for roadmap and ops, not a second source of env var truth.

## Roadmap (short)

- Unified “assistant” contract: user asks → retrieve → generate → cite sources.
- Optional admin-only analytics on common questions and failed retrievals.
- Stricter rate limits on AI endpoints (in addition to global API limits).
