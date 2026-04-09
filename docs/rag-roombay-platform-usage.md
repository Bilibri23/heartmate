# RoomBay platform usage (RAG knowledge)

How tenants, landlords, and admins use RoomBay. Keep answers aligned with the live app, not hypothetical features.

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
- **Payments:** See incoming payments and tenant proof where applicable under landlord payments.

## Admin

- **Verifications:** Approve or reject tenant and landlord ID submissions after viewing documents.
- **Listings:** Approve or reject pending listings; review **all photos** before approval.
- **Payments:** Verify or reject **payment proof** images for submitted payments.
- **Reports:** Triage user/listing reports under **Reports** (`/admin/reports`): set priority, resolve or dismiss.
- **Search:** If Elasticsearch is enabled and search is empty, run **Search Reindex** from Admin Settings.

## AI assistant

- Answers are **grounded in ingested markdown** from the `docs/` folder (RAG over **pgvector** in PostgreSQL). After changing docs, an admin runs **Ingest Docs** in Settings.
