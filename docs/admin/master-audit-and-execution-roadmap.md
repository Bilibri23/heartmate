# RoomBay Master Audit and Execution Roadmap

## Executive Summary

RoomBay has moved beyond prototype status. The core monolith is operational, role-based dashboards exist, Flyway is active in production, Admin Operations Center v1 is present, AI docs are packaged into the backend image, and the assistant now has role-aware retrieval boundaries. The next maturity step is not broad feature expansion. It is tightening production controls, marketplace trust, analytics quality, AI answer quality, and growth loops.

### Current maturity score

| Area | Score | Assessment |
| --- | ---: | --- |
| Architecture | 7/10 | Solid Spring Boot and Next.js monolith with clear role surfaces; needs stronger service boundary tests and queue/process maturity. |
| Security | 6/10 | JWT, refresh sessions, admin guards, CORS, and redacted ops logging exist; WebSocket auth rejection, upload hardening, and object-level regression tests need priority. |
| AI | 7/10 | Good RAG/GraphRAG skeleton, role filtering, packaged docs, no-doc fallback, and admin ingest; needs evaluation, retrieval analytics, and tool-calling guardrails. |
| Marketplace | 6/10 | Listings, applications, leases, reviews, reports, and verifications exist; needs stronger trust scores, fraud rules, and supply/demand operations. |
| UX | 6/10 | Main flows exist and hydration issues were addressed; needs empty states, onboarding depth, trust messaging, and dashboard task clarity. |
| SEO | 6/10 | Sitemap, robots, structured data, and city pages exist; needs content clusters, canonical discipline, and listing SEO quality checks. |
| Scalability | 5/10 | Monolith is appropriate for v1; rate limits and caches are mostly local/simple while Redis/search are disabled or optional. |
| Reliability | 6/10 | Health, ops dashboard, Flyway, Sentry config, and error tables exist; needs backup drills, alert delivery, runbooks, and background job reliability. |

### Security scan summary

Threat model: RoomBay handles tenant identity data, landlord verification documents, listings, messages, leases, manual payment proofs, admin moderation, AI knowledge retrieval, and role-specific dashboards. Attackers include unauthenticated visitors, authenticated tenants/landlords, abusive marketplace actors, malicious admins with limited scope assumptions, bot traffic, and users attempting IDOR by changing UUIDs.

Validated high-risk findings from the production-triage pass:

- WebSocket CONNECT authentication logs invalid or missing tokens but does not reject the STOMP message, so unauthenticated sessions may still connect to broker destinations unless downstream behavior blocks them.
- Upload validation relies on client-provided MIME type and size, with no magic-byte verification, malware scanning, or Cloudinary transformation policy enforcement.
- Production rate limiting falls back to local in-memory counters when Redis is disabled, which weakens protection across multiple instances or restarts.
- Current object-level authorization is improved in many flows, but coverage is uneven and needs regression tests for every path-based ID.
- Admin Operations Center v1 is useful but local to the app; alerts are computed but not delivered to out-of-band channels.

## Top 25 Risks

