# RoomBay AI Orchestrator (LangGraph sidecar)

The orchestrator is an optional Python service that adds a **LangGraph agent with a
feedback loop** on top of RoomBay's existing custom RAG/GraphRAG and listing tools.
It does not replace them and does not touch the database. The Spring Boot backend
remains the security boundary and the public AI contract is unchanged.

## Why it exists

The built-in assistant runs a single linear pass (classify → retrieve → generate →
guard) with no self-correction. The orchestrator orchestrates the same tools as a
graph with three **capped** feedback loops:

1. **Grounding retry** — re-retrieve when RAG context is weak.
2. **Listing-search retry** — relax filters (drop neighbourhood, then budget) when a
   listing search returns nothing.
3. **Self-check / safety reflect** — critique the draft and regenerate once, then a
   terminal safety gate.

## Architecture

```
Frontend → POST /api/ai/chat(/stream)
   └─ flag off → built-in AiAssistantService pipeline (unchanged fallback)
   └─ flag on  → AiOrchestratorClient → POST {sidecar}/orchestrate
                    sidecar (LangGraph) calls back, read-only:
                      POST /internal/ai/retrieve          (RAG/GraphRAG, role-filtered, sanitized)
                      POST /internal/ai/listings/search   (tenant listing search)
                      POST /internal/ai/preferences       (saved tenant preferences)
                 → Spring runs AiOutputGuard on the answer → AiChatResponse
```

- The sidecar owns LLM generation + self-critique (same provider as the backend).
- Spring owns data, RAG retrieval, role filtering, sanitization, logging, and the
  final output guard.

## Graph nodes

`classify_intent → extract_filters → check_role_permissions → retrieve_docs
(↺ grounding) → fetch_user_preferences → search_listings (↺ listing retry) →
rank_results → generate_response (↺ self-check) → safety_check → prepare_actions`.

Each loop is capped (default 1) so latency stays bounded — no unbounded reasoning.

## Security

- `/internal/ai/*` is permitted in `SecurityConfig` but **token-gated** in the
  controller: a request without a matching `X-RoomBay-Internal-Token` gets 401, and
  a blank configured token rejects everything (closed by default).
- **`POST /orchestrate` is also token-gated** with the same header. Only Spring
  (holding `ROOMBAY_AI_INTERNAL_TOKEN`) may invoke the graph; public callers cannot
  spoof `userId` / `userRole` through an open sidecar port.
- The shared secret is `roombay.ai.internal.token` (backend) =
  `ROOMBAY_INTERNAL_TOKEN` (sidecar).
- Every tool call carries `userId` / `role` / `requestId`; retrieval is role-filtered
  and listing search is tenant-scoped, so the sidecar cannot widen its own access.
- All internal endpoints are read-only. The agent only **suggests** actions — it
  never approves, rejects, verifies, pays, or deletes anything.

## Configuration

Backend (`application.properties`), off by default:

| Property | Env | Default |
|---|---|---|
| `roombay.ai.orchestrator.enabled` | `ROOMBAY_AI_ORCHESTRATOR_ENABLED` | `false` |
| `roombay.ai.orchestrator.base-url` | `ROOMBAY_AI_ORCHESTRATOR_BASE_URL` | `http://localhost:8131` |
| `roombay.ai.orchestrator.request-timeout-ms` | `ROOMBAY_AI_ORCHESTRATOR_TIMEOUT_MS` | `20000` |
| `roombay.ai.internal.token` | `ROOMBAY_AI_INTERNAL_TOKEN` | _(blank → closed)_ |

Sidecar env: see `ai-orchestrator/.env.example` (`ROOMBAY_API_BASE_URL`,
`ROOMBAY_INTERNAL_TOKEN`, `AI_PROVIDER`, model + loop-cap settings).

## Run

Local:

```bash
cd ai-orchestrator
python3 -m venv .venv && . .venv/bin/activate
pip install -r requirements.txt
cp .env.example .env            # set ROOMBAY_INTERNAL_TOKEN to match the backend
uvicorn app.main:app --port 8131
```

Then on the backend set `ROOMBAY_AI_ORCHESTRATOR_ENABLED=true` and the matching
`ROOMBAY_AI_INTERNAL_TOKEN`.

VPS: the sidecar is wired into `deploy/docker-compose.vps.yml` (service
`ai-orchestrator`, port 8131, reuses the VPS Ollama).

## Fallback behaviour

If the orchestrator is disabled, unreachable, slow (timeout), or returns a blank
answer, `AiOrchestratorClient` returns empty and the backend answers via its
built-in pipeline. `/api/ai/chat` and `/api/ai/chat/stream` keep the same request
and response either way.

## Tests

- Sidecar: `cd ai-orchestrator && pytest -q` (faked tools, stub LLM — offline).
- Backend: `AiInternalToolControllerTest` (token gate), `AiOrchestratorClientTest`
  (fallback on unreachable sidecar).
