# MVP Business Model and Flow

Decision: Pass-through payments (no funds flow through the platform).
- Tenants pay landlords directly (e.g., Mobile Money).
- The platform does NOT hold funds or orchestrate payouts in MVP.
- Monetization for MVP: start free to validate demand and UX; add either:
  - Subscription for landlords (monthly/annual), or
  - Off-platform success fee invoiced monthly (manual billing) until you can integrate split-pay later.

Why this choice:
- Zero treasury/compliance burden and no merchant account required to launch.
- Fast iteration and low cost while you learn usage patterns and refine UX.
- Can evolve later to request-to-pay and split fees once you have traction.

## Roles and User Journeys

Tenant
- Sees amount due and supported methods.
- Taps “Pay” and follows clear instructions (number + reference).
- Uploads proof (screenshot) and reference/transaction ID.
- Gets status: Submitted → Verified or Rejected. SLA: within 24 hours.

Landlord
- Configures payment destinations (MTN/Orange numbers, display name).
- Views incoming submissions, status, and notes.
- Receives notifications on new submissions and decisions.
- Can export payment history for their leases.

Admin/Support
- Reviews SUBMITTED payments with screenshots and metadata.
- Verifies or rejects with notes; audit log retained.
- Monitors queue length, SLA breaches, and common failure reasons.

## Payment Lifecycle (MVP)

Statuses:
- PENDING: Created by system for a lease awaiting payment or on “initiate”.
- SUBMITTED: Tenant uploaded proof with transaction details.
- VERIFIED: Admin approved; lease advanced and receipt issued.
- REJECTED: Admin refused; tenant receives reason and can resubmit.

Operational controls:
- Idempotency: prevent duplicate SUBMITTED entries for the same reference within a time window.
- Anti-fraud: require unique reference per payment; highlight mismatched amounts or suspicious patterns.
- Expiry: auto-expire long-stale PENDING after N days.

Data captured:
- Amount, currency (XAF), method, payer phone, transaction/reference id, proof URL, notes.
- Timestamps and verifier info, rejection reason, and raw metadata if needed.

## MVP Monetization

Phase 0 (MVP): Free to remove friction, focus on adoption and quality.
Phase 1: Subscription for landlords (starter and pro tiers) with feature gating:
- Starter: manual verification, basic dashboard, email notifications.
- Pro: priority verification, exports, advanced filters/analytics.
Alternative: Off-platform success fee via monthly invoice (simple at small scale).

Future: Move to platform-as-merchant or split-pay once you’re ready.
- Integrate request-to-pay (MTN/Orange) with webhooks for instant confirmation.
- Apply convenience fee at source or split payment between platform and landlord.

## UI/UX for MVP

- Clear steps with strong emphasis on the reference code and amount.
- Inline validation for phone (country code, length), required fields, and formats.
- Progress and status messaging (Submitted → Awaiting verification).
- Descriptive errors and helpful copy for rejections and resubmissions.
- Accessibility: keyboard focus states, ARIA labels for icons, color contrast, localized text.

## KPIs to Watch

- Payment submission-to-verification time (SLA adherence).
- Verification pass rate and top rejection reasons.
- Duplicate/forged proof rate (proxy for fraud).
- Landlord engagement: weekly active landlords, usage of exports.
- Tenant success rate: % who submit proof within 24 hours of initiation.

## Upgrade Path

- Add provider-initiated request-to-pay for at least one method (MTN first).
- Implement webhook verification and auto-updates to statuses.
- Add reconciliation jobs and a modest ledger if/when holding funds.
- Introduce fee collection (subscription or split-pay) once you have traction.
