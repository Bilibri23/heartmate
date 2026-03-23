# Tenant Flows Runbook (Production)

This runbook documents tenant-facing flows in RoomBuddy for support, QA, and AI assistant grounding.

## Persona definition
- Tenant = any room seeker (not limited to students).
- In backend role enum, tenant still maps to `STUDENT` for compatibility.

## Global assumptions
- User is authenticated unless flow says otherwise.
- Listings must be `ACTIVE` to apply/share.
- Verification status can be `NONE`, `PENDING`, `VERIFIED`, `REJECTED`.

## Flow T1: Register and login
### Trigger
- New tenant opens `/register`.

### Preconditions
- Email not already used.
- Valid phone and password format.

### Steps
1. Select `Tenant` role on register page.
2. Submit first name, last name, email, country code, phone, gender, password.
3. Frontend maps role to backend-compatible `STUDENT` during submit.
4. Login with created credentials.

### Expected result
- Account created and session/token available.

### Failure modes and fixes
- `Email already exists`: prompt login or password reset.
- Validation errors: highlight field-level errors.

## Flow T2: Search and view listings
### Steps
1. Open `/search`.
2. Apply filters (city, budget, property type).
3. Open listing detail `/listings/{id}`.

### Expected result
- Results load and listing details display correctly.

### Failure modes and fixes
- Empty results: broaden filters or ask admin to reindex search.

## Flow T3: Share listing with matched roommate
### Preconditions
- Listing is `ACTIVE`.
- Tenant has mutual roommate matches.

### Steps
1. On listing page, open share sheet.
2. Choose matched roommate.
3. Click `Share`.

### Expected result
- Roommate receives in-app notification and listing link.

### Failure modes and fixes
- Not mutual match: backend blocks share.
- Notification missing: verify notifications service/websocket.

## Flow T4: Apply to listing
### Preconditions
- Listing is active and tenant has not already applied.

### Steps
1. Open apply modal.
2. Submit message + move-in details.

### Expected result
- Application appears in `/applications` as `PENDING`.

### Failure modes and fixes
- Duplicate apply attempts are blocked by backend.

## Flow T5: Tenant verification (gov ID)
### Steps
1. Open `/verification`.
2. Select ID type and enter ID number.
3. Upload ID photo and selfie.
4. Submit.

### Expected result
- Verification status becomes `PENDING`.

### Failure modes and fixes
- Blurry docs: rejection with reason; tenant resubmits.

## Flow T6: Lease signing
1. Tenant receives lease notification after application acceptance.
2. Opens `/leases` and signs lease.

## Flow T7: Payments
1. Tenant submits payment proof via `/payments`.
2. Status updates after admin verification.

### Statuses
- `SUBMITTED` -> `VERIFIED` or `REJECTED`

## Flow T8: Notifications and messaging
- Notification history in `/notifications`.
- Real-time via websocket with polling fallback.

## Tenant support macros
- "Complete verification by uploading ID + selfie in Verification page."
- "If payment is rejected, resubmit with clearer proof and visible transaction details."
