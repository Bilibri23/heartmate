# Security Hardening Runbook

## Phase 0 Immediate Checklist

### Change freeze and review
- Enforce branch protection on `main`.
- Require security reviewer approval for changes touching:
  - auth (`SecurityConfig`, JWT, auth controller/service)
  - listings ownership and mutations
  - matching decision logic
  - payment and webhook handling

### Secret rotation
- Rotate these credentials immediately:
  - `JWT_SECRET`
  - `CLOUDINARY_CLOUD_NAME`
  - `CLOUDINARY_API_KEY`
  - `CLOUDINARY_API_SECRET`
  - payment/webhook secrets in deployment environment
- Invalidate old credentials.
- Verify old credentials fail in staging before production rollout.

### Startup guard verification
- Start backend in `prod` profile with one required secret removed.
- Confirm startup fails with a missing-required-config message.
- Restore secret and confirm startup succeeds.

### Audit logging verification
- Execute one action from each flow and verify `AUDIT` logs exist:
  - listing create/update/delete
  - admin listing approve/reject
  - match accept/reject

## Release Gate
- No hardcoded sensitive defaults in committed config.
- All production secrets sourced from environment variables.
- Non-dev startup fails when required secret/config is missing.
