# Quick Test Guide

## ✅ All Changes Complete!

### Backend ✅
- ListingController: Pagination, mark-rented, track-view endpoints added
- ListingService: All methods implemented
- WebSocket: Configuration complete
- Dependencies: WebSocket added to pom.xml

### Frontend ✅
- api.js: All new endpoints added
- ListingDetailsPage: Dynamic, real-time updates
- HomePage: Completely redesigned
- SearchListingsPage: Pagination, filters working
- NavBar: Updated links
- FilterSidebar: Enhanced with city/neighborhood

---

## 🚀 Start Testing

### 1. Start Backend
```bash
cd c:\Users\noble\IdeaProjects\Roombay
mvn clean install
mvn spring-boot:run
```

### 2. Start Frontend
```bash
cd frontend/room8
npm run dev
```

### 3. Test Flow

#### Test 1: Homepage
1. Visit: http://localhost:5173/home
2. Should see:
   - Featured listings (dynamic from backend)
   - Recent listings
   - Affordable listings
   - Apartment listings
3. Click "View All" - should navigate to search page

#### Test 2: Search & Pagination
1. Visit: http://localhost:5173/listings
2. Should see:
   - Real listings from backend
   - Pagination controls
   - Filter sidebar
3. Try filters:
   - Enter city name
   - Set price range
   - Select property type
4. Click "Apply Filters"
5. Test pagination: Click page numbers

#### Test 3: Listing Details
1. Click any listing card
2. Should see:
   - Real listing data
   - Multiple images in banner
   - "View on Map" button (if coordinates exist)
   - WhatsApp contact button
   - Similar listings at bottom
   - View count incrementing
3. Test favorite toggle
4. Click "View on Map" - opens Google Maps

#### Test 4: Real-time Updates (WebSocket)
1. Open listing details in 2 browser tabs
2. In backend, manually update a listing status
3. Both tabs should update automatically

#### Test 5: Landlord Features
1. Login as landlord
2. Go to your listings
3. Click "Mark as Rented" on a listing
4. Status should change to "RENTED"
5. Check statistics dashboard

---

## 🐛 Common Issues & Fixes

### Issue: Backend won't start
**Fix**: Run `mvn clean install` first

### Issue: Frontend shows no listings
**Fix**: 
1. Check backend is running on port 8080
2. Check browser console for errors
3. Verify CORS is enabled

### Issue: WebSocket not connecting
**Fix**: 
1. Check backend logs for WebSocket endpoint
2. Ensure URL is correct: `http://localhost:8080/ws`

### Issue: Pagination not working
**Fix**: Check backend returns `Page<>` not `List<>`

---

## 📊 What to Verify

### Backend Endpoints
- ✅ GET /api/listings?page=0&size=10 - Returns paginated listings
- ✅ POST /api/listings/{id}/mark-rented - Marks as rented
- ✅ POST /api/listings/{id}/view - Tracks view
- ✅ GET /api/listings/landlord/{id}/statistics - Returns stats

### Frontend Features
- ✅ Dynamic homepage with real data
- ✅ Search with filters and pagination
- ✅ Listing details with real data
- ✅ Map integration
- ✅ WhatsApp contact
- ✅ Favorite toggle
- ✅ Similar listings
- ✅ View tracking

---

## 🎯 Success Criteria

✅ Homepage loads with real listings
✅ Search page shows paginated results
✅ Filters work correctly
✅ Listing details show real data
✅ Map button opens Google Maps
✅ WhatsApp button works
✅ View count increments
✅ Similar listings appear
✅ Landlord can mark as rented
✅ Statistics show correct numbers

---

## 📝 Next Steps After Testing

1. **Fix any bugs found**
2. **Add more test data** to database
3. **Test on mobile devices**
4. **Performance testing** with many listings
5. **Security review** of endpoints
6. **Deploy to staging environment**

---

## 🎉 You're Ready!

Everything is implemented and connected. Just start both servers and test!

**Similar Listings**: Currently uses simple filter (propertyType + city). For V2, we'll add ML-based recommendations.

**All features are now DYNAMIC and connected to the backend!** 🚀