| Rank | Severity | Risk | Evidence | Immediate action |
| ---: | --- | --- | --- | --- |
| 1 | Critical | WebSocket auth does not reject invalid CONNECT frames. | `WebSocketAuthInterceptor` logs invalid/missing auth and returns `message`. | Reject invalid CONNECT with `AccessDeniedException` or return `null`; add STOMP auth tests. |
| 2 | Critical | Production secrets were previously shared in chat and must be assumed compromised. | JWT, Cloudinary, mail, Google OAuth, admin seed values were exposed in prior user input. | Rotate all exposed secrets, revoke old credentials, disable admin seed after use. |
| 3 | High | Object-level authorization lacks full regression coverage across all UUID routes. | Many controllers rely on service checks; some use `AuthorizationPolicyService`, others use custom checks. | Add IDOR test matrix for applications, leases, payments, profiles, messages, notifications, listings, reviews, verifications, reports. |
| 4 | High | Upload trust is based on MIME headers and Cloudinary acceptance. | `FileUploadService` checks `MultipartFile.getContentType()` and size only. | Add magic-byte validation, AV scanning readiness, and upload audit events. |
| 5 | High | In-memory rate limiting is not production-strong across horizontal instances. | Redis disabled in prod config by default; local fallback exists. | Enable Redis-backed limits before scaling and protect AI, messaging, uploads, support, and reports. |
| 6 | High | Manual payment proof flow is fraud-prone without reconciliation controls. | Payments are proof upload plus admin verify. | Add payment proof duplicate detection, transaction reference uniqueness, admin two-step review for large amounts. |
| 7 | High | Admin account lifecycle and seed controls need operational discipline. | `app.admin.seed.enabled` exists and env sample uses admin seed concepts. | Make admin seed one-time, audit use, and add admin MFA or stronger admin session policy. |
| 8 | High | AI can become a privileged data interface if future tool calling is added without policy engine. | Current AI is grounded chat; admin docs include internal/security docs for admins. | Define tool permissions, audit logs, dry-run mode, and admin-only approval gates before actions. |
| 9 | High | Ops alerts are visible only in the admin UI. | Admin Ops v1 computes alerts in `AdminOpsService`; no delivery channel. | Add email/Sentry/webhook alert delivery for critical alerts. |
| 10 | High | Database backup and restore drills are not codified as release gates. | Deploy docs mention Postgres and health checks, not restore drills. | Add backup schedule, restore test runbook, RPO/RTO targets. |
| 11 | Medium | Docker healthcheck probes port 8080 while app exposes 8082 by default. | `backend/Dockerfile` healthcheck uses `localhost:8080`; `EXPOSE 8082`. | Align healthcheck with `$PORT`/8082 or Railway health behavior. |
| 12 | Medium | Search and feed depend on DB fallback when Elasticsearch is disabled. | Prod config disables ES by default. | Add search quality metrics, outbox lag monitoring, and staged OpenSearch rollout. |
| 13 | Medium | Admin Ops v1 lacks queue-specific SLA views. | Ops page shows health, alerts, funnel, recent errors, AI health. | Add queue module for verifications, listings, reports, payments, support. |
| 14 | Medium | AI ingest depends on manual admin action after doc changes. | Admin ingest endpoint exists; `ai_ingest_run` tracks runs. | Add deploy-time or scheduled ingest validation and stale-doc alert. |
| 15 | Medium | AI quality has no automated evaluation gate. | Parser and retrieval tests exist; no golden answer evaluation pipeline. | Add role-based eval set and no-doc/citation score thresholds. |
| 16 | Medium | Marketplace trust is split across verification, listings, reviews, and reports without one visible trust model. | Docs and code expose pieces, not a unified score. | Build tenant-facing listing trust and landlord reliability signals. |
| 17 | Medium | Review moderation appears reactive and lightweight. | Review flagging exists; report/admin moderation exists. | Add moderation queue, reason taxonomy, and repeat-offender rules. |
| 18 | Medium | SEO dynamic listing quality depends on listing data completeness. | Sitemap includes listing IDs; schema helpers exist. | Add SEO completeness checks for listing title, city, media, price, structured data. |
| 19 | Medium | Frontend auth stores JWTs in localStorage. | `auth-context` and API client read/write localStorage tokens. | Consider httpOnly cookie migration or stronger XSS prevention and CSP rollout. |
| 20 | Medium | CSP is backend API-oriented and frontend CSP is not evident. | Security headers configured in Spring; Next frontend needs its own headers. | Add Next.js security headers for public web app. |
| 21 | Medium | Analytics event coverage is new and server-side only for selected actions. | `analytics_event` and helper exist; many events are now emitted. | Add event QA dashboard and data dictionary. |
| 22 | Medium | Full backend test suite has unrelated existing failures. | Recent full `mvn test` failed in auth verification and online status tests. | Fix or quarantine failing tests before CI gating. |
| 23 | Low | Admin errors show sanitized messages but stack display policy should stay strict. | `app_error_log` stores shortened stack traces. | Keep frontend stack traces hidden by default; expose only behind admin debug toggle. |
| 24 | Low | Cloudinary mock mode can mask production misconfiguration if env validation is weak. | `CloudinaryConfig` returns mock when credentials missing. | Fail startup in prod when Cloudinary env is missing. |
| 25 | Low | Public support route is authenticated, which may block acquisition/support for logged-out users. | Security config authenticates `/api/support/**`. | Decide if pre-login support/contact should be public with spam controls. |

