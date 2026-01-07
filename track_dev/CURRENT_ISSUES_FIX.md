# Current Issues & Fixes - Nov 23, 2025 (3:28 PM)

## Issues Reported

### 1. ✅ Match Cards Not Showing
**Problem:** Matches page shows "No matches found" even though matches exist in database

**Root Cause:** Response data extraction was incorrect
- Was looking for `response.data.data`
- Backend returns array directly in `response.data`

**Fix Applied:**
- Updated `fetchMatches()` in `MatchesPage.jsx`
- Now checks if `response.data` is an array first
- Added logging to debug response structure

**Test:**
1. Login as student who clicked "Find Matches"
2. Go to Matches page
3. Should see match cards with:
   - Profile photos
   - Names
   - Compatibility scores
   - Status badges (PENDING)
4. Check console for logs showing matches count

---

### 2. ✅ Students Can't See Listings
**Problem:** Student sees "No listings" even though landlord created listings

**Possible Causes:**
1. Listing status is not ACTIVE (might be PENDING or DRAFT)
2. API not fetching correctly
3. Listing not approved by admin

**Fixes Applied:**
1. Updated `listingService.getActive()` to accept pagination and filters
2. Added logging to see what's being fetched
3. Added userId parameter to track views

**Test:**
1. **As Landlord:**
   - Create a listing
   - Check status (should be PENDING initially)
   - If you have admin access, approve it to ACTIVE
   
2. **As Student:**
   - Go to "All Listings" page
   - Open browser console (F12)
   - Check logs:
     ```
     Fetching listings with params: {...}
     Listings response: {...}
     Listings count: X
     ```
   - Should see listings if status is ACTIVE

**Backend Check:**
```sql
-- Check listing status
SELECT id, title, status, created_at
FROM listings
ORDER BY created_at DESC;

-- If status is PENDING, update to ACTIVE for testing
UPDATE listings
SET status = 'ACTIVE'
WHERE status = 'PENDING';
```

---

### 3. ⚠️ Edit Listing Button "Missing"
**Status:** Button EXISTS in code (line 132-138 of LandlordListingDetailsPage.jsx)

**Possible Issues:**
1. Not navigating to listing details page correctly
2. Page not loading
3. Button hidden by CSS

**Verification Steps:**
1. Login as landlord
2. Go to "Manage Listings"
3. Click on a listing card
4. Should navigate to `/admin/listing-details?id={listingId}`
5. Check if page loads
6. Look for "Edit Listing" button in top right (blue button with pencil icon)

**If button not visible:**
- Check browser console for errors
- Check if `listingId` is in URL
- Check if listing data loaded successfully

---

### 4. ⚠️ Listing Details Page "Same as Before"
**Need Clarification:** What should be different?

**Current Features:**
- Status banner (ACTIVE/PENDING/RENTED)
- Edit Listing button
- Delete button
- Mark as Rented/Available buttons
- Property details
- Statistics (views, favorites)

**What might be missing:**
- [ ] Map integration?
- [ ] Multiple images carousel?
- [ ] Status update functionality?

---

## Files Modified

### 1. MatchesPage.jsx
```javascript
// Line 34-58
const fetchMatches = async () => {
  // Fixed: Now handles array response correctly
  const matchesData = Array.isArray(response.data) 
    ? response.data 
    : (response.data?.data || []);
};
```

### 2. api.js
```javascript
// Line 255-263
getActive: async (page = 0, size = 12, filters = {}, userId = null) => {
  // Fixed: Now accepts pagination and filters
  const params = new URLSearchParams({ page, size, ...filters });
  if (userId) params.append('userId', userId);
  return apiClient.get(`/listings/active?${params.toString()}`);
},
```

### 3. ListingPage.jsx
```javascript
// Line 32-61
const fetchListings = async () => {
  // Fixed: Now passes userId and has better logging
  const response = await listingService.getActive(currentPage, 12, filterParams, userId);
  console.log('Listings count:', listingsData.length);
};
```

---

## Testing Checklist

### Matches Feature
- [ ] Login as student who found matches
- [ ] Go to Matches page
- [ ] See match cards (not empty state)
- [ ] Click on a match card
- [ ] Modal opens with details
- [ ] Click "Accept Match"
- [ ] Status updates to ACCEPTED
- [ ] Click "Reject Match"
- [ ] Status updates to REJECTED

