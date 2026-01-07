# RoomConnect API Endpoints Summary

**Last Updated:** $(date)
**Base URL:** `http://localhost:8080`

All endpoints (except `/api/auth/**`) require JWT authentication.
Include the token in the Authorization header: `Bearer {token}`

---

## 🔐 Authentication Endpoints

### Register
```http
POST /api/auth/register
Content-Type: application/json

{
  "email": "user@example.com",
  "phone": "+237677123456",
  "password": "password123",
  "firstName": "John",
  "lastName": "Doe",
  "gender": "MALE",
  "dateOfBirth": "2000-01-15"
}
```

**Response:**
```json
{
  "userId": "uuid",
  "accessToken": "jwt-token",
  "refreshToken": "refresh-token",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```

### Login
```http
POST /api/auth/login
Content-Type: application/json

{
  "emailOrPhone": "user@example.com",
  "password": "password123"
}
```

**Response:** Same as register

### Refresh Token
```http
POST /api/auth/refresh
Authorization: Bearer {refresh-token}
```

### Forgot Password
```http
POST /api/auth/forgot-password
Content-Type: application/json

{
  "email": "user@example.com"
}
```

### Reset Password
```http
POST /api/auth/reset-password
Content-Type: application/json

{
  "token": "reset-token",
  "newPassword": "newpassword123"
}
```

---

## 👤 Profile Endpoints

### Create Profile
```http
POST /api/profiles?userId={userId}
Authorization: Bearer {token}
Content-Type: application/json

{
  "bio": "I'm a computer science student...",
  "profilePhotoUrl": "https://cloudinary.com/...",
  "languages": ["English", "French"],
  "whatsappNumber": "+237677123456",
  "emergencyContactName": "Jane Doe",
  "emergencyContactPhone": "+237677654321",
  "emergencyContactRelationship": "Sister",
  "visibility": "PUBLIC"
}
```

### Get Profile
```http
GET /api/profiles/{userId}
Authorization: Bearer {token}
```

### Update Profile
```http
PUT /api/profiles/{userId}
Authorization: Bearer {token}
Content-Type: application/json

{
  "bio": "Updated bio...",
  "visibility": "VERIFIED_ONLY"
}
```

### Delete Profile
```http
DELETE /api/profiles/{userId}
Authorization: Bearer {token}
```

---

## 🎓 Student Verification Endpoints

### Submit Verification
```http
POST /api/verifications?userId={userId}
Authorization: Bearer {token}
Content-Type: application/json

{
  "university": "University of Yaounde I",
  "studentId": "ST123456",
  "faculty": "Science",
  "department": "Computer Science",
  "yearOfStudy": 3,
  "studentIdPhotoUrl": "https://cloudinary.com/..."
}
```

### Get Verification
```http
GET /api/verifications/{userId}
Authorization: Bearer {token}
```

### Update Verification
```http
PUT /api/verifications/{userId}
Authorization: Bearer {token}
Content-Type: application/json

{
  "university": "University of Yaounde I",
  "studentId": "ST123456"
}
```

### Delete Verification
```http
DELETE /api/verifications/{userId}
Authorization: Bearer {token}
```

---

## 📋 Roommate Preferences Endpoints

### Create Preferences
```http
POST /api/preferences?userId={userId}
Authorization: Bearer {token}
Content-Type: application/json

{
  "minBudget": 50000,
  "maxBudget": 100000,
  "preferredLocations": ["Mvan", "Etoa-Meki"],
  "maxDistanceFromCampus": 5.0,
  "cleanlinessLevel": 4,
  "noiseTolerance": 3,
  "socialLevel": 3,
  "sleepSchedule": "NIGHT_OWL",
  "studyTimePreference": "EVENING",
  "smoking": false,
  "drinking": false,
  "pets": false,
  "guests": true,
  "cooking": true,
  "dealBreakers": "No smokers, No parties",
  "preferredGender": "ANY",
  "minAge": 20,
  "maxAge": 25,
  "sameUniversity": true,
  "sameFaculty": false,
  "lookingForRoommate": true
}
```

### Get Preferences
```http
GET /api/preferences/{userId}
Authorization: Bearer {token}
```

### Update Preferences
```http
PUT /api/preferences/{userId}
Authorization: Bearer {token}
Content-Type: application/json

{
  "minBudget": 60000,
  "maxBudget": 120000,
  "lookingForRoommate": true
}
```

### Delete Preferences
```http
DELETE /api/preferences/{userId}
Authorization: Bearer {token}
```

---

## 🎯 Matching Endpoints

### Find Matches
```http
POST /api/matches/find?userId={userId}
Authorization: Bearer {token}
```