## Top 25 Opportunities

| Rank | Impact | Opportunity | Why it matters |
| ---: | --- | --- | --- |
| 1 | Very high | Make trust signals first-class on every listing. | Trust is the marketplace differentiator and reduces tenant hesitation. |
| 2 | Very high | Build Admin Control Tower v2. | Operations queues and SLA views convert the new ops center into a daily command surface. |
| 3 | Very high | Add role-aware AI evaluation and coverage analytics. | Turns RoomBay AI from a demo into a reliable platform brain. |
| 4 | Very high | Instrument full marketplace funnel. | Lets the team optimize supply, search, applications, approvals, leases, and payments. |
| 5 | High | Launch Cameroon city/neighborhood SEO clusters. | Organic acquisition is high-leverage for housing search. |
| 6 | High | Add landlord onboarding checklist. | Improves listing quality and supply activation. |
| 7 | High | Add tenant application readiness checklist. | Improves application completion and reduces support. |
| 8 | High | Add verification queue SLAs and templates. | Shortens trust bottlenecks. |
| 9 | High | Build AI unanswered-question workflow. | Converts support and failed AI answers into docs and product improvements. |
| 10 | High | Add manual payment reconciliation dashboard. | Reduces fraud and admin workload. |
| 11 | High | Add listing quality score. | Improves feed quality, SEO, and conversion. |
| 12 | High | Add report/reputation linkage. | Helps identify abusive users and risky listings early. |
| 13 | High | Add saved-search and notification conversion loops. | Drives repeat visits and faster tenant matching. |
| 14 | High | Add supply/demand heatmaps by city and neighborhood. | Helps operations recruit landlords where tenant demand exists. |
| 15 | Medium | Add onboarding tours for admin, landlord, tenant. | Reduces confusion in feature-rich dashboards. |
| 16 | Medium | Add lease status timeline. | Makes lease/payment/signature flow easier to understand. |
| 17 | Medium | Add AI admin runbook assistant mode. | Speeds triage when grounded in internal docs. |
| 18 | Medium | Add SEO landing pages for universities and neighborhoods. | Captures high-intent searches. |
| 19 | Medium | Add structured listing media requirements. | Better listing cards, trust, and SEO. |
| 20 | Medium | Add notification preference center. | Improves engagement without spam. |
| 21 | Medium | Add landlord performance dashboard. | Encourages faster responses and better listings. |
| 22 | Medium | Add tenant safety education moments. | Builds trust and reduces scams. |
| 23 | Medium | Add admin audit history UI depth. | Improves accountability. |
| 24 | Low | Add public pricing/verification policy docs. | Reduces ambiguity before monetization. |
| 25 | Low | Add internal release checklist. | Keeps production fixes from regressing. |

## Security Risk Register

| Area | Risk | Current control | Gap | Priority |
| --- | --- | --- | --- | --- |
| JWT and refresh tokens | Token theft via XSS/localStorage | Short-lived access token, refresh sessions | Tokens stored in localStorage | High |
| Logout | Refresh session revocation | Refresh token session repository | Needs stronger device/session UI | Medium |
| OAuth | Redirect/session complexity | OAuth success handler and role stashing | Needs full redirect regression tests | Medium |
| Password reset | Token-based reset | Reset endpoints and email service | Needs abuse limits and reset telemetry | Medium |
| Authorization | IDOR via UUID changes | Many service-level checks | No complete route/entity test matrix | High |
| Admin access | Privileged endpoints | `/api/admin/**` and `@PreAuthorize` | Admin MFA/session policy missing | High |
| WebSockets | Unauthenticated STOMP connection | JWT parsing interceptor | Invalid auth is not rejected | Critical |
| WebSocket destinations | User queue subscription abuse | User-specific sends exist | No explicit channel authorization rules visible | High |
| Uploads | Polyglot/malware files | MIME and size checks | No magic-byte/AV scanning | High |
| Rate limiting | Bot traffic and brute force | Auth filter, advanced interceptor, AI limiter | Local fallback, Redis disabled | High |
| Logging | Secret leakage | `AppErrorLogService` redaction | Need periodic redaction tests and Sentry scrubbers | Medium |
| AI | Prompt/data leakage | Role retrieval policy, sanitizer, output guard | No red-team eval gate | Medium |

