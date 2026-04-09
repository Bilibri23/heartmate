# Onboarding and product flows (RoomBay)

This document aligns **tenant** and **landlord** journeys with common product patterns: show value before heavy commitment, reduce friction, and defer verification until users have a reason to trust the product.

## Principles

1. **Value before verification** — Let people browse listings, search, and understand the offer before blocking on email/SMS. Verification is a trust step for applications and payments, not a wall on first pixel.
2. **Progressive profiling** — Collect only what is needed for the current step (role, basics, then preferences, then verification for sensitive actions).
3. **OAuth preference** — Many users prefer **Continue with Google** over long forms; keep email/password as an alternative for markets and edge cases.
4. **Remind, don’t hard-block (by default)** — Backend flags (`app.verification.*`, `app.email.send-verification-on-register`) allow production to tighten policy without rewriting flows.

## Tenant (seeker) flow (target)

| Stage | User sees | Product goal |
| --- | --- | --- |
| Discover | Home, search, listing detail without account | Housing-first growth |
| Intent | Save, shortlist, apply, message | Account adds clear value |
| Account | Google or email register; optional onboarding wizard | Low friction |
| Depth | Roommate prefs, budget, areas | After they care |
| Trust | Phone/email verification when applying or paying | Verification tied to action |

Inspired patterns: **Airbnb** (browse → wishlist → book), **LinkedIn** (profile completion over time), **Duolingo** (lesson before account).

## Landlord flow (target)

| Stage | User sees | Product goal |
| --- | --- | --- |
| Value | Why list on RoomBay (reach, verification, payments) | Clear ROI |
| List | Create listing with minimal fields | Time-to-first-listing |
| Trust | Verification and payout details when money moves | Risk at the right step |

## Email verification timing

- **Immediate verification** increases drop-off when the user has not yet seen listings or matches.
- **Deferred verification** (default config: no verification email on register; API not gated by verification until you enable it) pairs with in-app reminders after meaningful actions (e.g. first application, first message).

## In-app guided tour (frontend)

First visit on **For You** (tenants) or **Landlord dashboard** starts a **[Shepherd.js](https://shepherdjs.dev/)** tour: modal overlay, step titles, Back/Next, and **cross-page navigation** (For You → Search with list/map/Reels → Profile menus → My Apps for tenants; Landlord dashboard → Search → Landlord profile for landlords). Targets use `data-tour` hooks on real components. Completion (including skip/close) is stored in `localStorage` as `roombay_shepherd_tenant_v2` / `roombay_shepherd_landlord_v2` (version bumps when steps change). Tours include the **RoomBay Assistant** (platform AI / RAG brain): floating chat, `data-tour="roombay-ai-assistant"`.

Shepherd is distributed under **AGPL-3.0**; if you ship a proprietary app, review license obligations or consider an alternative library for production builds.

Implementation: `PlatformTourProvider` in `app/providers.tsx`, tour definitions in `frontend/lib/tour/build-tours.ts`.

## Configuration (backend)

See `application.properties`:

- `app.verification.enforce-for-api` — When `true`, most authenticated `/api/**` calls require verified email and phone.
- `app.verification.require-for-login` — When `true`, password login requires both verifications first.
- `app.email.send-verification-on-register` — When `true`, send verification email right after register.

## Google OAuth

- Backend: `app.oauth.google.enabled=true`, `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`; redirect URI in Google Cloud Console: `{backend}/login/oauth2/code/google`.
- Frontend: `NEXT_PUBLIC_OAUTH_GOOGLE_ENABLED=true` to show “Continue with Google” / “Sign up with Google”; Next.js rewrites proxy `/oauth2/**` and `/login/oauth2/**` to the API origin.
