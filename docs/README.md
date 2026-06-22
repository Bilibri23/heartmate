# RoomBay documentation index

Authoritative product and operations docs for humans, admins, and the in-app AI (RAG).

## Source-of-truth hierarchy

1. **`docs/`** (this tree) — product behavior, runbooks, deploy
2. **Code** — implementation details when docs are silent
3. **[AGENTS.md](../AGENTS.md)** — engineering conventions for AI agents
4. **`track_dev/`** — historical only; do not treat as current

After editing docs, re-ingest for AI: [scripts/rag-ingest-local.ps1](../scripts/rag-ingest-local.ps1)

## Public

| Doc | Topic |
|-----|-------|
| [public/platform-overview.md](public/platform-overview.md) | What RoomBay is |
| [public/trust-and-safety.md](public/trust-and-safety.md) | Trust and safety |
| [public/verification-and-trust-signals.md](public/verification-and-trust-signals.md) | Verification badges |
| [public/payments-leases-messaging-support.md](public/payments-leases-messaging-support.md) | Payments, leases, messaging |
| [public/support-and-account-access.md](public/support-and-account-access.md) | Account access |
| [public/troubleshooting-common-issues.md](public/troubleshooting-common-issues.md) | Common issues |

## Tenant

| Doc | Topic |
|-----|-------|
| [tenant/search-and-feed-workflow.md](tenant/search-and-feed-workflow.md) | Search and feed |
| [tenant/discovery-search-and-saved-homes.md](tenant/discovery-search-and-saved-homes.md) | Discovery and saved homes |
| [tenant/saved-homes-and-notifications.md](tenant/saved-homes-and-notifications.md) | Favorites and notifications |
| [tenant/applications-verification-and-leases.md](tenant/applications-verification-and-leases.md) | Apply, verify, leases |
| [tenant/payments-and-lease-status.md](tenant/payments-and-lease-status.md) | Rent payments |
| [tenant/roommates-payments-and-safety.md](tenant/roommates-payments-and-safety.md) | Roommates |
| [tenant/troubleshooting-applications-and-messaging.md](tenant/troubleshooting-applications-and-messaging.md) | Troubleshooting |

## Landlord

| Doc | Topic |
|-----|-------|
| [landlord/onboarding-and-listing-creation.md](landlord/onboarding-and-listing-creation.md) | Onboarding |
| [landlord/listing-management-workflow.md](landlord/listing-management-workflow.md) | Manage listings |
| [landlord/applications-and-tenant-communication.md](landlord/applications-and-tenant-communication.md) | Applications and chat |
| [landlord/verification-applications-and-leases.md](landlord/verification-applications-and-leases.md) | Verification through lease |
| [landlord/leases-and-payouts-workflow.md](landlord/leases-and-payouts-workflow.md) | Leases and payouts |
| [landlord/payments-payouts-and-listing-trust.md](landlord/payments-payouts-and-listing-trust.md) | Payments and trust |
| [landlord/troubleshooting-listings-and-applications.md](landlord/troubleshooting-listings-and-applications.md) | Troubleshooting |

## Admin

| Doc | Topic |
|-----|-------|
| [admin/ai-admin-runbook.md](admin/ai-admin-runbook.md) | AI admin queues |
| [admin/ai-coverage-and-roadmap.md](admin/ai-coverage-and-roadmap.md) | AI coverage and gaps |
| [admin/listing-review-runbook.md](admin/listing-review-runbook.md) | Listing review |
| [admin/moderation-and-verification-queues.md](admin/moderation-and-verification-queues.md) | Moderation queues |
| [admin/incident-triage-and-diagnostics.md](admin/incident-triage-and-diagnostics.md) | Incidents |
| [admin/support-triage-runbook.md](admin/support-triage-runbook.md) | Support triage |
| [admin/master-audit-and-execution-roadmap.md](admin/master-audit-and-execution-roadmap.md) | Audit roadmap |

## Deploy and security

| Doc | Topic |
|-----|-------|
| [deploy/production-deploy.md](deploy/production-deploy.md) | Railway + Vercel + VPS |
| [deploy/database-backup-restore-runbook.md](deploy/database-backup-restore-runbook.md) | DB backup/restore |
| [deploy/secret-rotation-runbook.md](deploy/secret-rotation-runbook.md) | Secret rotation |
| [security/security-hardening-runbook.md](security/security-hardening-runbook.md) | Hardening |
| [security/abuse-prevention-and-data-handling.md](security/abuse-prevention-and-data-handling.md) | Abuse prevention |

## Internal

| Doc | Topic |
|-----|-------|
| [internal/platform-maintenance-procedures.md](internal/platform-maintenance-procedures.md) | Maintenance |
| [internal/ai-errors-and-fixes.md](internal/ai-errors-and-fixes.md) | AI error catalog |
