# Troubleshooting Guide

## Cloudinary Upload Errors

### Error: "Failed to upload image: api.cloudinary.com"

This error typically indicates one of the following issues:

1. **Network/Connection Issue**
   - Check your internet connection
   - Verify Cloudinary service is accessible
   - Check firewall settings

2. **Cloudinary Configuration**
   - Verify your Cloudinary credentials in `application.properties`:
     ```properties
     cloudinary.cloud-name=your-cloud-name
     cloudinary.api-key=your-api-key
     cloudinary.api-secret=your-api-secret
     ```
   - Or set environment variables:
     - `CLOUDINARY_CLOUD_NAME`
     - `CLOUDINARY_API_KEY`
     - `CLOUDINARY_API_SECRET`

3. **Invalid Credentials**
   - Log in to [Cloudinary Dashboard](https://console.cloudinary.com/)
   - Verify your API credentials are correct
   - Ensure your account is active

4. **File Size/Type Issues**
   - Maximum file size: 10MB
   - Allowed types: JPEG, JPG, PNG, WEBP

### Solution Steps:

1. **Test Cloudinary Connection:**
   ```bash
   # Check if Cloudinary is accessible
   curl https://api.cloudinary.com/v1_1/your-cloud-name/resources/image
   ```

2. **Verify Credentials:**
   - Go to Cloudinary Dashboard → Settings → Security
   - Copy your Cloud Name, API Key, and API Secret
   - Update `application.properties` or environment variables

3. **Check Logs:**
   - Look for more detailed error messages in the logs
   - Check if it's a timeout, connection refused, or authentication error

## Redis Connection Warnings

### Warning: "Cannot get Jedis connection"

This is **expected behavior** if Redis is not running. The application will:
- ✅ Continue to work normally
- ✅ Use in-memory cache instead of Redis
- ✅ Skip advanced rate limiting (falls back to basic rate limiting)

### To Enable Redis (Optional):

1. **Install Redis:**
   ```bash
   # Windows (using Chocolatey)
   choco install redis-64
   
   # Or download from: https://github.com/microsoftarchive/redis/releases
   ```

2. **Start Redis:**
   ```bash
   redis-server
   ```

3. **Verify Redis is Running:**
   ```bash
   redis-cli ping
   # Should return: PONG
   ```

4. **Restart Backend:**
   - The application will automatically detect and use Redis
   - You'll see: "Redis connection established successfully" in logs

### If You Don't Want Redis:

- The application works fine without Redis
- Warnings are now suppressed (only logged at DEBUG level)
- All features work except distributed caching and advanced rate limiting

## Profile Photo Not Persisting

### Issue: Photo disappears after refresh

**Fixed!** Profile photos now auto-save immediately after upload.

If you still experience issues:

1. **Check Browser Console:**
   - Look for JavaScript errors
   - Check network tab for failed API calls

2. **Verify Backend:**
   - Check if profile update API is being called
   - Look for errors in backend logs

3. **Check Profile Data:**
   - Go to Settings → Personal Information
   - Verify the photo URL is saved in the profile

## Match Percentage Not Showing

### Issue: No match percentage on listings

**Requirements:**
- You must be logged in as a **Student**
- You must have set your **Roommate Preferences**
- Listings must be active and verified

**To See Match Percentage:**

1. **Set Preferences:**
   - Go to Dashboard → Preferences
   - Set your budget, location, and other preferences

2. **View Listings:**
   - Go to Listings page
   - You should see green "% Match" badges on listing cards

3. **Check User Role:**
   - Match percentage only shows for Students
   - Landlords won't see match percentages

## Recommended Matches Not Showing

### Issue: No recommended matches on dashboard

**Requirements:**
- You must be a **Student**
- You must have **Roommate Preferences** set
- Other users must also be looking for roommates

**To See Recommended Matches:**

1. **Set Preferences:**
   - Go to Settings → Preferences
   - Enable "Looking for Roommate"
   - Set your preferences

2. **Find Matches:**
   - Go to Dashboard
   - Look for "Compatible Roommates" section
   - Or go to Matches page

3. **If No Matches:**
   - Make sure other users have also set preferences
   - Check compatibility threshold (minimum 60%)

## Online Status Not Working

### Issue: No green dot showing in chat

**Requirements:**
- Both users must be connected via WebSocket
- WebSocket connection must be established

**To Fix:**

1. **Check WebSocket Connection:**
   - Open browser DevTools → Network tab
   - Look for WebSocket connection to `/ws`
   - Should show status: 101 Switching Protocols

2. **Verify Backend:**
   - Check backend logs for WebSocket connection messages
   - Ensure WebSocket endpoint is accessible

3. **Check User IDs:**
   - Both users must be logged in
   - User IDs must be valid UUIDs

## General Debugging Tips

1. **Check Logs:**
   - Backend: Look for ERROR and WARN messages
   - Frontend: Check browser console for errors

2. **Verify Configuration:**
   - Check `application.properties` for correct values
   - Verify environment variables are set

3. **Test Endpoints:**
   - Use Swagger UI: `http://localhost:8080/swagger-ui.html`
   - Test API endpoints directly

4. **Clear Cache:**
   - Clear browser cache
   - Restart backend server
   - Clear Redis cache (if using Redis)

5. **Check Database:**
   - Verify data is being saved
   - Check for foreign key constraints
   - Verify user IDs exist

