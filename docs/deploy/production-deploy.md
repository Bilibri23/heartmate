# RoomBay production deploy (Railway + Vercel + Hetzner)

Operational runbook aligned with the production audit. Infrastructure steps are manual on each host; repo artifacts live under [`deploy/`](../../deploy/).

## Day 0 — Repo (done in codebase)

- Flyway `V37` SQL typo fixed
- [`application-prod.properties`](../../backend/src/main/resources/application-prod.properties) — AI, search, Redis, Sentry, payments, dev-seed
- [`frontend/.env.production`](../../frontend/.env.production) — Vercel variables
- [`deploy/railway.env.example`](../../deploy/railway.env.example) — Railway variable template

**Go/no-go before DNS:** Railway deploy with `SPRING_PROFILES_ACTIVE=prod`; Flyway completes; `GET /actuator/health` returns UP.

**Postgres:** After Railway Postgres plugin:

```sql
CREATE EXTENSION IF NOT EXISTS vector;
```

## Day 1 — Hetzner VPS + Google OAuth

### P1–P2 VPS

1. CX43 Ubuntu 24.04, SSH key, `deploy` user, Docker installed.
2. Copy [`deploy/docker-compose.vps.yml`](../../deploy/docker-compose.vps.yml) to `~/roombay/docker-compose.yml`.
3. `docker compose up -d`
4. Pull models:

```bash
docker exec ollama ollama pull qwen2.5:1.5b
docker exec ollama ollama pull nomic-embed-text
```

5. Verify: `curl http://127.0.0.1:11434/api/tags`, `curl http://127.0.0.1:9200/_cluster/health`, `redis-cli -h 127.0.0.1 ping`

### P3 Google OAuth (critical)

| Setting | Value |
|---------|--------|
| Redirect URI | `https://www.roombay.com/login/oauth2/code/google` |
| JS origins | `https://www.roombay.com`, `https://roombay.com` |

Do **not** use `api.roombay.com` for the redirect — the frontend proxies OAuth via Vercel.

## Day 2 — Railway, DNS, Vercel

### P4 Railway

- Import repo; root: `backend` (or monorepo build per Railway Java template)
- Postgres plugin + `vector` extension
- Variables: copy from [`deploy/railway.env.example`](../../deploy/railway.env.example)
- Custom domain: `api.roombay.com`
- Spend cap in billing

### P5 DNS

| Type | Name | Target |
|------|------|--------|
| CNAME | api | Railway CNAME |
| CNAME | www | `cname.vercel-dns.com` |
| A | @ | `76.76.21.21` (Vercel apex) |

Redirect apex → `www` in Vercel if needed.

### P6 Vercel

- Root: `frontend`
- Env: see [`frontend/.env.production`](../../frontend/.env.production)
- Domain: `www.roombay.com`

### P7 Network lockdown

**Do not rely on a single Railway egress IP** — it can change.

Launch options:

1. **Recommended:** `ROOMBAY_SEARCH_ELASTICSEARCH_ENABLED=false`; expose Ollama only; add VPN/tunnel or authenticated reverse proxy later.
2. **UFW by IP:** Allow VPS ports only from known Railway egress; re-check after redeploys.

## Day 3 — Verification

Run [`scripts/smoke-test-production.ps1`](../../scripts/smoke-test-production.ps1):

```powershell
.\scripts\smoke-test-production.ps1 -ApiBase https://api.roombay.com -WebBase https://www.roombay.com
```

Manual checks:

- Register / login (email + JWT)
- Google OAuth → `/auth/oauth-callback`
- Listings search (DB path with ES disabled)
- Cloudinary upload
- AI chat after **Admin → Ingest Docs** (or `POST /api/ai/admin/ingest`)
- Payments: manual MoMo flow only; set `ROOMBAY_PLATFORM_MTN_MOMO` / `ROOMBAY_PLATFORM_ORANGE_MONEY` before real money
- Browser Network tab: no requests to `localhost`

## Payments at launch

No card/Stripe gateway. Manual proof + admin verify is intentional. Do not market instant card checkout.

## Optional — LangGraph AI orchestrator (feedback loop sidecar)

