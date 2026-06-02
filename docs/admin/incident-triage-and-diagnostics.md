# Admin Incident Triage And Diagnostics

Admins can ask ops-level questions about platform health and incidents.

Common incident checks:
- Backend health: check `/actuator/health`.
- Database errors: look for SQLState, missing relation, failed migration, connection timeout, or credential errors.
- AI ingestion: verify `AI_DOCS_DIR`, docs presence in the deployment image, ingest result counts, and graph stats.
- Messaging and notifications: check backend logs, websocket connection status, and endpoint responses.
- Frontend crashes: check browser console, recent deploys, and route/auth hydration behavior.

Admin-safe diagnostic boundaries:
- Explain symptoms, likely causes, and next checks.
- Do not expose secrets, private tokens, full JWTs, credentials, or private documents.
- Prefer runbook-level steps over destructive actions.
- For database changes, use migrations and backups; do not recommend ad hoc production edits unless there is an approved incident procedure.

