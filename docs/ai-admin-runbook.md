# Admin Operations Runbook (Production)

## Queue priority
1. Payment verifications
2. User verifications
3. Listing approvals
4. Reports triage

## A1: Tenant verification review
### Checklist
- ID image readable
- ID number plausible
- Selfie appears to match ID
- No tampering signs

### Actions
- Approve -> `VERIFIED`
- Reject -> `REJECTED` with reason

## A2: Landlord verification review
- Validate identity docs and consistency.
- Approve/reject with explicit reason.

## A3: Listing approval review
### Checklist
- Photos present and relevant
- Description coherent
- Rent/location plausible
- No prohibited/spam content

### Actions
- Approve -> `ACTIVE`
- Reject -> `REJECTED`

## A4: Payment proof verification
### Checklist
- Proof image visible and legible
- Amount/reference align with expected payment

### Actions
- Verify or reject with reason

## A5: Reports triage
- Status path: `OPEN` -> `IN_PROGRESS` -> `RESOLVED`
- Set priority and document actions.

## A6: Search operations
- If approved listings missing from search: run reindex from Admin Settings.

## A7: User moderation
- Suspend/activate with auditable reasons.

## Incident quick checks
- Missing notifications: websocket + notification logs
- Missing images: media URL/CORS/storage checks
- AI bad answers: re-ingest docs and refresh runbooks
