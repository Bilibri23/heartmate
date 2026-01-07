# 🚀 Caching Strategy for RoomBuddy

## 📚 What is Caching? (Simple Explanation)

**Caching = Storing data temporarily so you don't have to fetch it again**

### **Real-World Analogy:**
Imagine you're a student who needs to check your class schedule:

**Without Caching:**
```
Every time you want to know your schedule:
1. Walk to admin office (5 minutes)
2. Wait in line (10 minutes)
3. Ask for schedule
4. Walk back (5 minutes)
Total: 20 minutes EVERY TIME
```

**With Caching:**
```
First time:
1. Walk to admin office (5 minutes)
2. Get schedule
3. Take a photo (cache it!)
4. Walk back (5 minutes)
Total: 10 minutes

Every other time:
1. Check photo on phone
Total: 5 seconds!
```

**That's caching!** Store it once, use it many times.

---

## 🎯 Why Cache?

### **1. Speed ⚡**
- **Without cache:** Every page load = API call = 1-3 seconds wait
- **With cache:** Instant! Data already on your device

### **2. Cost 💰**
- **Without cache:** 1000 users × 10 API calls/day = 10,000 API calls
- **With cache:** 1000 users × 2 API calls/day = 2,000 API calls
- **Savings:** 80% fewer API calls = Lower server costs

### **3. User Experience 😊**
- **Without cache:** Loading spinners everywhere
- **With cache:** Instant page loads, smooth experience

### **4. Offline Support 🌐**
- **Without cache:** No internet = App doesn't work
- **With cache:** Can view cached data offline

### **5. Reduced Server Load 🖥️**
- **Without cache:** Server handles every request
- **With cache:** Server only handles new/changed data

---

## 📊 What to Cache in RoomBuddy

### **1. User Data (High Priority)**
```javascript
// Cache this:
{
  userId: "123",
  name: "John Doe",
  email: "john@example.com",
  role: "STUDENT",
  verificationStatus: "VERIFIED",
  profilePhoto: "url",
  university: "University of Yaoundé I"
}

// Why: Used on every page
// Cache duration: Until logout or profile update
// Storage: localStorage + memory
```

### **2. Verification Status (High Priority)**
```javascript
// Cache this:
{
  status: "VERIFIED",
  verifiedAt: "2024-12-10",
  verificationType: "STUDENT_ID"
}

// Why: Checked frequently (banner, permissions)
// Cache duration: 1 hour (can change)
// Storage: localStorage
```

### **3. Listings (Medium Priority)**
```javascript
// Cache this:
{
  listings: [...],
  lastFetched: "2024-12-10T10:30:00",
  filters: { city: "Yaoundé", maxPrice: 50000 }
}

// Why: Listings don't change every second
// Cache duration: 5-10 minutes
// Storage: sessionStorage or React Query
```

### **4. University List (Low Priority - Static)**
```javascript
// Cache this:
const universities = [
  "University of Yaoundé I",
  "ICT University",
  // ... 35+ universities
]

// Why: Never changes (or rarely)
// Cache duration: Forever (until app update)
// Storage: localStorage or hardcoded
```

### **5. Recently Viewed Listings (Medium Priority)**
```javascript
// Cache this:
{
  recentlyViewed: [
    { id: 1, title: "...", viewedAt: "..." },
    { id: 2, title: "...", viewedAt: "..." }
  ]
}

// Why: User might want to revisit
// Cache duration: 7 days
// Storage: localStorage
```

---

## 🛠️ Caching Methods (3 Levels)

### **Level 1: localStorage (Simple)**
**Best for:** User data, preferences, static lists

```javascript
// Save to cache
localStorage.setItem('user', JSON.stringify(userData));

// Get from cache
const user = JSON.parse(localStorage.getItem('user'));

// Check if expired
const cacheTime = localStorage.getItem('user_cached_at');
const isExpired = Date.now() - cacheTime > 3600000; // 1 hour
```

**Pros:**
- ✅ Simple to implement
- ✅ Persists after browser close
- ✅ No library needed

**Cons:**
- ❌ Manual expiration logic
- ❌ No automatic refresh
- ❌ Limited to 5-10MB

---

### **Level 2: React Query (Smart)**
**Best for:** API data, listings, dynamic content

