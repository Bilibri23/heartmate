# Error Catalog and Fixes

## AI errors

### `model "nomic-embed-text" not found`
Cause: Ollama embedding model not installed.
Fix:
- `ollama pull nomic-embed-text`
- `ollama list`

### `extension "vector" is not available`
Cause: PostgreSQL lacks pgvector.
Fix: run pgvector-enabled Postgres and point `DATABASE_URL` to it.

### `/api/ai/chat` returns 500
Likely causes:
- missing model
- bad provider config
- DB/vector issues

Fix checklist:
1. Verify `AI_PROVIDER` and model env vars
2. Confirm models via `ollama list`
3. Confirm pgvector DB reachable
4. Re-ingest docs

## Docker/runtime errors

### Port already allocated (5432)
Cause: existing DB already binds 5432.
Fix: run new DB on 5433 or stop existing DB.

### Container name conflict
Cause: name already reserved.
Fix: `docker rm -f <name>` then rerun.

## Search/listing errors

### Approved listing not in search
Fix: reindex search from admin settings.

### Admin cannot see listing images
Fix: ensure admin detail uses `photos`/`primaryPhotoUrl` and valid URLs.

## Verification/payment errors

### Admin cannot verify due to missing proof
Fix: ensure admin DTO includes proof/image URLs and renders previews.

### Verification repeatedly rejected
Fix: provide clear rejection reason and ask for better image quality.
