# Test Matches Feature - Step by Step

## Date: Nov 22, 2025 - 10:33 PM

## Current Issue
- Backend logs show only `getMatches()` calls, NOT `findMatches()`
- This means the "Find Matches" button is NOT calling the correct API endpoint

## Testing Steps

### Step 1: Check Browser Console

1. Open browser DevTools (F12)
2. Go to Console tab
3. Click "Find Matches" button
4. Look for these logs:
   ```
   Finding matches for userId: <uuid>
   Find matches response: <response>
   ```

### Step 2: Check Network Tab

1. Open browser DevTools (F12)
2. Go to Network tab
3. Click "Find Matches" button
4. Look for this request:
   ```
   POST http://localhost:8080/api/matches/find?userId=<uuid>
   ```

**Expected:**
- Status: 200 OK
- Response: Array of matches

**If you see:**
- Status: 404 → Endpoint not found (check backend)
- Status: 500 → Server error (check backend logs)
- Status: 400 → Bad request (check userId format)
- No request at all → Frontend issue

### Step 3: Test API Directly with Postman/cURL

**Using cURL:**
```bash
# Replace <USER_ID> with actual UUID
# Replace <TOKEN> with actual JWT token from localStorage

curl -X POST "http://localhost:8080/api/matches/find?userId=d0c39663-702a-46da-96af-5230ee369c87" \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json"
```

**Using Postman:**
1. Method: POST
2. URL: `http://localhost:8080/api/matches/find?userId=d0c39663-702a-46da-96af-5230ee369c87`
3. Headers:
   - `Authorization: Bearer <your_token>`
   - `Content-Type: application/json`
4. Send

**Expected Response:**
```json
[
  {
    "id": "match-uuid",
    "matchedUserId": "other-user-uuid",
    "matchedUserFirstName": "John",
    "compatibilityScore": 85,
    "status": "PENDING",
    ...
  }
]
```

### Step 4: Check Backend Logs

**When you click "Find Matches", you MUST see:**
```
INFO ... MatchController : Finding matches for user: d0c39663-702a-46da-96af-5230ee369c87
INFO ... MatchingService : Finding matches for user: d0c39663-702a-46da-96af-5230ee369c87
INFO ... MatchingService : User preferences: ...
INFO ... MatchingService : Found X matches for user: d0c39663-702a-46da-96af-5230ee369c87
```

**If you DON'T see these logs:**
- The API endpoint is NOT being called
- Check frontend network tab
- Check if button is actually calling `handleFindMatches()`

### Step 5: Database Checks

**Check if preferences exist:**
```sql
SELECT 
    user_id,
    looking_for_roommate,
    min_budget,
    max_budget,
    preferred_locations,
    cleanliness_level,
    noise_tolerance,
    social_level,
    sleep_schedule,
    study_time_preference,
    preferred_gender,
    min_age,
    max_age
FROM roommate_preferences
WHERE user_id IN (
    'd0c39663-702a-46da-96af-5230ee369c87',
    '<other_student_uuid>'
);
```

**Expected:**
- Both rows exist
- `looking_for_roommate = true` for BOTH
- All required fields filled

**Check if matches already exist:**
```sql
SELECT * FROM matches
WHERE user1_id = 'd0c39663-702a-46da-96af-5230ee369c87'
   OR user2_id = 'd0c39663-702a-46da-96af-5230ee369c87';
```

**If matches exist:**
- Algorithm won't create duplicates
- Delete existing matches to test:
```sql
DELETE FROM matches
WHERE user1_id = 'd0c39663-702a-46da-96af-5230ee369c87'
   OR user2_id = 'd0c39663-702a-46da-96af-5230ee369c87';
```

## Debugging Checklist

### Frontend Issues:
- [ ] Browser console shows "Finding matches for userId: ..."
- [ ] Network tab shows POST request to `/api/matches/find`
- [ ] Request includes valid JWT token in Authorization header
- [ ] Request includes valid UUID in userId parameter
- [ ] No CORS errors in console
- [ ] No 401 Unauthorized errors