```javascript
import { useQuery } from '@tanstack/react-query';

// Fetch with automatic caching
const { data, isLoading } = useQuery({
  queryKey: ['listings', filters],
  queryFn: () => fetchListings(filters),
  staleTime: 5 * 60 * 1000, // 5 minutes
  cacheTime: 10 * 60 * 1000, // 10 minutes
});
```

**Pros:**
- ✅ Automatic caching
- ✅ Automatic refetching
- ✅ Smart invalidation
- ✅ Loading states built-in

**Cons:**
- ❌ Requires library installation
- ❌ Learning curve

---

### **Level 3: Service Workers (Advanced)**
**Best for:** Offline support, PWA, background sync

```javascript
// service-worker.js
self.addEventListener('fetch', (event) => {
  event.respondWith(
    caches.match(event.request).then((response) => {
      return response || fetch(event.request);
    })
  );
});
```

**Pros:**
- ✅ Full offline support
- ✅ Background sync
- ✅ Push notifications

**Cons:**
- ❌ Complex to implement
- ❌ Debugging is hard
- ❌ Overkill for MVP

---

## 🎯 Recommended Caching Strategy for RoomBuddy

### **Phase 1: MVP (Now) - localStorage**

**What to cache:**
1. ✅ User profile data
2. ✅ Verification status
3. ✅ University list (hardcoded)
4. ✅ Recently viewed listings

**Implementation:**
```javascript
// utils/cache.js
export const cache = {
  set: (key, value, ttl = 3600000) => {
    const item = {
      value,
      expiry: Date.now() + ttl
    };
    localStorage.setItem(key, JSON.stringify(item));
  },
  
  get: (key) => {
    const item = localStorage.getItem(key);
    if (!item) return null;
    
    const { value, expiry } = JSON.parse(item);
    if (Date.now() > expiry) {
      localStorage.removeItem(key);
      return null;
    }
    
    return value;
  },
  
  remove: (key) => {
    localStorage.removeItem(key);
  },
  
  clear: () => {
    localStorage.clear();
  }
};

// Usage
cache.set('user', userData, 3600000); // 1 hour
const user = cache.get('user');
```

---

### **Phase 2: Growth (3-6 months) - React Query**

**What to cache:**
1. ✅ All API calls
2. ✅ Listings with smart invalidation
3. ✅ User data with auto-refresh
4. ✅ Verification status

**Implementation:**
```javascript
// Install
npm install @tanstack/react-query

// Setup
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 5 * 60 * 1000, // 5 minutes
      cacheTime: 10 * 60 * 1000, // 10 minutes
      retry: 1,
    },
  },
});

// App.jsx
<QueryClientProvider client={queryClient}>
  <App />
</QueryClientProvider>

// Usage in components
const { data: listings, isLoading } = useQuery({
  queryKey: ['listings', filters],
  queryFn: () => listingService.getAll(filters),
});
```

---

### **Phase 3: Scale (6-12 months) - Service Workers**

**What to cache:**
1. ✅ All static assets
2. ✅ API responses
3. ✅ Offline fallback pages
4. ✅ Background sync

**Implementation:**
```javascript
// Use Workbox (Google's service worker library)
npm install workbox-webpack-plugin

// Or use Vite PWA plugin
npm install vite-plugin-pwa
```

---

## 📋 Implementation Plan

### **Week 1: Basic localStorage Caching**

**Tasks:**
1. Create `utils/cache.js` helper
2. Cache user data on login
3. Cache verification status
4. Cache recently viewed listings
5. Add cache expiration logic

**Files to modify:**
- `src/utils/cache.js` (new)
- `src/services/authService.js`
- `src/services/verificationService.js`
- `src/pages/ListingDetailsPage.jsx`

---

### **Week 2: Optimize Cache Usage**

**Tasks:**
1. Add cache invalidation on user actions
2. Cache university list
3. Add loading states with cached data
4. Implement "stale-while-revalidate" pattern

**Pattern:**
```javascript
// Show cached data immediately
const cachedData = cache.get('listings');
if (cachedData) {
  setListings(cachedData);
}

// Fetch fresh data in background
fetchListings().then(freshData => {
  setListings(freshData);
  cache.set('listings', freshData);
});
```

---

### **Week 3: Add React Query (Optional)**

**Tasks:**
1. Install React Query
2. Setup QueryClient
3. Migrate API calls to useQuery
4. Add optimistic updates
5. Add cache invalidation

---

