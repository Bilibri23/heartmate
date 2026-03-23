# Roombuddy Product Backlog

**Principle:** Each item ties to a real user problem. Prioritized by impact and dependency.

**Format:** Problem → Solution → Acceptance Criteria

---

## P0 – Blockers (Must fix before production)

### P0-1. Admin cannot verify payments without seeing proof
**Problem:** Admin verifies payments blindly. No way to view the payment screenshot before approve/reject. High risk of wrong decisions.

**Solution:** Display payment proof image in admin payments detail sheet. Include `paymentProofUrl` in API response; render image with open-in-new-tab option.

**Acceptance criteria:**
- Admin payments page shows proof image when viewing a SUBMITTED payment
- Image loads from Cloudinary/storage URL
- Admin can verify or reject with reason after viewing proof

---

### P0-2. Admin cannot properly verify users without viewing documents
**Problem:** Admin approves/rejects tenant and landlord verifications without seeing uploaded ID photos and selfies. Cannot validate authenticity.

**Solution:** Ensure admin verifications page displays all uploaded images (tenant: gov ID; landlord: ID front, selfie with ID). Fix any broken URLs or CORS.

**Acceptance criteria:**
- Tenant verification detail shows gov ID image (and selfie if applicable)
- Landlord verification detail shows ID front + selfie with ID
- Images load and are viewable before approve/reject

---

### P0-3. Landlord tenant list is inaccurate
**Problem:** Landlords cannot reliably see who their tenants are. Data not derived from leases.

**Solution:** Derive tenant list from active leases. Tenant = user(s) on lease. Display per-lease tenant info.

**Acceptance criteria:**
- Landlord tenants page shows tenants from their leases only
- Each tenant linked to correct lease and listing
- No phantom or duplicate tenants

---

## P1 – Core MVP (Required for launch)

### P1-1. Tenant verification too narrow (student-only)
**Problem:** Verification assumes university + student ID. Platform serves all tenants, not just students.

**Solution:** Generalize tenant verification: gov ID (passport/ID card) + selfie. Remove university/student ID requirement. Update verification form and admin review.

**Acceptance criteria:**
- Tenant verification form accepts gov ID type, ID number, ID photo, selfie
- Admin sees generalized "Tenant Verification" (not "Student Verification")
- Existing student verifications still work during transition

---

### P1-2. Simplify co-apply to "share listing with roommate"
**Problem:** Co-apply/invite flow is complex and not aligned with real use: tenant finds listing, wants to share with matched roommate.

**Solution:** Replace co-apply with "Share with roommate": send listing link or notification to matched roommate. No joint application submission.

**Acceptance criteria:**
- Tenant can share a listing with a matched roommate (from matches)
- Roommate receives notification/link to view listing
- Roommate can apply independently if interested
- Remove or simplify co-application invite flow

---

### P1-3. Remove household from MVP
**Problem:** Household (expenses, tasks, rules) adds scope without validated demand. Not core to finding a room.

**Solution:** Remove household from tenant and landlord flows. Hide nav links. Keep backend/DB for future.

**Acceptance criteria:**
- No household links in tenant or landlord nav
- Lease flow does not create or link to household
- Existing household data preserved (no migration)

---

### P1-4. Landlord payments view lacking
**Problem:** Landlords cannot see payment status, history, or proof per lease.

**Solution:** Landlord payments page shows: payments per lease, status (Pending/Submitted/Verified/Rejected), amount, date. Link to lease and listing.

**Acceptance criteria:**
- Landlord sees all payments for their leases
- Filter by lease, status
- Clear status labels and dates

---

### P1-5. Landlord analytics (basic)
**Problem:** Landlords have no visibility into listing performance.

**Solution:** Basic analytics: views per listing, application count per listing. Simple dashboard.

**Acceptance criteria:**
- Landlord sees view count per listing
- Landlord sees application count per listing
- Data comes from existing tracking

---

### P1-6. Admin listing approval – ensure images visible
**Problem:** Admin may approve listings without seeing photos. Need to confirm images display.

**Solution:** Verify listing detail sheet in admin listings page shows all images. Fix if missing.

**Acceptance criteria:**
- Admin can view all listing images before approve/reject
- Images load correctly

---

## P2 – Important (Post-launch hardening)

### P2-1. Admin reports – basic triage
**Problem:** Reports exist but admin has no workflow to triage.

**Solution:** Admin reports page: view report, set status (Open/In Progress/Resolved), set priority.

**Acceptance criteria:**
- Admin can view report details
- Admin can update status and priority
- Reporter sees status update (optional notification)

---

### P2-2. Remove disputes from admin MVP
**Problem:** Disputes not in MVP but appear in admin nav.

**Solution:** Remove disputes from admin navigation. Keep backend for future.

**Acceptance criteria:**
- No disputes link in admin nav
- Dispute backend remains intact

---

### P2-3. Rename "Student" to "Tenant" in UI
**Problem:** UI says "Student" but persona is broader.

**Solution:** Replace user-facing "Student" with "Tenant" (verification tabs, role labels, etc.). Keep `UserRole.STUDENT` in code for now.

**Acceptance criteria:**
- No "Student" in user-facing text where we mean tenant
- Admin verification tab says "Tenants" not "Students"

---

## P3 – Deferred (Scale / automation)

### P3-1. Verification automation
**Problem:** Manual verification does not scale.

**Solution:** Integrate identity provider (Onfido, Jumio) for automated ID + liveness. Income verification via Plaid (future).

---

### P3-2. Application review automation
**Problem:** Manual application review does not scale.

**Solution:** Automated screening (income, background) with third-party APIs. Landlord gets pre-scored applicants.

---

### P3-3. Household (if validated)
**Problem:** Roommates may need to split expenses.

**Solution:** Reintroduce household with expenses, tasks, rules if user research validates need.

---

## Backlog Summary

| Priority | Count | Focus |
|----------|-------|-------|
| P0 | 3 | Admin visibility, landlord tenant accuracy |
| P1 | 6 | Verification generalization, co-apply simplification, household removal, payments, analytics |
| P2 | 3 | Reports triage, disputes removal, UI rename |
| P3 | 3 | Automation (verification, applications), household |
