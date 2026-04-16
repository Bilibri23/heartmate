# GraphRAG rollout

## What shipped

- **Schema:** `ai_entities`, `ai_chunk_entities`, `ai_edges` (see `V37__ai_graph_rag_tables.sql` and runtime bootstrap in `AiRagSchemaInitializer`).
- **Ingest:** Each markdown chunk is embedded as before; additionally, headings, `docs/*.md` links, and domain keywords become entities; co-occurrence and reference edges are stored.
- **Retrieval:** `AiGraphRagService` seeds with vector similarity, expands to chunks that share entities, then merges scores (configurable weights).
- **Chat:** `AiAssistantService` calls `graphRagService.retrieve` (falls back automatically when the graph is empty or retrieval is disabled).
- **Admin:** `GET /api/ai/admin/graph-stats` and the Admin Settings “AI Assistant” card show counts; ingest responses include `graphEntitiesWritten` and `graphEdgesWritten`.

## Configuration

See `application.properties`:

- `roombay.ai.graph-rag-enabled` (env: `ROOMBAY_AI_GRAPH_RAG`) — set `false` to use flat pgvector top-K only.
- `roombay.ai.graph-rag-seed-k`, `roombay.ai.graph-rag-expand-limit`, `roombay.ai.graph-vector-weight`, `roombay.ai.graph-edge-weight` — tune hybrid behavior.

## Operations

1. Run doc ingest (Admin Settings or `scripts/rag-ingest-local.ps1`). Force re-ingest rebuilds chunks and the graph for each file.
2. Confirm stats via Admin Settings “Refresh graph stats” or `GET /api/ai/admin/graph-stats`.

## Regression checks

- Ask the in-app assistant a question that spans two docs; citations should include both sources when graph links exist.
- With `roombay.ai.graph-rag-enabled=false`, behavior should match the previous flat RAG path.
