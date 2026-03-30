# Discovery & feed architecture (RoomBay)

This document records product–engineering decisions aligned with the discovery roadmap: accurate IA facts, a chosen unification strategy, rules for a future “express interest” action, and why package-level splits are deferred.

## 1. Navigation facts (validate roadmap copy)

- **Primary mobile navigation (students)** is **five items**, not “18 tabs”: For You, Search, My Apps, Messages, Profile (`frontend/components/layout/bottom-nav.tsx`).
- **Cognitive overload** comes from **many reachable routes** (profile, onboarding, preferences, landlord/admin trees), deep links, and **duplicate discovery entry points** (`/for-you` vs `/search`), not from a crowded tab bar.
- **`/matches` is not a bottom-nav tab.** It is reached from profile (“Find Roommates”), listing detail, onboarding, and preferences. Roommate flows should stay **contextual**, not central.

## 2. Unification strategy (chosen)

**Decision:** Ship a **compound feed API** on the backend (`GET /api/feed`) first, keep **both** `/for-you` and `/search` URLs for now, and **drive both from the same listing card + filter primitives** where possible.

**Implementation:** [`FeedController`](../backend/src/main/java/org/rooms/roombay/controller/FeedController.java) + [`FeedService`](../backend/src/main/java/org/rooms/roombay/service/FeedService.java) — `sections=forYou,trending,recent` (optional `reels`), response includes `videoTourListingCount` for client-side reels gating. The For You page uses this endpoint in [`frontend/app/(main)/for-you/page.tsx`](../frontend/app/(main)/for-you/page.tsx).

**Rationale:**

- Merging into a single `/` route is a larger UX/design pass (avoid a busy above-the-fold).
- A **single feed response** removes multiple client round-trips for the home experience without forcing a big-bang route rename.
- A future **shared layout** for `/for-you` and `/search` can reuse components; URLs can stay for bookmarks and analytics.

**Alternatives not chosen for this phase:** Next.js-only BFF aggregation (duplicates composition until native clients need the same contract), or full route merge before the feed API exists.

## 3. Express interest semantics (before a dedicated endpoint)

If you add `POST /api/listings/:id/express-interest` (or similar), define these rules **up front**:

| Topic | Guideline |
| --- | --- |
| **Single landlord inbox** | Landlords see **one thread or queue per listing**, not parallel ambiguous pipelines. |
| **Deduping** | If a user already has an **application** for the listing, **do not** create a second “interest” row; surface status instead. |
| **Promotion** | “Interest” should **promote** to a full application when the tenant adds message/dates, or **expire** after N days if unused. |
| **Notifications** | One clear notification type (“Someone is interested”) with link to listing applications; avoid duplicate pushes for interest + apply. |

Until those rules exist, prefer **analytics events** (`listing_interest`) or **lighter validation on apply** over a second persisted entity.

## 4. Reels / video feed gating

- Reels are most valuable when **enough listings include a video tour**; an empty or single-item reel feed hurts trust.
- **UI rule:** Expose reels as a **mode** (e.g. on search) only when inventory meets a **minimum threshold**; otherwise show a short explanation or hide the control.
- Promoting reels to a **primary tab** should wait until content supply and performance metrics justify it.

## 5. Package structure: defer broad refactors

- **Do not** reorganize `org.rooms.roombay` into `discovery/`, `listing/`, etc. as a pre-launch project.
- **Do** add a **vertical slice**: `FeedController` + `FeedService` colocated with existing packages, calling `RecommendationService`, `ElasticsearchSearchService`, and `ListingService` only through existing services.
- Revisit package-by-domain **after** one slice proves boundaries and tests.
