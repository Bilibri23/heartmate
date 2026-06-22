# AGENTS.md — RoomBay operating context

Read this before planning or editing multiple files. The repo is the prompt: product docs, CI, migrations, and this file define how agents should work here.

Human entry point: [README.md](README.md)

## Must-follow conventions

- **Minimize scope** — match naming, patterns, and abstractions in the file you are editing.
- **Never commit secrets** — only `.env.example` / `deploy/*.env.example` templates in git.
- **Only commit when the user asks.**
- **Roles:** `STUDENT`, `LANDLORD`, `ADMIN` (not Seeker/Lister).
- **Product name:** RoomBay (legacy strings may say RoomConnect in old notes).

### Database changes

1. Add Flyway SQL: `backend/src/main/resources/db/migration/V{n}__snake_case_description.sql` (next after latest, currently **V53**).
2. Update entity, DTOs (`ListingRequest` / `ListingResponse` or equivalent), service mapping.
3. Add or extend test in `backend/src/test/java/org/rooms/roombay/MigrationCoverageTest.java`.
4. If user-facing: frontend types + UI (see listing taxonomy pattern below).

### Full-stack feature pattern (listings example)

```
PropertyListing enums/fields
  → ListingRequest / ListingResponse
  → ListingService (applyTaxonomyFields, mapToResponse, validation)
  → ListingSpecifications + controllers (search params)
  → frontend/lib/listing-taxonomy.ts + ListingAttributeBadges
  → cards, detail page, landlord wizard, AI cards
```

### Windows / Maven

- Use `.\mvnw.cmd` from repo root (not bare `mvn`).
- PowerShell: `;` not `&&`; quote `-Dtest=...` args: `.\mvnw.cmd "-Dtest=FooTest" test`.

## Environment matrix

| | Local | Production |
|---|-------|------------|
| Profile | default | `prod` (`SPRING_PROFILES_ACTIVE=prod`) |
| Flyway | **OFF** | **ON** |
| Schema | Hibernate `ddl-auto=update` | Flyway + validate |
| Backend port | 8082 | Railway |
| Frontend port | 3000 | Vercel |
| Postgres | `localhost:5433` / `roomconnect_db` | Railway |
| AI | Ollama optional (local) | VPS Ollama URL |
| OAuth redirect | localhost / ngrok | `www.roombay.com` only |

Config: [backend/src/main/resources/application.properties](backend/src/main/resources/application.properties), [application-prod.properties](backend/src/main/resources/application-prod.properties)

## Where to look

| Task | Start here |
|------|------------|
| User-facing behavior | `docs/{public,tenant,landlord,admin}/` |
| Deploy / incidents | `docs/deploy/`, `docs/admin/incident-triage-and-diagnostics.md` |
| AI assistant | `AiAssistantService`, `AiCopilotToolService`, `docs/admin/ai-admin-runbook.md` |
| Listings / search | `ListingService`, `ListingSpecifications`, `frontend/app/(main)/search/` |
| Security | `docs/security/`, `InputSanitizer`, `AuthorizationPolicyService` |
| Finetune / RAG | `AiFinetuneDatasetService`, `scripts/rag-ingest-local.ps1` |

## Do not use as source of truth

- **`track_dev/`** — historical sprint logs; may reference Vite, Render, wrong ports.
- **`target/`** — build output, not source.

Authoritative deploy doc: [docs/deploy/production-deploy.md](docs/deploy/production-deploy.md)

## Test before claiming done

```powershell
.\mvnw.cmd test
.\mvnw.cmd "-Dtest=MigrationCoverageTest" test   # if migration added
cd frontend; npm run lint; npm run build          # if frontend touched
```

## Product rules agents should preserve

- **Sale listings:** Contact seller — not Apply.
- **Room Preview:** Live for tenants when `roomPreviewEnabled && roomPreviewStatus === APPROVED`.
- **Schedule viewing:** Message-based (not full calendar) unless explicitly requested.
- **Photo limit:** 15 per listing (`ListingService.MAX_LISTING_PHOTOS`).

## Deferred (do not rebuild unless asked)

Payments automation, WhatsApp API, full calendar booking, university onboarding, mobile app, Elasticsearch as default search path.
