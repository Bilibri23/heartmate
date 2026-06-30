# RoomBay AI Orchestrator (LangGraph sidecar)

> **Deprecated / not deployed.** Agentic actions (save listing, apply to listing, request a
> visit) now run **natively in the Spring backend** via `service/AiAgentActionRouter`, which
> calls the same `AiAgentToolService` directly — no separate Python service to host, and it
> works with `roombay.ai.orchestrator.enabled=false`. This directory is retained as a
> reference/thesis artefact. To re-enable the standalone sidecar, deploy this service, set the
> `ROOMBAY_AI_ORCHESTRATOR_*` env vars, and flip the flag to true.

A lightweight Python service that adds a **real LangGraph agent with a feedback
loop** on top of RoomBay's existing custom RAG/GraphRAG and listing tools. It does
**not** replace them and does **not** touch the database: it calls back into the
Spring Boot backend's read-only `/internal/ai/*` endpoints, which re-enforce role
on every call. Spring remains the security boundary and the source of truth.

## Why

The previous AI flow was a single linear pass (classify → retrieve → generate →
guard) with no self-correction. This sidecar orchestrates the same tools as a graph
with three **capped** feedback loops:

1. **Grounding retry** — re-retrieve when RAG context is weak.
2. **Listing-search retry** — relax filters (drop neighbourhood, then budget) when a
   listing search returns nothing.
3. **Self-check / safety reflect** — critique the draft and regenerate once before a
   terminal safety gate.

## Graph

```
classify_intent → extract_filters → check_role_permissions → retrieve_docs
  ↺ grounding retry
→ fetch_user_preferences → search_listings
  ↺ listing-search retry
→ rank_results → generate_response
  ↺ self-check reflect
→ safety_check → prepare_actions → END
```

## Contract

`POST /orchestrate` accepts `{message, persona, threadId, userId, userRole, requestId}`
and returns an `AiChatResponse`-shaped body (`answer, threadId, citations,
suggestedActions, listingResults, ragGrounded, meta`). `GET /health` reports the
active provider and loop caps.

## Run locally

```bash
python3 -m venv .venv && . .venv/bin/activate
pip install -r requirements.txt
cp .env.example .env            # AI_PROVIDER=stub works with no model
uvicorn app.main:app --reload --port 8131
```

In `stub` mode the service runs fully offline (deterministic text, tools degrade to
empty on connection errors). Set `AI_PROVIDER=openai`/`ollama` + the matching env to
use a real model, and point `ROOMBAY_API_BASE_URL`/`ROOMBAY_INTERNAL_TOKEN` at the
backend to enable RAG + listing tools.

## Test

```bash
. .venv/bin/activate
pytest -q
```

Tests fake the Spring tools and run the LLM in stub mode, so they are deterministic
and need no network.

## Security

- `POST /orchestrate` requires `X-RoomBay-Internal-Token` (same secret as Spring's
  `ROOMBAY_AI_INTERNAL_TOKEN`). Closed by default when unset — blocks spoofed
  `userId`/`role` from callers who reach port 8131.
- Every outbound tool call carries the internal token + `userId`/`role`/`requestId`; Spring
  rejects bad tokens and re-checks role.
- All tools are read-only. The agent only **suggests** actions — it never approves,
  rejects, verifies, or deletes anything.
- Spring runs its own `AiOutputGuard` on the returned answer as a final backstop.

## Fallback

If this service is disabled, down, or slow, Spring's `AiOrchestratorClient` times out
and the backend answers via its existing pipeline. The public `/api/ai/chat`
contract is unchanged either way.
