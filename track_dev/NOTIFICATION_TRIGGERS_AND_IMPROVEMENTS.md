# Notification Triggers & System Improvements

## 🔔 **What Triggers Notifications**

### **Current Notification Triggers:**

1. **ApplicationService** → `notifyApplicationReceived()`
   - When: Student applies to a listing
   - Recipient: Landlord
   - Type: `APPLICATION_RECEIVED`

2. **ApplicationService** → `notifyApplicationAccepted()` / `notifyApplicationRejected()`
   - When: Landlord accepts/rejects application
   - Recipient: Student
   - Types: `APPLICATION_ACCEPTED`, `APPLICATION_REJECTED`

3. **MessageService** → `notifyNewMessage()`
   - When: User receives a new message
   - Recipient: Message receiver
   - Type: `NEW_MESSAGE`

4. **LeaseService** → `notifyLeaseCreated()`
   - When: Lease is created after application acceptance
   - Recipient: Student
   - Type: `LEASE_CREATED`

5. **PaymentService** → `notifyPaymentVerified()`, `notifyPaymentReceived()`, `notifyPaymentRejected()`
   - When: Payment is verified/received/rejected
   - Recipients: Student (verified/rejected), Landlord (received)
   - Types: `PAYMENT_VERIFIED`, `PAYMENT_RECEIVED`, `PAYMENT_REJECTED`

6. **ReviewService** → `notifyReviewReceived()`
   - When: User receives a review
   - Recipient: Reviewed user
   - Type: `REVIEW_RECEIVED`

7. **DisputeService** → `notifyDisputeUpdated()`, `notifyDisputeResolved()`
   - When: Dispute status changes or is resolved
   - Recipient: Dispute participants
   - Types: `DISPUTE_UPDATED`, `DISPUTE_RESOLVED`

### **Missing Notification Triggers:**
- ❌ New match found (for roommate matching)
- ❌ Mutual match (when both users like each other)
- ❌ Listing favorited by multiple users (for landlords)
- ❌ Verification status updates

---

## 📋 **Profile/Settings Duplication Fix**

### **Current Issue:**
- `BasicProfileInfoSection` (with profile photo) is in Settings
- Profile page also has profile display
- Duplication violates DRY principle

### **Solution:**
1. ✅ Remove `BasicProfileInfoSection` from Settings
2. ✅ Add profile photo upload to Profile page (`EditProfilePanel`)
3. ✅ Keep Settings for: Personal Info (name, email, phone), Security, Privacy, Display, Account Management

---

## 🎯 **Recommendation Feature Analysis**

### **How It Works:**
The `RecommendationService` uses a **hybrid scoring system**:

1. **Preference-Based Scoring (60% weight)**
   - Budget match (40% of preference score)
   - Location match (30% of preference score)
   - Distance to university (20% of preference score)
   - Property type match (10% of preference score)

2. **Behavioral Scoring (40% weight)**
   - Recent views of similar listings
   - Favorited listing patterns
   - Property type interest
   - Price range similarity

### **Is It Necessary?**
✅ **YES** - But needs improvement:
- Currently not exposed to users (90% complete)
- Helps students find relevant listings faster
- Reduces search fatigue
- Increases engagement

### **What's Missing:**
- ❌ "Recommended For You" feed on student dashboard
- ❌ Match explanation ("You matched because...")
- ❌ Integration with frontend

---

## 🚀 **Implementation Roadmap**

### **1. Redis Caching Strategy**

**Purpose:** Reduce database load, improve response times

**Implementation Plan:**
```java
// Cache Configuration
@Configuration
@EnableCaching
public class RedisConfig {
    @Bean
    public RedisTemplate<String, Object> redisTemplate() {
        // Configure Redis connection
    }
}

// Cache Strategy:
- Listings: Cache for 5 minutes (frequently accessed)
- User profiles: Cache for 10 minutes
- Recommendations: Cache for 15 minutes (expensive computation)
- Match results: Cache for 30 minutes
- Notification counts: Cache for 1 minute (real-time)
```

**Cache Keys:**
- `listing:{id}` - Individual listing
- `listings:active:{page}:{size}` - Paginated listings
- `recommendations:{userId}` - User recommendations
- `matches:{userId}` - User matches
- `profile:{userId}` - User profile
- `notifications:unread:{userId}` - Unread count

---

### **2. Advanced Rate Limiting**