**Response:**
```json
[
  {
    "id": "match-uuid",
    "userId": "user-uuid",
    "matchedUserId": "matched-user-uuid",
    "matchedUserFirstName": "Jane",
    "matchedUserLastName": "Smith",
    "matchedUserEmail": "jane@example.com",
    "matchedUserProfilePhotoUrl": "https://cloudinary.com/...",
    "matchedUserWhatsapp": "+237677123456", // Only visible on mutual match
    "compatibilityScore": 85,
    "budgetScore": 90,
    "lifestyleScore": 80,
    "scheduleScore": 85,
    "locationScore": 90,
    "habitsScore": 80,
    "status": "PENDING",
    "userAction": null,
    "matchedUserAction": null,
    "isMutualMatch": false,
    "createdAt": "2024-01-15T10:00:00",
    "updatedAt": "2024-01-15T10:00:00"
  }
]
```

**Note:** `matchedUserWhatsapp` is only populated when `isMutualMatch` is `true`

### Get Matches
```http
GET /api/matches/{userId}?status=PENDING
Authorization: Bearer {token}
```

### Get Pending Matches
```http
GET /api/matches/{userId}/pending
Authorization: Bearer {token}
```

### Accept/Reject Match
```http
POST /api/matches/{matchId}/action?userId={userId}
Authorization: Bearer {token}
Content-Type: application/json

{
  "action": "ACCEPT"
}
```

## 🏠 Property Listings Endpoints

### Create Listing
```http
POST /api/listings?landlordId={landlordId}
Authorization: Bearer {token}
Content-Type: application/json

{
  "title": "Beautiful 2-bedroom apartment",
  "description": "Spacious apartment near university",
  "propertyType": "APARTMENT",
  "rentAmount": 80000,
  "deposit": 160000,
  "agencyFees": 80000,
  "region": "Centre",
  "city": "Yaounde",
  "neighborhood": "Mvan",
  "address": "123 Main Street",
  "latitude": 3.8480,
  "longitude": 11.5021,
  "distanceToUniversity": 2.5,
  "bedrooms": 2,
  "bathrooms": 1,
  "squareMeters": 60,
  "floor": 2,
  "amenities": ["WiFi", "Water", "Electricity", "Parking"],
  "availableFrom": "2024-02-01",
  "availableTo": "2024-12-31",
  "landlordWhatsapp": "+237677123456",
  "status": "PENDING"
}
```

### Get Listing
```http
GET /api/listings/{listingId}?userId={userId}
Authorization: Bearer {token}
```

### Update Listing
```http
PUT /api/listings/{listingId}?landlordId={landlordId}
Authorization: Bearer {token}
Content-Type: application/json

{
  "title": "Updated title",
  "rentAmount": 85000
}
```

### Delete Listing
```http
DELETE /api/listings/{listingId}?landlordId={landlordId}
Authorization: Bearer {token}
```

### Search Listings
```http
GET /api/listings?city=Yaounde&neighborhood=Mvan&propertyType=APARTMENT&minPrice=50000&maxPrice=100000&amenities=WiFi&amenities=Water&userId={userId}
Authorization: Bearer {token}
```

### Get Active Listings
```http
GET /api/listings/active?userId={userId}
Authorization: Bearer {token}
```

### Get Featured Listings
```http
GET /api/listings/featured?userId={userId}
Authorization: Bearer {token}
```

### Get Landlord Listings
```http
GET /api/listings/landlord/{landlordId}
Authorization: Bearer {token}
```

### Add Photo to Listing
```http
POST /api/listings/{listingId}/photos?landlordId={landlordId}&isPrimary=true
Authorization: Bearer {token}
Content-Type: multipart/form-data

file: (binary)
```

### Remove Photo from Listing
```http
DELETE /api/listings/photos/{photoId}?landlordId={landlordId}
Authorization: Bearer {token}
```

### Toggle Favorite
```http
POST /api/listings/{listingId}/favorite?userId={userId}
Authorization: Bearer {token}
```

### Get Favorites
```http
GET /api/listings/favorites/{userId}
Authorization: Bearer {token}
```

---

## 📤 File Upload Endpoints

### Upload Profile Photo
```http
POST /api/upload/profile-photo
Authorization: Bearer {token}
Content-Type: multipart/form-data

file: (binary)
```

### Upload Student ID Photo
```http
POST /api/upload/student-id
Authorization: Bearer {token}
Content-Type: multipart/form-data

file: (binary)
```

### Delete Image
```http
DELETE /api/upload/image?url={image-url}
Authorization: Bearer {token}
```

---

