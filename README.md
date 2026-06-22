# RoomBay

RoomBay is a Cameroon-focused housing marketplace connecting tenants, landlords, and admins — search, applications, messaging, leases, payments, and an AI assistant grounded in product docs.

Product overview: [docs/public/platform-overview.md](docs/public/platform-overview.md)

For AI coding agents, read [AGENTS.md](AGENTS.md) before multi-file changes.

## Monorepo layout

| Path | Purpose |
|------|---------|
| `backend/` | Spring Boot 3.5, Java 17, Maven, Flyway migrations |
| `frontend/` | Next.js 16 (App Router), React 19, Tailwind |
| `docs/` | Product + ops knowledge (RAG source for in-app AI) |
| `deploy/` | Railway, Vercel, Hetzner VPS artifacts |
| `scripts/` | RAG ingest, AI finetune, smoke tests |
| `track_dev/` | **Historical** sprint journals — not source of truth |

Doc index: [docs/README.md](docs/README.md)

## Local quickstart

**Prerequisites:** Java 17, Node 20+, PostgreSQL 15 (with `vector` extension for AI features).

1. Copy env template:
   ```powershell
   copy backend\.env.example backend\.env
   ```
2. Postgres: `localhost:5433`, database `roomconnect_db` (see `backend/.env.example`).
3. Backend (from repo root, port **8082**):
   ```powershell
   .\mvnw.cmd spring-boot:run
   ```
4. Frontend (port **3000**):
   ```powershell
   cd frontend
   npm install
   npm run dev
   ```

**Schema note:** Flyway is **disabled** locally (`spring.flyway.enabled=false`); Hibernate uses `ddl-auto=update`. Production uses Flyway migrations on deploy.

## Tests (mirrors CI)

From repo root:

```powershell
.\mvnw.cmd clean compile
.\mvnw.cmd test
.\mvnw.cmd "-Dtest=AiCopilotToolServiceTest" test
```

Frontend:

```powershell
cd frontend
npm run lint
npm run build
```

CI workflow: [.github/workflows/ci.yml](.github/workflows/ci.yml)

## Deploy

Production: **Railway** (API + Postgres), **Vercel** (frontend), **Hetzner VPS** (Ollama / optional Elasticsearch / Redis).

Runbook: [docs/deploy/production-deploy.md](docs/deploy/production-deploy.md)

Env templates: [deploy/railway.env.example](deploy/railway.env.example), [backend/.env.example](backend/.env.example)

## Key conventions

- User roles: `STUDENT`, `LANDLORD`, `ADMIN`
- Migrations: `backend/src/main/resources/db/migration/V{n}__snake_case.sql` (latest: V53)
- OAuth redirect in prod: `https://www.roombay.com/login/oauth2/code/google` (not `api.roombay.com`)