**Current:** IP-based only (insufficient)

**Improved Strategy:**
```java
// Multi-factor rate limiting
1. User-based (authenticated users)
   - Different limits per role (student, landlord, admin)
   - Track by userId + IP

2. Endpoint-specific limits
   - Login: 5 attempts/minute
   - Registration: 3 attempts/hour
   - API calls: 100 requests/minute
   - File uploads: 10 uploads/hour

3. Sliding window algorithm
   - Use Redis for distributed rate limiting
   - Track: userId, IP, endpoint, timestamp

4. Progressive penalties
   - First violation: Warning
   - Multiple violations: Temporary ban (1 hour)
   - Severe violations: Account suspension
```

**Implementation:**
- Use `Bucket4j` with Redis backend
- Custom annotation: `@RateLimited(userLimit=100, ipLimit=50)`
- Global exception handler for rate limit errors

---

### **3. Test Coverage**

#### **Backend Tests:**

**Unit Tests (Target: 80% coverage)**
```java
// Service Layer
- NotificationServiceTest
- MatchingServiceTest
- RecommendationServiceTest
- PaymentServiceTest
- ApplicationServiceTest

// Controller Layer
- AuthControllerTest
- ListingControllerTest
- MatchControllerTest

// Repository Layer
- Custom query tests
- Pagination tests
```

**Integration Tests (Target: 70% coverage)**
```java
// API Integration Tests
- Authentication flow
- Listing CRUD operations
- Matching algorithm
- Payment processing
- Notification delivery

// Database Integration
- Transaction rollback tests
- Constraint validation
- Migration tests
```

#### **Frontend Tests:**

**Component Tests (Target: 60% coverage)**
```javascript
// Using React Testing Library
- NotificationBell.test.jsx
- ListingCard.test.jsx
- MatchCard.test.jsx
- ProfilePage.test.jsx

// Integration Tests
- Authentication flow
- Listing search/filter
- Match interaction
- Notification display
```

**E2E Tests (Critical paths)**
```javascript
// Using Playwright/Cypress
- User registration → Profile completion → Find matches
- Landlord: Create listing → Receive application → Accept
- Student: Apply → Get accepted → Create lease → Make payment
```

---

## 💡 **Matching System Improvements**

### **Current Status: 90% Complete**

**What Exists:**
✅ Sophisticated scoring (budget 30%, lifestyle 25%, schedule 20%, location 15%, habits 10%)
✅ Deal-breaker filtering
✅ Gender/age preference handling
✅ WhatsApp exchange on mutual match

**What's Missing:**

1. **"Recommended For You" Feed** ❌
   - Add to student dashboard
   - Show top 5-10 matches
   - Refresh daily

2. **Match Explanation** ❌
   - Show why users matched
   - "You matched because: Similar budget, same lifestyle preferences, compatible schedules"

3. **Real-time Match Notifications** ❌
   - WebSocket notification when new match found
   - Toast notification on dashboard

4. **Match Percentage on Listings** ❌
   - Show compatibility score on listing cards
   - Help students identify best matches quickly

5. **Online Status Tracking** ⚠️
   - Track last active time
   - Show "Online", "Away", "Offline"
   - Update via WebSocket heartbeat

6. **File Attachments in Messages** ⚠️
   - Currently: Text + listing shares only
   - Add: Image uploads, PDF documents
   - Max file size: 5MB per file

---

## 📝 **Action Items**

### **Priority 1 (Critical):**
1. ✅ Fix profile/settings duplication
2. ✅ Add match notifications
3. ✅ Implement Redis caching
4. ✅ Add advanced rate limiting

### **Priority 2 (Important):**
5. Expose recommendation feed to users
6. Add match explanations
7. Show match percentage on listings
8. Implement online status tracking

### **Priority 3 (Nice to Have):**
9. File attachments in messages
10. Comprehensive test coverage
11. E2E testing setup

---

## 🔧 **Technical Debt**

1. **Notification System:**
   - Missing match notifications
   - No notification preferences (users can't control what they receive)

2. **Matching System:**
   - Algorithm works but not exposed
   - No explanation of match reasons
   - No real-time updates

3. **Caching:**
   - No caching currently implemented
   - Database queries on every request

4. **Rate Limiting:**
   - Basic IP-based only
   - No user-based or endpoint-specific limits

5. **Testing:**
   - Minimal test coverage
   - No E2E tests