## 🧪 Testing Cache

### **Test Scenarios:**

1. **Cache Hit:**
   - Load page → Data from cache (instant)
   - Check network tab → No API call

2. **Cache Miss:**
   - Clear cache → Load page
   - Check network tab → API call made
   - Data cached for next time

3. **Cache Expiration:**
   - Wait for TTL to expire
   - Load page → Fresh API call
   - New data cached

4. **Cache Invalidation:**
   - Update profile → Cache cleared
   - Load page → Fresh data fetched

---

## 📊 Cache Performance Metrics

### **Before Caching:**
```
Page Load Time: 2-3 seconds
API Calls per User: 10-15/day
Server Load: High
User Experience: Slow, loading spinners
```

### **After Caching:**
```
Page Load Time: 0.5-1 second (80% faster)
API Calls per User: 2-3/day (80% reduction)
Server Load: Low
User Experience: Fast, smooth
```

---

## 💡 Best Practices

### **1. Cache Wisely**
```javascript
// ✅ DO cache:
- User profile (changes rarely)
- Verification status (changes rarely)
- Static lists (universities, cities)
- Recently viewed items

// ❌ DON'T cache:
- Real-time chat messages
- Payment transactions
- Sensitive data (passwords, tokens)
- Frequently changing data
```

### **2. Set Appropriate TTL**
```javascript
// User data: 1 hour
cache.set('user', userData, 3600000);

// Listings: 5 minutes
cache.set('listings', listings, 300000);

// Static data: 1 day
cache.set('universities', universities, 86400000);

// Recently viewed: 7 days
cache.set('recentlyViewed', items, 604800000);
```

### **3. Invalidate on Actions**
```javascript
// When user updates profile
const updateProfile = async (data) => {
  await api.updateProfile(data);
  cache.remove('user'); // Invalidate cache
  fetchUserProfile(); // Fetch fresh data
};

// When user creates listing
const createListing = async (data) => {
  await api.createListing(data);
  cache.remove('myListings'); // Invalidate cache
};
```

### **4. Handle Cache Errors**
```javascript
const getUser = () => {
  try {
    return cache.get('user');
  } catch (error) {
    console.error('Cache error:', error);
    return null; // Fallback to API
  }
};
```

---

## 🎯 Quick Wins (Implement Today!)

### **1. Cache User Data on Login**
```javascript
// authService.js
const login = async (credentials) => {
  const response = await api.post('/auth/login', credentials);
  const { user, token } = response.data;
  
  // Cache user data
  localStorage.setItem('user', JSON.stringify(user));
  localStorage.setItem('accessToken', token);
  
  return user;
};
```

### **2. Use Cached User Data**
```javascript
// Dashboard.jsx
const [user, setUser] = useState(() => {
  // Try cache first
  const cached = localStorage.getItem('user');
  return cached ? JSON.parse(cached) : null;
});

useEffect(() => {
  // Fetch fresh data in background
  if (user) {
    fetchUserProfile().then(freshUser => {
      setUser(freshUser);
      localStorage.setItem('user', JSON.stringify(freshUser));
    });
  }
}, []);
```

### **3. Cache Verification Status**
```javascript
// VerificationBanner.jsx
const [verificationStatus, setVerificationStatus] = useState(() => {
  const cached = localStorage.getItem('verificationStatus');
  return cached || 'NOT_VERIFIED';
});

useEffect(() => {
  verificationService.getStatus(userId).then(status => {
    setVerificationStatus(status);
    localStorage.setItem('verificationStatus', status);
  });
}, [userId]);
```

---

## 🚀 Summary

### **What is Caching?**
> Storing data temporarily to avoid fetching it again

### **Why Cache?**
- ⚡ 80% faster page loads
- 💰 80% fewer API calls
- 😊 Better user experience
- 🌐 Partial offline support

### **What to Cache?**
1. User profile data
2. Verification status
3. Listings (5-10 min TTL)
4. University list (static)
5. Recently viewed items

### **How to Cache?**
1. **Phase 1 (Now):** localStorage (simple)
2. **Phase 2 (Later):** React Query (smart)
3. **Phase 3 (Future):** Service Workers (advanced)

### **Quick Wins:**
1. Cache user data on login ✅
2. Cache verification status ✅
3. Cache recently viewed listings ✅

---

**Ready to implement? Let's start with Phase 1! 🚀**
