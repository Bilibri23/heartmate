# RoomBay platform usage (RAG knowledge)

How tenants, landlords, and admins use RoomBay. Keep answers aligned with the live app, not hypothetical features.

## Vision and what makes RoomBay different (canonical for “why unique” questions)

- **North-star vision:** Make **finding a home as easy as booking a ride or buying something on Amazon** — fast, low-friction discovery and clear next steps, not a maze of forms before you see value.
- **Feed-first discovery:** The **For You feed** (and reels-style vertical browsing where enabled) is designed like **short-form video discovery (e.g. TikTok-style)** so users can skim many options quickly with **minimal friction** and **make a first decision in seconds** (often around **~2 seconds** to know “worth a closer look or skip”), then drill into listing detail, search, or apply when ready.
- **Trust without wrong claims:** Uniqueness is **not** “landlords manually verify tenant ID selfies.” Tenant identity documents are an **admin-reviewed** trust layer; landlords work with **applications and leases** in the product (see Privacy section below).

## Privacy: who sees verification and lease documents (canonical)

- **Tenant / student verification (government ID + selfie):** Submitted under **tenant verification** for **admin review**. **Landlords do not see** the tenant’s verification document images or selfie. Do **not** tell users that landlords review or view those ID/selfie uploads.
- **What landlords typically see for tenants:** Application information the product exposes, and **lease-related flow and documents** (e.g. signed lease / lease docs the workflow attaches for the landlord side) — **not** the tenant’s raw identity verification packet.
- **Landlord verification:** Landlord KYC documents are reviewed by **admin** (separate flow from tenant verification).
- **Payment proof:** May be visible to **admin** (and landlord where the app surfaces payment/lease status) per the payments UX — do not equate with tenant ID/selfie visibility.

## Tenant (role STUDENT in API)

- **Discover:** Use **For You** for the feed and **Search** for filters. Save places with **favorites** (heart).
- **Listing detail:** Photos, video tour if present, rent, location. **Message** the landlord from the app.
- **Apply:** Submit an application from the listing; track status under **My Apps** (`/applications`).
- **Verify identity:** `/verification` — government ID + selfie (or legacy campus path where applicable).
- **Roommates:** Set preferences in profile/preferences; **Matches** suggests compatible roommates. Share a listing link; each person applies separately (no mandatory joint application).
- **Lease & payments:** If the landlord accepts you, you may **sign a lease** and **submit payment proof** (e.g. Mobile Money screenshot) for admin verification when required.

## Landlord

- **Dashboard:** `/landlord` — listings, applications, tenants linked to leases.
- **Listings:** Create/edit under landlord listings; listings may need **admin approval** before going ACTIVE.
- **Applications:** Review and shortlist or reject under `/landlord/applications`.
- **Tenants:** Shown from **active leases**, not a separate informal list.
- **Tenant identity docs:** Landlords **do not** access tenant **verification** ID/selfie uploads; those are **admin-only** for review.
- **Payments:** See incoming payments and tenant proof where applicable under landlord payments.

## Admin

- **Verifications:** Approve or reject tenant and landlord ID submissions after viewing documents.
- **Listings:** Approve or reject pending listings; review **all photos** before approval.
- **Payments:** Verify or reject **payment proof** images for submitted payments.
- **Reports:** Triage user/listing reports under **Reports** (`/admin/reports`): set priority, resolve or dismiss.
- **Support messages:** Help and Support form submissions appear under **Support** (`/admin/support-inquiries`), separate from moderation reports.
- **Search:** If Elasticsearch is enabled and search is empty, run **Search Reindex** from Admin Settings.

## AI assistant

- Answers are **grounded in ingested markdown** from the `docs/` folder (RAG over **pgvector** in PostgreSQL). After changing docs, an admin runs **Ingest Docs** in Settings.
