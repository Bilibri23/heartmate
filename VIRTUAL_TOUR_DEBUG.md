# Virtual Tour Debugging Guide

## Issue: 360° Tour Not Displaying on Listing Page

### Debug Steps

1. **Check Browser Console**
   - Open the listing detail page in your browser
   - Press F12 to open Developer Tools
   - Go to the Console tab
   - Look for these log messages:
     ```
     Virtual Tour Data: {
       videoTourUrl: "...",
       virtualTourProvider: "360",
       videoTour: null,
       videoTourEmbedCode: null
     }
     ```

2. **Verify Data in Console**
   - `videoTourUrl` should be a Cloudinary URL (e.g., `https://res.cloudinary.com/...`)
   - `virtualTourProvider` should be exactly `"360"` (not `"video"`)
   - If these are correct, you should also see:
     ```
     🔮 Initializing Pannellum 360° viewer with URL: https://res.cloudinary.com/...
     ✅ Pannellum 360° tour loaded successfully
     ```

3. **Common Issues & Fixes**

   **Issue A: `virtualTourProvider` is `null` or `undefined`**
   - **Cause**: The field wasn't saved during upload
   - **Fix**: Re-upload the 360° image through the landlord listing form
   - **Verify**: Check that Step 5 shows "360° Virtual Tour" option selected

   **Issue B: `videoTourUrl` is `null` or `undefined`**
   - **Cause**: File upload failed or wasn't completed
   - **Fix**: Check backend logs for upload errors
   - **Verify**: The file should appear in your Cloudinary dashboard

   **Issue C: Pannellum shows error "Failed to load 360° Tour"**
   - **Cause**: Image is not in equirectangular format or CORS issue
   - **Fix**: 
     - Ensure the image is a proper 360° panorama (2:1 aspect ratio)
     - Test with sample image: https://pannellum.org/images/alma.jpg
     - Check Cloudinary CORS settings

   **Issue D: Virtual Tour section doesn't appear at all**
   - **Cause**: The condition `(listing.videoTour || listing.videoTourUrl || listing.videoTourEmbedCode)` is false
   - **Fix**: Verify `videoTourUrl` is set in the database
   - **SQL Check**:
     ```sql
     SELECT id, title, video_tour_url, virtual_tour_provider 
     FROM property_listings 
     WHERE id = 'your-listing-id';
     ```

4. **Manual Database Fix (If Needed)**

   If the `virtualTourProvider` field is missing but `videoTourUrl` exists:

   ```sql
   UPDATE property_listings 
   SET virtual_tour_provider = '360' 
   WHERE id = 'your-listing-id' 
   AND video_tour_url IS NOT NULL;
   ```

5. **Re-test Upload Flow**

   To ensure future uploads work correctly:
   
   a. Create a new test listing
   b. In Step 5 (Virtual Tour):
      - Click "Choose 360° Photo"
      - Select a 360° equirectangular image
      - You should see a preview with "360° Virtual Tour Ready"
   c. Click "Publish Listing"
   d. Wait for redirect to landlord dashboard
   e. Click on the listing to view detail page
   f. Scroll to Virtual Tour section
   g. You should see an interactive 360° panorama

6. **Expected Behavior**

   When working correctly, you should see:
   - ✅ Section header: "360° Virtual Tour" (not just "Virtual Tour")
   - ✅ Interactive panorama that you can drag to look around
   - ✅ Zoom controls (scroll wheel)
   - ✅ Fullscreen button
   - ✅ Auto-rotation (slow spin)
   - ✅ Badge: "360° Interactive Tour" with pulsing dot
   - ✅ Instructions: "🖱️ Drag to look around · Scroll to zoom"
   - ✅ Info chips showing "360° View" and "Interactive" controls

## Quick Test with Sample Image

1. Download: https://pannellum.org/images/alma.jpg
2. Go to: http://localhost:3000/landlord/listings/new
3. Upload property photos → fill details
4. Step 5: Click "Choose 360° Photo" → select alma.jpg
5. Publish listing
6. View listing detail page
7. Should see working 360° tour

## Backend Verification

Check backend logs when uploading:
```
Uploading photo for listing: [listing-id]
Updating listing: [listing-id]
```

The PATCH request should include:
```json
{
  "videoTourUrl": "https://res.cloudinary.com/...",
  "virtualTourProvider": "360"
}
```

## Frontend Network Tab

1. Open DevTools → Network tab
2. Filter by "Fetch/XHR"
3. Look for PATCH request to `/api/listings/{id}`
4. Check Request Payload contains both fields
5. Check Response shows updated listing with both fields set

## Still Not Working?

If you've verified all the above and it's still not displaying:

1. **Clear browser cache** (Ctrl+Shift+Delete)
2. **Hard refresh** the page (Ctrl+Shift+R)
3. **Check if Pannellum CDN is accessible**:
   - Open: https://cdn.jsdelivr.net/npm/pannellum@2.5.6/build/pannellum.js
   - Should download a JavaScript file
4. **Try a different browser** (Chrome, Firefox, Edge)
5. **Check for JavaScript errors** in console that might block execution

## Contact Points

If issue persists, provide:
- Browser console logs (full output)
- Network tab screenshot showing PATCH request/response
- Database query result for the listing
- Screenshot of the listing detail page