## Production Readiness Audit

### Strengths

- Production profile uses Flyway and `ddl-auto=validate`.
- Admin Operations Center v1 exposes health, recent errors, funnel, alerts, and AI stats.
- Sentry is configured with `send-default-pii=false`.
- Backend Docker image runs as non-root and copies docs into `/app/docs`.
- Deploy docs include smoke checks and AI ingest validation.

### Missing controls

- Out-of-band alert delivery for critical ops alerts.
- Database backup policy, restore drill, and RPO/RTO targets.
- CI gate that runs backend focused schema/security tests and frontend build.
- Production startup guards for mandatory Cloudinary, JWT, mail, OAuth, and admin seed settings.
- Runbook for incident severity, rollback, customer comms, and postmortems.

## Admin Control Tower v2

Admin Operations Center v1 is a good foundation, but it is still mostly health and metrics. v2 should become the daily operating surface.

### Required modules

- Operations: health, database, WebSocket, AI, deploy version, uptime, 5xx rate, background jobs, backup freshness.
- Queues: pending verifications, listings, reports, payments, support tickets, stale applications, stale landlord responses.
- AI: docs/chunks/entities/edges, last ingest, top no-answer questions, source distribution, retrieval hit rate, role leakage checks.
- Growth: user growth, listing growth, city supply, search demand, listing views, saves, applications, approvals, leases.
- Audit: admin action history, moderation decisions, payment verification decisions, verification review notes, seed/admin lifecycle events.

### v2 gaps to close

- Add SLA thresholds per queue, not only counts.
- Add direct links from alerts to the exact admin queue or error table.
- Add alert acknowledgements and owner assignment.
- Add daily/weekly ops digest.
- Add safe stack trace reveal only behind admin debug permissions.

## AI Platform Roadmap

### Current maturity

RoomBay AI has the right v1 skeleton: authenticated user context, role-aware retrieval, GraphRAG, pgvector, docs packaging, no-doc fallback, memory hooks, output guards, admin ingest, and coverage analytics. The main risk is not architecture. It is answer quality, evaluation discipline, and future tool safety.

### Phase 1: Knowledge quality

- Keep expanding curated docs for tenant, landlord, admin, internal, and security.
- Add source ownership and review dates to docs.
- Add top no-answer questions to admin AI analytics.
- Add role-based golden evaluation set.

### Phase 2: Conversation memory

- Enable short-term memory only after privacy review.
- Store memory as summarized, non-sensitive state.
- Let users clear assistant memory.
- Keep admin memory separate from tenant/landlord memory.

### Phase 3: Tool calling

- Start with read-only tools: find listing status, explain application state, show queue counts.
- Require strict role policy per tool.
- Log every tool call with user, role, tool, input summary, and result summary.
- Add dry-run responses before any mutation tool.

### Phase 4: Platform copilot

- Add safe workflow assistants for tenant applications, landlord listing quality, and admin triage.
- Use deterministic backend tools for actions; never let the model invent state.
- Require confirmation for state changes.

### Phase 5: AI operations assistant

- Admin-only incident assistant grounded in ops docs, recent errors, ingest state, and alerts.
- No raw secrets, tokens, private docs, or unrestricted database access.
- Add incident summary export for postmortems.

## Marketplace Trust Roadmap

### Trust Layer v1

- Show listing trust tier consistently in cards, detail pages, search, and AI answers.
- Explain what landlord/listing verification means without exposing private document details.
- Add clear report listing/user actions.
- Add admin queue SLAs for verification, listing review, reports, and payments.

### Trust Layer v2

