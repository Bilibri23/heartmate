
# RoomBay FAQ (Tenant, Landlord, Admin)

## Tenant

### How do I verify my account?
Go to `/verification`, select ID type, enter ID number, upload ID photo and selfie, then submit.

### Why is verification pending?
Verification is manually reviewed by admin. Use refresh on verification page.

### How do I share a listing with a roommate?
Open listing details, tap share, choose a matched roommate, and send.

### Can my roommate apply with me automatically?
No. Co-apply was simplified. Shared roommate applies independently.

### How do I track applications?
Open `/applications` and check status per listing.

### Why was payment rejected?
Payment proof may be unclear or mismatched. Check rejection reason and resubmit.

## Landlord

### Why is my listing not visible?
Listing must be approved by admin to become `ACTIVE`.

### How do I review applications?
Use `/landlord/applications` to review and accept/reject applicants.

### How is tenant management determined?
Tenants should be derived from active leases.

### What analytics are available?
Basic listing views and application counts.

## Admin

### What must I check before approving tenant verification?
Readable ID, plausible ID number, selfie match, no tampering.

### What must I check before approving listings?
Photo quality/completeness, coherent details, and policy compliance.

### How do I fix missing search results?
Run Search Reindex from Admin Settings.

## AI assistant

### Where are embeddings stored?
Retrieval uses **PostgreSQL with the pgvector extension**, not FAISS. Chunks and vectors live in tables created by Flyway migration `V32__create_ai_rag_tables.sql`; similarity search is implemented in `AiRagRepository`.

### Which provider is active?
Based on backend env `AI_PROVIDER` (`openai`, `ollama`, or `auto`). For **self-hosted only**, use `ollama` and do not set `OPENAI_API_KEY`.

### Why does AI fail with model not found?
Configured Ollama model is not pulled yet.

### How do I refresh AI knowledge?
Update docs and run Admin "Ingest Docs".

## Production / VPS (checklist)

1. **Ollama** on the same VPS or reachable URL: install models you configured (`OLLAMA_CHAT_MODEL`, `OLLAMA_EMBEDDING_MODEL`).
2. **Env vars:** `AI_PROVIDER=ollama` for self-hosted-only; leave `OPENAI_API_KEY` unset. Optional: `AI_DOCS_DIR` absolute path to the repo `docs/` folder if the JVM working directory is not the repo root.
3. **PostgreSQL:** Ensure extension **pgvector** is enabled where the app runs Flyway migrations (or applied manually) so RAG tables exist.
4. **After changing markdown:** run ingest again so chunks and embeddings stay current.
5. **Elasticsearch:** See [`backend/docs/ELASTICSEARCH-SETUP.md`](../backend/docs/ELASTICSEARCH-SETUP.md) and [`NEXT-TODOS.md`](NEXT-TODOS.md).

## Knowledge files ingested by default

All `*.md` under `docs/` (default `AI_DOCS_DIR=../docs` from `backend/`). Cameroon and safety content includes [`rag-cameroon-rental-basics.md`](rag-cameroon-rental-basics.md), [`rag-cameroon-safety-scams.md`](rag-cameroon-safety-scams.md), [`rag-roombay-platform-usage.md`](rag-roombay-platform-usage.md).
