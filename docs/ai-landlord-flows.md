# Landlord Flows Runbook (Production)

This runbook documents landlord-facing flows for operations, QA, and AI grounding.

## Flow L1: Register and profile
- Register as landlord.
- Complete profile basics.
- Access landlord dashboard.

## Flow L2: Create listing and submit
### Steps
1. Open new listing page.
2. Fill title, location, rent, details.
3. Upload listing photos.
4. Submit for admin review.

### Expected result
- Listing status becomes `PENDING`.

## Flow L3: Listing approval lifecycle
- Admin approves -> listing `ACTIVE`.
- Admin rejects -> listing `REJECTED` with reason.
- Landlord edits and resubmits.

## Flow L4: Application management
### Steps
1. Open `/landlord/applications`.
2. Review candidate details.
3. Accept or reject.

### Expected result
- Accepted application moves to lease flow.

## Flow L5: Tenant management (lease-derived)
- Tenant list must come from lease records.
- No manual/phantom tenant records.

## Flow L6: Landlord verification
- Submit identity docs on `/landlord/verification`.
- Track status and resubmit when rejected.

## Flow L7: Payments visibility
- View payments per lease.
- Track statuses and history.

## Flow L8: Basic analytics
- View counts per listing.
- Application counts per listing.

## Landlord support macros
- "Listings only appear to tenants after admin approval."
- "Use lease-based tenant records for accurate tenant management."