## 👨‍💼 Admin Endpoints (Requires ADMIN role)

### Get Pending Verifications
```http
GET /api/admin/verifications/pending
Authorization: Bearer {admin-token}
```

### Get All Verifications
```http
GET /api/admin/verifications?status=PENDING
Authorization: Bearer {admin-token}
```

### Approve/Reject Verification
```http
POST /api/admin/verifications/{verificationId}/approve
Authorization: Bearer {admin-token}
Content-Type: application/json

{
  "status": "VERIFIED",
  "rejectionReason": null
}
```

or

```json
{
  "status": "REJECTED",
  "rejectionReason": "Student ID photo is not clear"
}
```

### Get Pending Listings
```http
GET /api/admin/listings/pending
Authorization: Bearer {admin-token}
```

### Approve/Reject Listing
```http
POST /api/admin/listings/{listingId}/approve
Authorization: Bearer {admin-token}
Content-Type: application/json

{
  "status": "ACTIVE",
  "rejectionReason": null,
  "featured": true
}
```

or

```json
{
  "status": "REJECTED",
  "rejectionReason": "Listing does not meet quality standards",
  "featured": false
}
```

### Get Platform Statistics
```http
GET /api/admin/statistics
Authorization: Bearer {admin-token}
```

**Response:**
```json
{
  "totalUsers": 150,
  "totalStudents": 140,
  "totalLandlords": 10,
  "verifiedStudents": 120,
  "activeListings": 50,
  "pendingListings": 5,
  "totalMatches": 200,
  "acceptedMatches": 30,
  "pendingVerifications": 3,
  "pendingListingApprovals": 5
}
```

---

## 🔄 Typical User Flows

### Flow A: Solo House Hunting
1. Register & Login → Get JWT token
2. Create profile → Upload photo, add WhatsApp number
3. Set preferences → Set `lookingForRoommate: false`
4. Browse listings → Search and filter
5. View listing details → See landlord WhatsApp
6. Contact landlord → Generate WhatsApp chat link
7. Favorite listings → Save for later

### Flow B: Find Roommate First
1. Register & Login → Get JWT token
2. Create profile → Upload photo, add WhatsApp number
3. Submit verification → Upload student ID photo
4. Set preferences → Set `lookingForRoommate: true`
5. Find matches → Call `POST /api/matches/find`
6. View match details → Check compatibility scores
7. Accept/Reject matches → Update match status
8. Mutual match → WhatsApp numbers exposed
9. Coordinate on WhatsApp → External communication

### Flow C: Landlord Posts Property
1. Register as LANDLORD → Get JWT token
2. Create listing → Add details, photos
3. Submit for approval → Status becomes PENDING
4. Admin approves → Status becomes ACTIVE
5. Students see listing → Search and browse
6. Students contact → Via WhatsApp
7. Manage inquiries → On WhatsApp
8. Update listing → Mark as RENTED when rented

---

## 📊 Response Formats

### Success Response
```json
{
  "success": true,
  "message": "Operation successful",
  "data": {...},
  "timestamp": "2024-01-15T10:00:00"
}
```

### Error Response
```json
{
  "timestamp": "2024-01-15T10:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/profiles",
  "errors": {
    "field": ["Error message"]
  }
}
```

---

## 🔒 Authentication

### Getting JWT Token
1. Register or login to get `accessToken` and `refreshToken`
2. Use `accessToken` in Authorization header for all requests
3. Token expires in 1 hour
4. Use `refreshToken` to get new `accessToken`

### Token Refresh
```http
POST /api/auth/refresh
Authorization: Bearer {refresh-token}
```

---

## 📝 Notes

1. **All endpoints require authentication** except `/api/auth/**`
2. **Admin endpoints require ADMIN role** in JWT token
3. **File uploads** are limited to 10MB
4. **Image formats** supported: JPEG, JPG, PNG, WEBP
5. **Phone numbers** must be in format: `+237XXXXXXXXX`
6. **Compatibility scores** range from 0-100
7. **Minimum compatibility threshold** is 60% for matches
8. **Top 20 matches** are returned per request

---

## 🚀 Frontend Integration Checklist

- [ ] Set up API client with base URL
- [ ] Implement JWT token storage (localStorage/sessionStorage)
- [ ] Add token refresh logic
- [ ] Create authentication service
- [ ] Create profile service
- [ ] Create preferences service
- [ ] Create matching service
- [ ] Create file upload service
- [ ] Handle error responses
- [ ] Add loading states
- [ ] Implement token expiration handling

---

**Ready for Frontend Integration!** 🎉

All core APIs are implemented and tested. You can now start building the frontend.

