# System Gaps Beyond Payments and Recommendations

This document highlights broader gaps and provides a pragmatic roadmap.

## Security and Access Control
- Role-based access: ensure tenants only access their leases/payments; landlords their own properties; admins have audit trails.
- Input validation: enforce strict server-side validation (lengths, formats, enums) plus descriptive errors.
- Rate limiting and abuse protection on submission endpoints to prevent spam/fraud.
- Secrets management: environment-based config with secrets in a secure vault; rotate regularly.
- Logging hygiene: never log full PII or secrets; mask phone numbers and tokens.

## Data and Storage
- PII governance: retention policy for phone numbers and proofs; allow deletion after defined period.
- File storage: use a secure object store with short-lived pre-signed URLs; AV scanning recommended.
- Indexing: add DB indexes on foreign keys and search fields (leaseId, payerId, status, createdAt).
- Idempotency: enforce unique reference/idempotency keys to prevent duplicate payments.

## Reliability and Observability
- Centralized error handling and meaningful API error shapes.
- Metrics: request rates, error rates, latency; business metrics for payment statuses.
- Tracing: correlate frontend actions to backend requests for supportability.
- Alerts for: verification backlog breaches, high rejection rates, storage/upload failures.

## UX and Accessibility
- Input masks and inline validation (phone number, required fields).
- Clear empty states and loading states; reduce cognitive load in forms.
- Accessible components: ARIA roles for icons, screen reader text, minimum color contrast.
- Receipts and status pages tenants can revisit; export for landlords.

## Performance and Scale
- Pagination on all list endpoints; safe sorts.
- Caching read-heavy endpoints when feasible.
- Asynchronous tasks for heavy operations (image processing, notifications).

## Testing
- Unit tests for validation and state transitions (PENDING → SUBMITTED → VERIFIED/REJECTED).
- Integration tests for file uploads and permissions.
- E2E flows covering tenant/landlord/admin happy paths and failure scenarios.

## DevOps
- CI with static analysis, tests, and artifact builds.
- Blue/green or rolling deploys; database migrations via a tool.
- Backup strategy and disaster recovery plan.

## Roadmap (Prioritized)

Week 1–2 (MVP hardening)
- Add server-side validation and consistent error responses.
- Enforce idempotency and prevent duplicate submissions per lease/reference.
- Improve file upload security (pre-signed URLs, size/type limits).
- Basic admin dashboard for verification with filters and notes.
- Observability baseline: logs, metrics, simple alerts.

Week 3–4 (UX and Ops)
- Input masks, better inline validation, receipts.
- SLA tracking and backlog alerting.
- Role-based access tightening and audit logs for admin actions.

Month 2 (Growth)
- Introduce landlord subscription (starter/pro) or begin manual monthly invoicing.
- Evaluate request-to-pay integration for one provider to reduce manual verification.
- Add automated expiry/re-notification for stale PENDING submissions.

Month 3 (Scale)
- If integrating provider payments: webhook verification, retries, and reconciliation jobs.
- Expand analytics: payment funnel, rejection reasons, time-to-verify distributions.
- Security review and threat modeling; implement prioritized fixes.
