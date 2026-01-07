# Routing Debug Guide

## Problem
Landlord sees student buttons (Contact Landlord, Share, Message, Email) instead of landlord buttons (Edit, Delete, Mark as Rented).

## Root Cause
Landlord is being directed to the STUDENT listing details page instead of the LANDLORD listing details page.

---

## Two Different Pages

### 1. Student Page (ListingDetailsPage.jsx)
- **Route:** `/listingDetails?listingId={id}`
- **Location:** `frontend/room8/src/pages/ListingDetailsPage/ListingDetailsPage.jsx`
- **Has:** Contact Landlord, Share, Message, Email buttons
- **Does NOT have:** Edit, Delete, Mark as Rented buttons

### 2. Landlord Page (LandlordListingDetailsPage.jsx)
- **Route:** `/admin/listing-details?id={id}`
- **Location:** `frontend/room8/src/pages/admin/ManageListings/LandlordListingDetailsPage.jsx`
- **Has:** Edit, Delete, Mark as Rented, Upload Images buttons
- **Does NOT have:** Contact, Share, Message buttons

---

## How to Check Which Page You're On

### Step 1: Check the URL
When you click a listing as landlord, look at the browser address bar:

**CORRECT (Landlord page):**
```
http://localhost:3000/admin/listing-details?id=abc-123-def
```

**WRONG (Student page):**
```
http://localhost:3000/listingDetails?listingId=abc-123-def
```

### Step 2: Check the Buttons
**If you see:**
- ✅ Edit Listing (blue button, top right)
- ✅ Delete (red button, top right)
- ✅ Mark as Rented (in status banner)
- ✅ Add Images (in photo section)
→ **You're on the LANDLORD page** ✅

**If you see:**
- ❌ Contact Landlord
- ❌ Share Listing
- ❌ Message / Email buttons
→ **You're on the STUDENT page** ❌ (WRONG!)

---

## Fix: Update Navigation

The issue is in how you navigate to the listing details. You need to update the click handler.

### Where to Fix

**File:** `frontend/room8/src/pages/admin/ManageListings/ManageListingsPage.jsx`

**Current code (if broken):**
```javascript
// WRONG - This goes to student page
onClick={() => navigate(`/listingDetails?listingId=${listing.id}`)}
```

**Correct code:**
```javascript
// CORRECT - This goes to landlord page
onClick={() => navigate(`/admin/listing-details?id=${listing.id}`)}
```

---

## Quick Test

### Test 1: Direct URL
1. Copy a listing ID from your database
2. Manually type in browser:
   ```
   http://localhost:3000/admin/listing-details?id=YOUR_LISTING_ID
   ```
3. Press Enter
4. **Expected:** See Edit, Delete, Mark as Rented buttons
5. **If you see Contact Landlord instead:** The route is not configured correctly

### Test 2: From Manage Listings
1. Login as landlord
2. Go to "Manage Listings" (`/admin/manage-listings`)
3. Click on any listing card
4. **Check URL in address bar**
5. **Expected:** `/admin/listing-details?id=...`
6. **If you see:** `/listingDetails?listingId=...` → Navigation is wrong

---

## Solution: Force Correct Navigation

Let me check and fix the ManageListingsPage navigation:
