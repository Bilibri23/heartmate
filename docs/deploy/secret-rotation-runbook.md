# RoomBay Secret Rotation Runbook

## Purpose

Rotate secrets that were exposed outside the deployment environment and make production startup fail if unsafe seed or unresolved variables remain.

## Rotate Immediately

1. JWT signing secret
   - Generate a new 32+ byte random value.
   - Set `JWT_SECRET` in Railway.
   - Deploy backend.
   - Expect all existing sessions to be invalidated.

2. Cloudinary
   - Rotate the API secret in Cloudinary.
   - Update `CLOUDINARY_API_SECRET` in Railway.
   - Verify uploads from landlord verification and listing media.

3. Mail account
   - Revoke the exposed Gmail app password.
   - Generate a new app password.
   - Update `MAIL_PASSWORD` in Railway.
   - Send a password-reset email smoke test.

4. Google OAuth
   - Rotate `GOOGLE_CLIENT_SECRET` in Google Cloud Console.
   - Update Railway.
   - Verify Google sign-in through `https://www.roombay.app`.

5. Sentry
   - Rotate DSN only if project access or DSN exposure policy requires it.
   - Keep `sentry.send-default-pii=false` in production.

6. Admin seed
   - Confirm the production admin user already exists.
   - Set `APP_ADMIN_SEED_ENABLED=false`.
   - Remove `APP_ADMIN_SEED_PASSWORD` from Railway if no longer needed.
   - Production now fails startup if admin seeding is enabled.

## Post-Rotation Smoke Checks

- `GET /actuator/health` returns `UP`.
- Login with email/password works.
- Google OAuth works.
- Listing image upload works.
- AI chat still works.
- Admin Operations Center loads.
- Recent errors do not show secrets, tokens, cookies, passwords, or Authorization headers.

## Rollback

Do not roll back to exposed secrets. If a rotation breaks production, create a fresh replacement secret and redeploy.
