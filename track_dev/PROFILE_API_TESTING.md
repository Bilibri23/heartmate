# Profile Management API - Testing Guide

## Overview
This guide provides instructions for testing the Profile Management API endpoints using Postman or any REST client.

## Prerequisites
1. PostgreSQL database running on `localhost:5432`
2. Database `roomconnect_db` created
3. Spring Boot application running on `http://localhost:8080`
4. At least one user exists in the `users` table (from V1 migration)

## Sample User IDs from Migration
The V1 migration creates two test users:
- John Doe: Check database for UUID
- Jane Smith: Check database for UUID

To get user IDs, run this SQL query:
```sql
SELECT id, email, first_name, last_name FROM users;
```

## API Endpoints

### 1. Create Profile
**POST** `/api/profiles?userId={userId}`

**Request Body:**
```json
{
  "bio": "Computer Science student at UY1 looking for a quiet roommate",
  "profilePhotoUrl": "https://cloudinary.com/sample/profile.jpg",
  "coverPhotoUrl": "https://cloudinary.com/sample/cover.jpg",
  "languages": ["English", "French", "Pidgin"],
  "interests": ["Coding", "Reading", "Football"],
  "hobbies": ["Gaming", "Music", "Cooking"],
  "instagramHandle": "@johndoe",
  "facebookProfile": "https://facebook.com/johndoe",
  "whatsappNumber": "+237677123456",
  "emergencyContactName": "Mary Doe",
  "emergencyContactPhone": "+237677999888",
  "emergencyContactRelationship": "Mother",
  "visibility": "PUBLIC"
}
```

**Success Response (201 Created):**
```json
{
  "id": "uuid-here",
  "userId": "user-uuid-here",
  "bio": "Computer Science student at UY1 looking for a quiet roommate",
  "profilePhotoUrl": "https://cloudinary.com/sample/profile.jpg",
  "coverPhotoUrl": "https://cloudinary.com/sample/cover.jpg",
  "languages": ["English", "French", "Pidgin"],
  "interests": ["Coding", "Reading", "Football"],
  "hobbies": ["Gaming", "Music", "Cooking"],
  "instagramHandle": "@johndoe",
  "facebookProfile": "https://facebook.com/johndoe",
  "whatsappNumber": "+237677123456",
  "emergencyContactName": "Mary Doe",
  "emergencyContactPhone": "+237677999888",
  "emergencyContactRelationship": "Mother",
  "visibility": "PUBLIC",
  "createdAt": "2025-11-06T07:00:00",
  "updatedAt": "2025-11-06T07:00:00"
}
```

**Error Responses:**
- `404 Not Found`: User not found
- `400 Bad Request`: Profile already exists or validation error

---

### 2. Get Profile by User ID
**GET** `/api/profiles/{userId}`

**Success Response (200 OK):**
```json
{
  "id": "uuid-here",
  "userId": "user-uuid-here",
  "bio": "Computer Science student at UY1 looking for a quiet roommate",
  "..."
}
```

**Error Response:**
- `404 Not Found`: Profile not found for user

---

### 3. Update Profile
**PUT** `/api/profiles/{userId}`

**Request Body (all fields optional):**
```json
{
  "bio": "Updated bio - Engineering student seeking compatible roommate",
  "interests": ["Coding", "Reading", "Football", "Photography"],
  "visibility": "VERIFIED_ONLY"
}
```

**Success Response (200 OK):**
```json
{
  "id": "uuid-here",
  "userId": "user-uuid-here",
  "bio": "Updated bio - Engineering student seeking compatible roommate",
  "interests": ["Coding", "Reading", "Football", "Photography"],
  "visibility": "VERIFIED_ONLY",
  ...
}
```

**Error Response:**
- `404 Not Found`: Profile not found for user

---

### 4. Delete Profile
**DELETE** `/api/profiles/{userId}`

**Success Response (204 No Content):**
No body returned

**Error Response:**
- `404 Not Found`: Profile not found for user

---

## Validation Rules

### Phone Numbers
- Format: `+237XXXXXXXXX` (Cameroon format)
- Examples: `+237677123456`, `+237699887766`

### Instagram Handle
- Pattern: `@username` or `username`
- Max 30 characters
- Alphanumeric, dots, and underscores only

### Bio
- Max 1000 characters

### Visibility Options
- `PUBLIC`: Everyone can see
- `VERIFIED_ONLY`: Only verified students
- `PRIVATE`: Only connections

### Languages
- Supported: English, French, Pidgin

---

## Testing Workflow

### Step 1: Get User ID
```sql
SELECT id FROM users WHERE email = 'john.doe@example.com';
```

### Step 2: Create Profile
```bash
POST http://localhost:8080/api/profiles?userId=<user-id-from-step-1>
Content-Type: application/json

{
  "bio": "Test bio",
  "languages": ["English", "French"],
  "interests": ["Coding"],
  "visibility": "PUBLIC"
}
```

### Step 3: Get Profile
```bash
GET http://localhost:8080/api/profiles/<user-id>
```

### Step 4: Update Profile
```bash
PUT http://localhost:8080/api/profiles/<user-id>
Content-Type: application/json

{
  "bio": "Updated bio",
  "hobbies": ["Gaming", "Music"]
}
```

### Step 5: Delete Profile
```bash
DELETE http://localhost:8080/api/profiles/<user-id>
```

---

## Postman Collection

### Environment Variables
Create a Postman environment with:
- `baseUrl`: `http://localhost:8080`
- `userId`: `<paste-user-id-here>`

### Sample Requests

**1. Create Profile**
```
POST {{baseUrl}}/api/profiles?userId={{userId}}
```

**2. Get Profile**
```
GET {{baseUrl}}/api/profiles/{{userId}}
```

**3. Update Profile**
```
PUT {{baseUrl}}/api/profiles/{{userId}}
```

**4. Delete Profile**
```
DELETE {{baseUrl}}/api/profiles/{{userId}}
```

---

## Common Issues & Solutions

### Issue: "User not found"
**Solution:** Verify user exists in  a database:
```sql
SELECT * FROM users WHERE id = '<your-user-id>';
```

### Issue: "Profile already exists"
**Solution:** Delete existing profile first or use UPDATE endpoint

### Issue: "Invalid phone number format"
**Solution:** Ensure phone numbers follow `+237XXXXXXXXX` format

### Issue: "Invalid visibility value"
**Solution:** Use only: PUBLIC, VERIFIED_ONLY, or PRIVATE

---

## Database Verification

### Check Profile Created
```sql
SELECT * FROM profiles WHERE user_id = '<user-id>';
```

### Check User's profile_completed Flag
```sql
SELECT id, email, profile_completed FROM users WHERE id = '<user-id>';
```

### View All Profiles
```sql
SELECT p.id, u.email, p.bio, p.visibility, p.created_at 
FROM profiles p 
JOIN users u ON p.user_id = u.id;
```

---

## Next Steps
After testing Profile Management:
1. Test with Swagger UI: `http://localhost:8080/swagger-ui.html`
2. Implement Student Verification
3. Implement Roommate Preferences
4. Integrate OAuth2 authentication
5. Add file upload for profile photos (Cloudinary)

---

## Notes
- Currently, authentication is disabled for testing
- In production, OAuth2 will be required
- Profile photos will be uploaded to Cloudinary
- Arrays (languages, interests, hobbies) are stored as PostgreSQL TEXT[] arrays