### Backend Issues:
- [ ] Backend logs show "Finding matches for user: ..."
- [ ] Backend logs show "User preferences: ..."
- [ ] Backend logs show "Found X matches for user: ..."
- [ ] No exceptions in backend logs
- [ ] Database has preferences for both users
- [ ] `looking_for_roommate = true` for both users

### Database Issues:
- [ ] Both users have preferences in `roommate_preferences` table
- [ ] `looking_for_roommate = true` for both
- [ ] All required fields are NOT NULL
- [ ] No existing matches between the two users
- [ ] User profiles exist with gender and date_of_birth

## Quick Fix Attempts

### Fix 1: Clear Existing Matches
```sql
-- Delete all matches for testing
DELETE FROM matches;
```

### Fix 2: Verify Preferences
```sql
-- Update looking_for_roommate if false
UPDATE roommate_preferences
SET looking_for_roommate = true
WHERE user_id IN (
    'd0c39663-702a-46da-96af-5230ee369c87',
    '<other_student_uuid>'
);
```

### Fix 3: Set Identical Preferences (for testing)
```sql
-- Make preferences identical for guaranteed match
UPDATE roommate_preferences
SET 
    min_budget = 50000,
    max_budget = 100000,
    preferred_locations = '["Bastos"]',
    cleanliness_level = 4,
    noise_tolerance = 3,
    social_level = 4,
    sleep_schedule = 'NIGHT_OWL',
    study_time_preference = 'EVENING',
    smoking = false,
    drinking = false,
    pets = false,
    guests = true,
    preferred_gender = 'ANY',
    min_age = 18,
    max_age = 100,
    deal_breakers = null,
    looking_for_roommate = true
WHERE user_id IN (
    'd0c39663-702a-46da-96af-5230ee369c87',
    '<other_student_uuid>'
);
```

### Fix 4: Test with cURL
```bash
# Get your access token from browser localStorage
# Then test the endpoint directly

TOKEN="your_jwt_token_here"
USER_ID="d0c39663-702a-46da-96af-5230ee369c87"

curl -X POST "http://localhost:8080/api/matches/find?userId=${USER_ID}" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -v
```

## Expected Behavior

### When "Find Matches" Works:

**Frontend:**
1. Button shows loading spinner
2. Console logs: "Finding matches for userId: ..."
3. Network shows POST to `/api/matches/find`
4. Toast notification: "Found X new matches for you!"
5. Matches appear in the list

**Backend:**
1. Logs: "Finding matches for user: ..."
2. Logs: "User preferences: ..."
3. Logs: "Found X matches for user: ..."
4. Database: New rows in `matches` table

**Database:**
```sql
-- New match created
SELECT * FROM matches
WHERE user1_id = 'd0c39663-702a-46da-96af-5230ee369c87'
   OR user2_id = 'd0c39663-702a-46da-96af-5230ee369c87';
```

## Next Steps

1. **Open browser DevTools** → Check Console and Network tabs
2. **Click "Find Matches"** → See what happens
3. **Check backend logs** → Should see "Finding matches for user: ..."
4. **If no backend logs** → Frontend not calling API (check Network tab)
5. **If backend logs but no matches** → Check database preferences
6. **Share findings** → Console logs, Network tab, Backend logs

## Common Solutions

### Solution 1: Frontend Not Calling API
- Check if userId is null
- Check if button is disabled
- Check browser console for errors
- Check Network tab for failed requests

### Solution 2: Backend Receiving Request but No Matches
- Check `looking_for_roommate = true`
- Check compatibility score (must be >= 60%)
- Check for existing matches (no duplicates)
- Check for deal-breakers

### Solution 3: Database Issues
- Preferences don't exist → Complete preferences wizard
- Fields are NULL → Update with valid values
- Matches already exist → Delete and retry

## Summary

**Most likely issue:** One of these:
1. ❌ Frontend not calling the API (check Network tab)
2. ❌ Backend receiving request but returning empty (check logs)
3. ❌ Compatibility score < 60% (check preferences)
4. ❌ Matches already exist (check database)

**Quick test:**
1. Open browser DevTools
2. Click "Find Matches"
3. Check Console for logs
4. Check Network for POST request
5. Share what you see!
