# Test strategy — thesis defense reference

RoomBay uses a **unit-first backend pyramid** with security and migration gates, multi-stack CI, and deterministic AI orchestrator tests.

## 1. Unit testing (primary layer)

- ~41 JUnit 5 test classes, ~141+ test methods
- Pattern: Mockito mocks, no Spring context; fast service-layer tests
- Examples: `MatchingServiceTest`, `AiAssistantServiceTest`, `LandlordAnalyticsServiceTest`, `FeedServiceTest`, `ListingSearchIndexerServiceTest`

## 2. Component / module testing

- **Validation:** `ListingRequestValidationTest` (Bean Validation)
- **Exception handling:** `GlobalExceptionHandlerTest`
- **AI submodules:** `AiGraphRagServiceTest`, `AiRetrievalPolicyTest`
- **Python orchestrator:** pytest in `ai-orchestrator/tests/` — API auth + LangGraph behavior with stub LLM

## 3. Security testing

- **Authorization matrix:** `IdorAuthorizationMatrixTest` — parameterized IDOR checks
- **Policy & controller security:** `AuthorizationPolicyServiceTest`, `AdminOpsControllerSecurityTest`, `LandlordAnalyticsControllerSecurityTest`
- **Rate limiting:** `AdvancedRateLimitInterceptorTest`, `AuthEndpointRateLimitFilterTest`
- **WebSocket JWT:** `WebSocketAuthInterceptorTest`
- **Internal AI token:** `AiInternalToolControllerTest`

## 4. Schema / migration testing

- **Static Flyway regression:** `MigrationCoverageTest` — DDL assertions for recent migrations
- **Live DB integration (CI gated):** `RoombayApplicationTests` — `@SpringBootTest` + Flyway table existence; enabled in CI via `-Droombay.schemaValidation.enabled=true`

## 5. Continuous integration

From `.github/workflows/ci.yml`:

| CI job | Validates |
|--------|-----------|
| **backend** | `mvn test` against Postgres 15 + Flyway + Hibernate validate |
| **migration-check** | Application boot + schema path (`RoombayApplicationTests`) |
| **ai-orchestrator** | `pytest -q` |
| **frontend** | ESLint + production `next build` |
| **docker** | Backend image builds on `main` |

## 6. Not automated (state honestly)

- No browser E2E (Playwright/Cypress)
- No frontend unit tests (Jest/Vitest)
- No Elasticsearch Testcontainers integration tests yet
- Manual smoke: `scripts/smoke-test-production.ps1`

## Suggested slide title

**“Test strategy: unit-first backend pyramid with security and migration gates, multi-stack CI, deterministic AI orchestrator tests.”**

## Architecture note (thesis wording)

Listing index updates propagate through a **PostgreSQL transactional outbox** processed by a scheduled indexer (`ListingSearchIndexerService`, 15s poll). Redis supports **distributed rate limiting** when enabled in production — not an Elasticsearch sync queue.