- Add landlord reliability score: verified status, response time, completed leases, reports, payment disputes.
- Add tenant readiness score visible to tenant, not as a public label.
- Add listing quality score: photos, price clarity, location, amenities, video, verification, policy completeness.
- Add fraud heuristics: duplicate listings, repeated phone numbers, suspicious price deltas, repeated reports.

### Trust Layer v3

- Add marketplace reputation graph across listings, landlords, tenants, reviews, reports, and payments.
- Add risk-based review routing.
- Add automated abuse prevention recommendations for admins.
- Add public trust center and safety education pages.

## UX Audit: Top 20 Improvements

1. Add tenant application readiness checklist.
2. Add landlord listing quality checklist before publish.
3. Add admin queue landing page with urgent counts.
4. Add lease/payment/signature timeline.
5. Add clear trust badges and explanations on listing cards.
6. Add empty states for applications, leases, payments, messages, and saved homes.
7. Add "why am I seeing this listing" chips in feed/recommendations.
8. Add AI suggested actions that deep-link to exact screens.
9. Add verification rejection recovery flows with examples.
10. Add pending listing review status for landlords.
11. Add payment proof status and expected review time.
12. Add support ticket status for users.
13. Add onboarding tours by role.
14. Add mobile-first admin queue filters.
15. Add unread and stale conversation indicators.
16. Add saved search notification preferences.
17. Add city/neighborhood landing pages with real listing previews.
18. Add landlord response time nudges.
19. Add admin audit timeline on user/listing/report detail pages.
20. Add consistent error boundaries and retry states around dashboard widgets.

## Growth and SEO Roadmap

### Current state

The app has public browsing routes, city pages, sitemap generation, robots rules, structured-data helpers, and listing sitemap inclusion. This is a good technical base, but RoomBay needs content depth and listing quality discipline to compete in search.

### 90-day SEO roadmap

#### Days 1-30

- Finalize robots and sitemap rules after the current SEO local changes are reviewed.
- Add canonical metadata for city, neighborhood, listing, and search routes.
- Add listing schema to listing detail pages with price, city, images, availability, and property type.
- Add SEO completeness checks to listing creation.
- Create pages for Douala, Yaounde, Buea, Soa, Bastos, Bonamoussadi, Logbessou.

#### Days 31-60

- Publish university housing pages and neighborhood guides.
- Add internal links from city pages to neighborhoods and listing searches.
- Add FAQ schema for trust, payments, verification, leases, and safety.
- Add search demand analytics by city/neighborhood/query.

#### Days 61-90

- Launch landlord acquisition content.
- Build programmatic pages for "rooms near [university]" and "student housing in [neighborhood]".
- Add content refresh process from marketplace analytics.
- Track organic conversion from city page to listing view to application.

## Analytics Roadmap

### North Star Metrics

- Tenant north star: qualified applications submitted per active tenant.
- Landlord north star: verified active listings with at least one qualified application.
- Marketplace north star: leases signed from verified listings.
- AI north star: grounded answers that lead to successful user actions.

### Dashboard model

- Executive dashboard: users, active listings, applications, approvals, leases, payments, revenue, trust queue SLAs.
- Growth dashboard: acquisition, SEO pages, city demand, conversion rates, retention.
- Marketplace dashboard: supply/demand balance, listing quality, landlord response, application outcomes.
- AI dashboard: questions, grounded rate, no-answer rate, top missing docs, source distribution.
- Ops dashboard: health, errors, incidents, queue backlog, alert acknowledgements.

### Implementation roadmap

- Add event data dictionary and QA tests.
- Add `analytics_event` deduping rules for high-volume events.
- Add UTM/source attribution to public traffic.
- Add daily rollups for fast dashboards.
- Add admin export for monthly operating review.

## Monetization Review

### Most realistic revenue opportunities

1. Premium landlord listing boosts after trust baseline is stable.
2. Verified landlord subscription for professional managers.
3. Featured listings by city/neighborhood with strict quality rules.
4. Tenant convenience services around lease/payment/admin support, not core access.
5. Verification or onboarding service fees for landlords, only after trust value is clear.
6. Advertising partnerships with movers, internet providers, cleaning, and student services.

