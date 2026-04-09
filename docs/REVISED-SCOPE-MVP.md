# Roombay Revised Scope (MVP)

**Purpose:** Align product scope with real user problems. Remove features that don't serve the core value; fix gaps that block admin/landlord/tenant workflows.

**Date:** March 2025

**Canonical engineering docs:** This file plus [`PRODUCT-BACKLOG.md`](PRODUCT-BACKLOG.md) define MVP scope. Implementation tracking: [`mvp-vision-vs-implementation-gaps.md`](mvp-vision-vs-implementation-gaps.md). **VPS / go-live checklist (4 items):** [`NEXT-TODOS.md`](NEXT-TODOS.md). AI RAG retrieval uses **pgvector** in PostgreSQL (see [`ai-faq.md`](ai-faq.md)).

---

## 1. User Personas (Revised)

| Persona | Description | Rename |
|---------|-------------|--------|
| **Tenant** | Anyone looking for a room (not limited to students). Uses roommate matching for shared listings. | Rename from "Student" in UI and code where user-facing. |
| **Landlord** | Property owners who list and manage rentals. | No change |
| **Admin** | Platform admin for approvals and verifications. | No change |

---

## 2. In Scope (MVP)

### Tenant
- Register, login, profile
- Search listings (filters, Elasticsearch)
- View listing details, favorites
- Apply to listings
- **Share listing with matched roommate** (simplified from co-apply/invite)
- Roommate matching
- Lease signing
- Payment submission (proof upload) and status tracking
- Messages (basic)
- Reviews (basic)
- Preferences (budget, move-in date, etc.)
- **Tenant verification** (general: gov ID + selfie, not student-specific)

### Landlord
- Register, login, profile
- Create listing (draft → submit for approval)
- Admin approval → listing goes ACTIVE
- View received applications
- Approve application → create lease
- **Tenant management** (derived from leases: who is on which lease)
- **Payments** (view submissions, status, history per lease)
- **Analytics** (basic: views, applications count)
- Landlord verification (identity, business, property docs)

### Admin
- Login
- **Listing approval** – view listing details + images before approve/reject
- **User verification** – view uploaded images (tenant ID, landlord ID, selfie) before approve/reject
- **Payment verification** – view payment proof image before verify/reject
- **Reports** – basic triage (view, status, resolve)
- Search reindex (Elasticsearch)
- User management (suspend, activate)

---

## 3. Out of Scope (MVP)

| Feature | Action | Reason |
|---------|--------|--------|
| **Household** | Remove | Not in MVP. Expenses, tasks, rules, disputes between roommates deferred. |
| **Disputes** | Remove from MVP | Not in MVP. Admin dispute management deferred. |
| **Co-apply / Invitations** | Simplify | Replace with "share listing with matched roommate" only. |
| **Student-specific verification** | Generalize | Replace university/student ID with general gov ID + selfie. |
| **Advanced analytics** | Defer | Basic stats only for MVP. |
| **Application automation** | Defer | Manual review for MVP; automate as scale. |
| **Verification automation** | Defer | Manual admin review for MVP; integrate third-party (e.g. Onfido, Plaid) later. |

---

## 4. Fixes Required (Gaps)

### Admin
| Gap | Fix |
|-----|-----|
| Cannot view payment proof when verifying | Add `paymentProofUrl` to admin payment response; display in admin payments sheet. |
| Listing approval – unclear if images visible | Ensure listing detail sheet shows all images when approving. |
| User verification – images may not load | Verify admin verifications page shows tenant/landlord images. If broken, fix URLs or CORS. |
| Reports management | Implement basic triage (view report, mark resolved, set priority). |
| Disputes | Remove from admin nav and flows; keep backend for future. |

### Landlord
| Gap | Fix |
|-----|-----|
| Tenant management inaccurate | Derive tenant list from leases (lease → tenant mapping). Display correct tenants per lease. |
| Payments lacking | Show payment status per lease, history, proof URL. |
| Analytics lacking | Add basic stats: listing views, application count per listing. |

### Tenant
| Gap | Fix |
|-----|-----|
| Verification too narrow (student-only) | Generalize: gov ID (passport/ID card) + selfie. Remove university/student ID requirement. |
| Co-apply complex | Simplify to "share listing with matched roommate" – send link or notification. |
| Household | Remove from tenant nav and flows. |
| Payments | Align with landlord payment flow; clear status (Submitted → Verified/Rejected). |

---

## 5. Technical Debt / Cleanup

| Item | Action |
|------|--------|
| `UserRole.STUDENT` | Keep enum for now; rename in UI to "Tenant". Consider migration later. |
| Household entities, controllers, services | Deprecate or hide. Remove from nav. Optionally keep DB tables for future. |
| Dispute entities, controllers | Remove from admin nav. Keep backend for future. |
| Verification entity | Extend for general ID (not just student ID). |

---

## 6. Future (Post-MVP)

- Verification automation (Onfido, Jumio, Plaid)
- Application review automation
- Household (expenses, tasks) – reintroduce if needed
- Disputes – full admin workflow
- Advanced analytics
- Request-to-pay integration (MTN/Orange webhooks)
