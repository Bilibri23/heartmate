
# RoomBay FAQ (Tenant, Landlord, Admin)

## Tenant

### How do I verify my account?
Go to `/verification`, select ID type, enter ID number, upload ID photo and selfie, then submit.

### Why is verification pending?
Verification is manually reviewed by admin. Use refresh on verification page.

### How do I share a listing with a roommate?
Open listing details, tap share, choose a matched roommate, and send.

### Can my roommate apply with me automatically?
No. Co-apply was simplified. Shared roommate applies independently.

### How do I track applications?
Open `/applications` and check status per listing.

### Why was payment rejected?
Payment proof may be unclear or mismatched. Check rejection reason and resubmit.

## Landlord

### Why is my listing not visible?
Listing must be approved by admin to become `ACTIVE`.

### How do I review applications?
Use `/landlord/applications` to review and accept/reject applicants.

### How is tenant management determined?
Tenants should be derived from active leases.

### What analytics are available?
Basic listing views and application counts.

## Admin

### What must I check before approving tenant verification?
Readable ID, plausible ID number, selfie match, no tampering.

### What must I check before approving listings?
Photo quality/completeness, coherent details, and policy compliance.

### How do I fix missing search results?
Run Search Reindex from Admin Settings.

## AI assistant

### Which provider is active?
Based on backend env `AI_PROVIDER` (`openai`, `ollama`, or `auto`).

### Why does AI fail with model not found?
Configured Ollama model is not pulled yet.

### How do I refresh AI knowledge?
Update docs and run Admin "Ingest Docs".
