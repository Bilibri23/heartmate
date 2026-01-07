# 🔒 Verification Enforcement System

## The Problem You Identified

**Current Issue:**
- ✅ Students can access everything without verification
- ✅ Verification is optional (inside dashboard)
- ✅ No enforcement of verification status

**This is a critical security flaw for a housing platform!**

---

## The Solution: Verification Gates

### What I Implemented

| Component | Purpose |
|-----------|---------|
| `@RequiresVerification` | Annotation to mark protected endpoints |
| `VerificationAspect` | Automatically checks verification before method execution |
| `VerificationRequiredException` | Custom exception when verification is missing |
| `GlobalExceptionHandler` | Returns clear error messages to frontend |

---

## How It Works

### 1. Annotate Protected Endpoints

```java
// In ListingController.java
@PostMapping("/{listingId}/apply")
@RequiresVerification(role = "STUDENT", verificationType = "STUDENT_ID")
public ResponseEntity<ApplicationResponse> applyToListing(@PathVariable UUID listingId) {
    // Only verified students can reach this code
}

// In ListingController.java
@PostMapping("/")
@RequiresVerification(role = "LANDLORD", verificationType = "IDENTITY")
public ResponseEntity<ListingResponse> createListing(@RequestBody ListingRequest request) {
    // Only identity-verified landlords can post listings
}
```

### 2. What Happens When Unverified User Tries to Access

```json
// HTTP 403 Forbidden
{
  "timestamp": "2025-12-10T14:00:00",
  "status": 403,
  "error": "Verification Required",
  "message": "STUDENT must complete Student ID verification to access this resource"
}
```

### 3. Frontend Can Show Verification Prompt

```javascript
// In your React app
if (error.status === 403 && error.error === "Verification Required") {
  // Show verification modal
  showVerificationPrompt(error.message);
}
```

---

## Verification Levels

### For STUDENTS

| Verification Status | Can Do | Cannot Do |
|-------------------|--------|-----------|
| **Not Submitted** | Browse listings (no contact info), View profile | Apply to listings, Message landlords, See phone numbers |
| **Pending** | Same as above + See submission status | Apply to listings, Message landlords |
| **Verified** | ✅ Full access | - |
| **Rejected** | Same as Not Submitted + See rejection reason | Apply to listings |

### For LANDLORDS

| Verification Status | Can Do | Cannot Do |
|-------------------|--------|-----------|
| **Not Submitted** | Browse listings, View profile | Post listings, Message students |
| **Identity Pending** | Same + See submission status | Post listings |
| **Identity Verified** | ✅ Post listings, Message students | - |
| **Business Verified** | ✅ Get "Verified Business" badge | - |
| **Fully Verified** | ✅ Get "Trusted Landlord" badge, Priority in search | - |

---

## Next Steps: Apply to Your Controllers

### Step 1: Protect Listing Applications

```java
// In ApplicationController.java or ListingController.java
@PostMapping("/{listingId}/apply")
@RequiresVerification(role = "STUDENT", verificationType = "STUDENT_ID")
public ResponseEntity<ApplicationResponse> applyToListing(
        @PathVariable UUID listingId,
        @RequestBody ApplicationRequest request) {
    // Only verified students can apply
}
```

### Step 2: Protect Listing Creation

```java
// In ListingController.java
@PostMapping("/")
@RequiresVerification(role = "LANDLORD", verificationType = "IDENTITY")
public ResponseEntity<ListingResponse> createListing(@RequestBody ListingRequest request) {
    // Only identity-verified landlords can post
}
```

### Step 3: Protect Messaging

```java
// In MessageController.java (if you have one)
@PostMapping("/")
@RequiresVerification(role = "STUDENT", verificationType = "STUDENT_ID")
public ResponseEntity<MessageResponse> sendMessage(@RequestBody MessageRequest request) {
    // Only verified students can message landlords
}
```

### Step 4: Protect Contact Info Access