### Monetization guardrails

- Do not monetize trust badges in a way that weakens safety.
- Do not let paid listings bypass review.
- Do not charge tenants for basic access to safe housing search.
- Keep payment proof verification conservative before adding revenue products.

## 30-Day Roadmap

1. Fix WebSocket auth rejection and add STOMP auth tests.
2. Rotate all previously exposed secrets and disable admin seed after confirmed admin creation.
3. Add IDOR regression tests for applications, leases, payments, profiles, messages, notifications, listings, reviews, verifications, reports.
4. Add upload magic-byte validation and Cloudinary production startup guard.
5. Enable Redis-backed rate limiting or document single-instance limits before scaling.
6. Add backup and restore runbook with first restore drill.
7. Build Admin Control Tower v2 queue summary cards and queue deep links.
8. Add AI top no-answer questions and role-based golden evals.
9. Fix existing unrelated full-suite test failures so CI can become reliable.
10. Finalize SEO robots/sitemap/canonical changes and add listing schema validation.

## 60-Day Roadmap

1. Add alert delivery through Sentry/email/webhook for critical ops events.
2. Add queue SLAs and ownership in Admin Control Tower.
3. Add payment proof reconciliation controls and duplicate transaction checks.
4. Add listing quality score and landlord onboarding checklist.
5. Add tenant application readiness checklist.
6. Add AI stale-ingest detection and scheduled ingest verification.
7. Add city/neighborhood SEO content clusters.
8. Add analytics event QA dashboard and daily funnel rollups.
9. Add frontend security headers and CSP for the Next.js app.
10. Add moderation reason taxonomy and repeat-offender reporting.

## 90-Day Roadmap

1. Launch Trust Layer v1 across listing cards, listing detail, search, and AI answers.
2. Add landlord reliability metrics and response-time tracking.
3. Add marketplace supply/demand heatmaps.
4. Add AI admin runbook assistant mode with read-only tools.
5. Add support-ticket status tracking for users and admins.
6. Add SEO university pages and FAQ schema.
7. Add lifecycle email/notification loops for saved searches, stale applications, and landlord response nudges.
8. Add admin audit detail pages for users, listings, reports, and payments.
9. Add backup freshness and restore-drill status to ops dashboard.
10. Add monthly executive marketplace review dashboard.

## 6-Month Roadmap

1. Trust Layer v2: landlord reliability score, listing quality score, fraud heuristics, moderation intelligence.
2. AI Phase 3: safe read-only tool calling with full audit logs.
3. Marketplace growth engine: city acquisition playbooks, landlord recruitment, SEO content operations.
4. Monetization v1: premium listing boosts and verified landlord subscription pilots.
5. Scale readiness: Redis required, background job queue, OpenSearch rollout, dashboard rollups, production SLOs.
6. Security maturity: admin MFA, httpOnly token migration assessment, full object-level auth test coverage, annual penetration test.
7. Reliability maturity: incident process, rollback drills, database restore drills, error budget tracking.

## Recommended Order of Execution

1. Security and reliability first: WebSocket rejection, secret rotation, upload hardening, IDOR tests, backup restore drill.
2. Operational control second: Admin Control Tower v2 queues, alert delivery, queue SLAs, payment reconciliation.
3. Trust third: listing trust explanations, landlord verification clarity, listing quality score, report/reputation improvements.
4. Analytics fourth: event QA, rollups, executive/growth/marketplace dashboards.
5. AI fifth: evaluation, no-answer workflow, stale ingest detection, then read-only tool calling.
6. Growth sixth: SEO clusters, city pages, saved-search loops, landlord acquisition.
7. Monetization last: only after trust, supply quality, and conversion instrumentation are stable.

## Deferred Deeper Review

This report is a production-triage audit, not an exhaustive file-by-file certification. The next deep review should include:

- Full route-by-route IDOR test generation.
- STOMP subscription authorization testing.
- Dependency vulnerability scan for Maven and npm packages.
- Cloudinary transformation and delivery security review.
- AI red-team prompt evaluation.
- Payment fraud tabletop exercise.
- Disaster recovery tabletop and restore proof.