### Listings Feature
- [ ] Login as landlord
- [ ] Create a new listing
- [ ] Check database: `SELECT * FROM listings ORDER BY created_at DESC LIMIT 1;`
- [ ] Note the status (PENDING or ACTIVE)
- [ ] If PENDING, update to ACTIVE: `UPDATE listings SET status = 'ACTIVE' WHERE id = '...';`
- [ ] Logout
- [ ] Login as student
- [ ] Go to "All Listings"
- [ ] Should see the listing
- [ ] Click on listing
- [ ] Should load details page
- [ ] Click heart icon
- [ ] Should save to favorites

### Landlord Features
- [ ] Login as landlord
- [ ] Go to "Manage Listings"
- [ ] Click on a listing
- [ ] Should navigate to details page
- [ ] Check for "Edit Listing" button (top right, blue)
- [ ] Click "Edit Listing"
- [ ] Should navigate to edit form
- [ ] Form should be pre-filled with listing data
- [ ] Make changes
- [ ] Save
- [ ] Should update listing

---

## Common Issues & Solutions

### Issue: Listings Not Showing
**Solution 1: Check Status**
```sql
SELECT id, title, status FROM listings;
-- If PENDING, change to ACTIVE
UPDATE listings SET status = 'ACTIVE' WHERE status = 'PENDING';
```

**Solution 2: Check Console**
- Open browser DevTools (F12)
- Go to Console tab
- Look for "Listings count: X"
- If 0, check backend logs
- If error, check error message

**Solution 3: Check Backend**
```
GET http://localhost:8080/api/listings/active?page=0&size=12
```
Should return array of listings with status ACTIVE

### Issue: Matches Not Showing
**Solution 1: Check Console**
```
Matches count: X
```
If 0, matches don't exist in database

**Solution 2: Check Database**
```sql
SELECT * FROM matches WHERE user1_id = 'your_user_id' OR user2_id = 'your_user_id';
```

**Solution 3: Create Matches**
- Click "Find Matches" button
- Check backend logs for "Found X matches"
- If 0 found, check preferences compatibility

### Issue: Edit Button Not Visible
**Solution 1: Check URL**
- Should be `/admin/listing-details?id={uuid}`
- If missing `id`, navigation is broken

**Solution 2: Check Page Load**
- Open console
- Look for errors
- Check if listing data loaded

**Solution 3: Check Element**
- Right-click page → Inspect
- Search for "Edit Listing" in HTML
- Check if element exists but hidden

---

## Next Steps

1. **Test Matches Page**
   - Refresh page
   - Check console logs
   - Verify matches appear

2. **Test Listings**
   - Check database for ACTIVE listings
   - If none, update status
   - Refresh student listings page
   - Check console logs

3. **Test Edit Button**
   - Navigate to landlord listing details
   - Take screenshot if button not visible
   - Share console errors

4. **Share Results**
   - Console logs
   - Database query results
   - Screenshots of issues
   - Backend logs

---

## Quick Database Checks

```sql
-- 1. Check matches
SELECT 
    m.id,
    u1.first_name as user1,
    u2.first_name as user2,
    m.compatibility_score,
    m.status
FROM matches m
JOIN users u1 ON m.user1_id = u1.id
JOIN users u2 ON m.user2_id = u2.id
ORDER BY m.created_at DESC;

-- 2. Check listings
SELECT 
    l.id,
    l.title,
    l.status,
    u.first_name as landlord,
    l.created_at
FROM listings l
JOIN users u ON l.landlord_id = u.id
ORDER BY l.created_at DESC;

-- 3. Check if listings are ACTIVE
SELECT COUNT(*) as active_listings
FROM listings
WHERE status = 'ACTIVE';

-- 4. Make all listings ACTIVE (for testing)
UPDATE listings
SET status = 'ACTIVE'
WHERE status IN ('PENDING', 'DRAFT');
```

---

## Summary

**Fixes Applied:**
1. ✅ Matches page data extraction
2. ✅ Listings API pagination and filters
3. ✅ Added comprehensive logging

**Need Testing:**
1. Matches page showing cards
2. Listings showing for students
3. Edit button visibility

**Need Clarification:**
1. What's missing from listing details page?
2. What features should be added?

**Next Actions:**
1. Refresh pages with new code
2. Check console logs
3. Share results