The orchestrator is **off by default**. Spring falls back to the built-in assistant if the sidecar is disabled or unreachable. Full architecture: [`docs/admin/ai-orchestrator.md`](../admin/ai-orchestrator.md).

### Why VPS (not a second Railway service)

Ollama already runs on Hetzner. The sidecar calls Ollama locally and calls Railway for read-only tools (`/internal/ai/*`). A Railway-hosted sidecar would still need VPS Ollama over the network.

### 1. Generate shared secret

```bash
openssl rand -hex 32
```

Use the same value as:

- Railway: `ROOMBAY_AI_INTERNAL_TOKEN`
- VPS `.env`: `ROOMBAY_INTERNAL_TOKEN`

### 2. VPS — start the sidecar

On the VPS (`~/roombay`):

1. Copy [`deploy/vps.env.example`](../../deploy/vps.env.example) to `.env` and set `ROOMBAY_API_BASE_URL`, `ROOMBAY_INTERNAL_TOKEN`, `AI_PROVIDER=ollama`.
2. Ensure [`deploy/docker-compose.vps.yml`](../../deploy/docker-compose.vps.yml) is present (build context includes `ai-orchestrator/`).
3. Start:

```bash
docker compose up -d ai-orchestrator
```

4. Lock port **8131** to Railway egress only — see [`deploy/vps-network-lockdown.md`](../../deploy/vps-network-lockdown.md).

### 3. Railway — enable delegation

In the backend service variables (see [`deploy/railway.env.example`](../../deploy/railway.env.example)):

| Variable | Value |
|----------|--------|
| `ROOMBAY_AI_ORCHESTRATOR_ENABLED` | `true` |
| `ROOMBAY_AI_ORCHESTRATOR_BASE_URL` | `http://YOUR_VPS_IP:8131` |
| `ROOMBAY_AI_INTERNAL_TOKEN` | same secret as VPS |

Redeploy Railway after saving variables.

### 4. Verify (smoke test)

**Level 1 — sidecar health (on VPS or from a machine that can reach :8131):**

```bash
curl http://YOUR_VPS_IP:8131/health
```

**Level 2 — orchestrate (LangGraph loop):**

```bash
curl -X POST http://YOUR_VPS_IP:8131/orchestrate \
  -H "Content-Type: application/json" \
  -H "X-RoomBay-Internal-Token: YOUR_SECRET" \
  -d '{"message":"Find a room in Buea under 50000","persona":"TENANT","userId":"00000000-0000-0000-0000-000000000001","userRole":"STUDENT","requestId":"smoke-1"}'
```

Without the internal token header, `/orchestrate` returns **401** (prevents spoofed `userId`/`role`).

Use `AI_PROVIDER=stub` in VPS `.env` for a deterministic response without Ollama.

**Level 3 — internal tool gate (Railway API):**

```bash
# Expect 401
curl -X POST https://api.roombay.app/internal/ai/retrieve \
  -H "Content-Type: application/json" \
  -d '{"query":"test","userId":"00000000-0000-0000-0000-000000000001","role":"STUDENT"}'

# Expect 200 with token
curl -X POST https://api.roombay.app/internal/ai/retrieve \
  -H "Content-Type: application/json" \
  -H "X-RoomBay-Internal-Token: YOUR_SECRET" \
  -d '{"query":"how do I apply","userId":"00000000-0000-0000-0000-000000000001","role":"STUDENT"}'
```

**Level 4 — product UI:** log in as a **STUDENT**, open AI chat, ask a listing question. Check Railway logs for orchestrator delegation vs built-in fallback.

**Level 5 — automated (local / CI):**

```bash
cd ai-orchestrator && pytest -q
.\mvnw.cmd -Dtest=AiOrchestratorClientTest,AiInternalToolControllerTest test
```

### Demo tip

Compare the same chat question with `ROOMBAY_AI_ORCHESTRATOR_ENABLED=false` vs `true`. With orchestrator on, strict listing filters can trigger a listing-search retry (relaxed budget/neighbourhood). VPS logs: `docker logs ai-orchestrator`.
