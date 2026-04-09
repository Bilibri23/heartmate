# Next todos (post-MVP plan follow-ups)

Use this list to mirror the four operational items that often get cut off after code merges. Check them off on VPS/staging before calling production “ready.”

| # | Task | Done |
|---|------|------|
| 1 | **RAG ingest after deploy:** Point `AI_DOCS_DIR` at the repo `docs/` folder if the backend cwd differs; run **Admin → Ingest Docs** (or `POST /api/ai/admin/ingest` with admin JWT). Confirm assistant answers cite Cameroon/platform docs. | [ ] |
| 2 | **Self-hosted AI (VPS):** Install Ollama, set `AI_PROVIDER=ollama`, `OLLAMA_BASE_URL` (e.g. `http://127.0.0.1:11434`), pull **chat** and **embedding** models per [`ai-faq.md`](ai-faq.md). Do not set `OPENAI_API_KEY` if policy forbids external AI APIs. | [ ] |
| 3 | **Elasticsearch:** Set `roombay.search.elasticsearch.enabled=true` and `spring.elasticsearch.uris` to your cluster; restart backend; **Admin → Reindex** and verify document count. See [`backend/docs/ELASTICSEARCH-SETUP.md`](../backend/docs/ELASTICSEARCH-SETUP.md). | [ ] |
| 4 | **Secrets only via env:** `JWT_SECRET`, Cloudinary keys, `SENTRY_DSN`, DB credentials, and any tokens must live in **`.env` or your host secret manager** — never committed. Rotate anything that was ever committed in git history. | [ ] |

**Related:** [`mvp-vision-vs-implementation-gaps.md`](mvp-vision-vs-implementation-gaps.md), [`system-gaps-and-recommendations.md`](system-gaps-and-recommendations.md), [`onboarding-ux-and-flows.md`](onboarding-ux-and-flows.md), [`platform-ai-brain.md`](platform-ai-brain.md).

**Google OAuth (staging):** `app.oauth.google.enabled=true`, `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, redirect URI `{API}/login/oauth2/code/google`; frontend `NEXT_PUBLIC_OAUTH_GOOGLE_ENABLED=true`.