```java
// In ListingController.java
@GetMapping("/{listingId}/contact")
@RequiresVerification(role = "STUDENT", verificationType = "STUDENT_ID")
public ResponseEntity<ContactInfoResponse> getContactInfo(@PathVariable UUID listingId) {
    // Only verified students can see landlord contact details
}
```

---

## Testing the System

### Test 1: Unverified Student Tries to Apply

```bash
# Login as unverified student
POST /api/auth/login
{
  "email": "student@example.com",
  "password": "password"
}

# Try to apply to a listing (should fail)
POST /api/listings/123/apply
Authorization: Bearer <token>

# Expected Response: 403 Forbidden
{
  "error": "Verification Required",
  "message": "STUDENT must complete Student ID verification to access this resource"
}
```

### Test 2: Verified Student Can Apply

```bash
# Submit verification
POST /api/verifications
{
  "university": "University of Buea",
  "studentId": "UB12345",
  ...
}

# Admin approves
POST /api/admin/verifications/{id}/approve
{
  "status": "VERIFIED"
}

# Now student can apply
POST /api/listings/123/apply
# Expected: 200 OK
```

---

## Frontend Integration

### 1. Detect Verification Required Error

```javascript
// In your API service
const handleApiError = (error) => {
  if (error.response?.status === 403 && 
      error.response?.data?.error === "Verification Required") {
    // Show verification modal
    store.dispatch(showVerificationModal({
      message: error.response.data.message,
      userRole: getCurrentUserRole()
    }));
  }
};
```

### 2. Show Verification Banner

```jsx
// In StudentDashboard.jsx
{!user.isVerified && (
  <Alert severity="warning">
    <AlertTitle>Verification Required</AlertTitle>
    You must verify your student ID to apply to listings.
    <Button onClick={() => navigate('/verify')}>
      Verify Now
    </Button>
  </Alert>
)}
```

### 3. Disable Actions for Unverified Users

```jsx
// In ListingCard.jsx
<Button
  disabled={!user.isVerified}
  onClick={handleApply}
>
  {user.isVerified ? 'Apply Now' : 'Verify to Apply'}
</Button>
```

---

## Configuration

### Enable/Disable Verification Enforcement

```properties
# In application.properties
# Set to false to disable verification checks (for testing)
verification.enforcement.enabled=true
```

### Customize Verification Messages

```java
// In VerificationAspect.java
throw new VerificationRequiredException(
    "STUDENT", 
    "Student ID verification. Please upload your student ID card to continue."
);
```

---

## Benefits

| Benefit | Impact |
|---------|--------|
| **Prevents Scams** | Only verified users can interact |
| **Builds Trust** | Users know they're dealing with verified people |
| **Reduces Spam** | Bots can't apply without verification |
| **Compliance** | Audit trail of who accessed what |
| **Better UX** | Clear error messages guide users |

---

## Rollout Plan

### Phase 1: Soft Launch (Current)
- ✅ System is implemented
- ⏳ Apply annotations to critical endpoints
- ⏳ Test with real users
- ⏳ Monitor error rates

### Phase 2: Frontend Integration
- Add verification banners
- Show verification status in profile
- Add verification modals
- Disable buttons for unverified users

### Phase 3: Full Enforcement
- Apply to all protected endpoints
- Remove any bypasses
- Monitor and adjust

---

## Files Created

```
backend/src/main/java/org/rooms/roombuddy/security/
├── RequiresVerification.java       # Annotation
├── VerificationAspect.java         # Enforcement logic
└── VerificationRequiredException.java  # Custom exception

backend/src/main/java/org/rooms/roombuddy/exception/
└── GlobalExceptionHandler.java     # Updated with verification handler
```

---

## Next Actions for You

1. **Restart backend** to load new dependencies:
   ```bash
   # Stop current run (Ctrl+C)
   mvn clean compile
   mvn spring-boot:run
   ```

2. **Apply annotations** to your controllers (I'll help with this next)

3. **Test the system** with Postman/Swagger

4. **Update frontend** to handle verification errors

Would you like me to help apply the `@RequiresVerification` annotation to your existing controllers?
