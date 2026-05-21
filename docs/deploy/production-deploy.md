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
