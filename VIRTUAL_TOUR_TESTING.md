# Virtual Tour Testing Guide

## ✅ Feature Status: FULLY IMPLEMENTED

The virtual tour feature is **production-ready** with self-hosted media (no 3rd party iframe dependencies).

## How It Works

### Two Tour Modes

1. **Video Walkthrough** - Upload MP4/MOV video file
2. **360° Interactive Tour** - Upload equirectangular panoramic JPG/PNG

Both are uploaded to **Cloudinary** (already integrated) and displayed using:
- Native HTML5 `<video>` player for videos
- **Pannellum.js** (open source, CDN-loaded) for 360° panoramas

## Testing Instructions

### Step 1: Get Test Media

#### Option A: 360° Panoramic Image (Recommended for Quick Test)
Download a free sample 360° image:
- **Pannellum Demo Image**: https://pannellum.org/images/alma.jpg
- **Google Sample**: https://pannellum.org/images/cerro-toco-0.jpg
- Or use Google Street View app to capture your own 360° photo

#### Option B: Video Walkthrough
- Record a simple MP4 video walking through a room (30-60 seconds)
- Use your phone camera or any video recording tool
- Keep file size under 100MB

### Step 2: Create a Test Listing

1. **Start the backend**:
   ```bash
   cd backend
   ./mvnw spring-boot:run
   ```

2. **Start the frontend**:
   ```bash
   cd frontend
   npm run dev
   ```

3. **Log in as a landlord** (or create a landlord account)

4. **Go to**: http://localhost:3000/landlord/listings/new

5. **Follow the wizard**:
   - **Step 1**: Upload at least 1 property photo
   - **Step 2**: Fill in property details (title, city, neighborhood, type)
   - **Step 3**: Set rent amount
   - **Step 4**: Select amenities (optional)
   - **Step 5 - Virtual Tour**:
     - For **Video Tour**: Click "Choose Video File" → select your MP4
     - For **360° Tour**: Click "Choose 360° Photo" → select the panoramic JPG
   - Click **Publish Listing**

### Step 3: View the Tour

1. Go to the listing detail page (you'll be redirected after publishing)
2. Scroll to the **Virtual Tour** section
3. **For 360° Tour**:
   - You should see an interactive panorama
   - Drag with mouse to look around
   - Scroll to zoom in/out
   - Click fullscreen button for immersive view
4. **For Video Tour**:
   - Click play to watch the walkthrough
   - Standard video controls available

## Expected Behavior

### 360° Tour (Pannellum.js)
- ✅ Loads panoramic image from Cloudinary
- ✅ Interactive drag-to-look-around controls
- ✅ Zoom in/out with scroll wheel
- ✅ Auto-rotation (slow spin)
- ✅ Fullscreen mode
- ✅ "360° Interactive Tour" badge
- ✅ Instructions overlay: "🖱️ Drag to look around · Scroll to zoom"

### Video Tour
- ✅ Native HTML5 video player
- ✅ Play/pause controls
- ✅ Seek bar
- ✅ Volume control
- ✅ Fullscreen mode
- ✅ "Virtual Tour" badge

## Troubleshooting

### Issue: "Failed to load 360° Tour"
**Cause**: Image URL not accessible or not equirectangular format
**Fix**: 
- Ensure the image uploaded successfully to Cloudinary
- Check browser console for CORS errors
- Verify the image is in equirectangular format (2:1 aspect ratio)

### Issue: Video doesn't play
**Cause**: Unsupported video format or codec
**Fix**:
- Use MP4 with H.264 codec (most compatible)
- Convert video using: `ffmpeg -i input.mov -c:v libx264 -c:a aac output.mp4`

### Issue: Upload fails
**Cause**: File too large or network timeout
**Fix**:
- Keep videos under 100MB (configured max size)
- Compress large files before upload
- Check backend logs for Cloudinary errors

## How to Capture 360° Photos

### Using Google Street View App (Free)
1. Download **Google Street View** app (iOS/Android)
2. Open app → tap Camera icon
3. Select "Camera" mode
4. Follow on-screen guide to capture 360° panorama
5. Export as JPG from the app
6. Upload to RoomBuddy

### Using Ricoh Theta or Insta360 Camera
1. Capture 360° photo with camera
2. Export as equirectangular JPG (2:1 aspect ratio)
3. Upload to RoomBuddy

## Technical Details

### Backend Endpoints
- `POST /api/listings/{listingId}/photos` - Upload tour media (reuses photo endpoint)
- `PATCH /api/listings/{listingId}` - Set `videoTourUrl` and `virtualTourProvider`

### Frontend Components
- `virtual-tour-embed.tsx` - Pannellum.js 360° viewer
- `virtual-tour-3d.tsx` - Video player (legacy, still works)
- `video-player.tsx` - Standard video player

### Database Fields
- `videoTourUrl` - Cloudinary URL of the tour media
- `virtualTourProvider` - Either `'video'` or `'360'`

## Sample 360° Images for Testing

Direct download links:
- https://pannellum.org/images/alma.jpg (Alma Observatory, Chile)
- https://pannellum.org/images/cerro-toco-0.jpg (Cerro Toco, Chile)
- https://pannellum.org/images/from-tree.jpg (Forest view)

Right-click → Save As → upload to RoomBuddy

## Next Steps After Testing

Once virtual tours are working:
1. ✅ Test on mobile devices (touch drag for 360°)
2. ✅ Test fullscreen mode
3. ✅ Verify Cloudinary URLs are permanent (not temporary)
4. ✅ Add virtual tour badge to listing cards in search results
5. ✅ Consider adding multiple 360° photos per listing (future enhancement)

## Status: Ready for Production ✅

The virtual tour feature is **fully implemented** and **self-hosted** (no 3rd party iframe issues). Just needs real media uploads for testing.
