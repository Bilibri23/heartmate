# Virtual Tour - Known Issues

## Current Status
- ✅ Upload flow working (file uploads to Cloudinary)
- ✅ Backend saves videoTourUrl and virtualTourProvider correctly
- ✅ Frontend extracts URL from photos array
- ⚠️ Pannellum 360° viewer sometimes stuck on loading

## Issue: Pannellum Loading Stuck
**Symptom**: Loading spinner shows indefinitely, no console messages
**Temporary Fix**: Added 5-second timeout to show fallback static image
**Root Cause**: Needs investigation - possibly CDN loading issue or React hydration timing

## Next Steps
1. Investigate why Pannellum CDN script sometimes doesn't trigger initialization
2. Consider self-hosting Pannellum library instead of CDN
3. Add better error boundaries and loading states
4. Test with different browsers and network conditions

## Workaround
Users can still view the 360° image as a static image if Pannellum fails to load. The fallback shows after 5 seconds with a message and "Open Full Image" button.
