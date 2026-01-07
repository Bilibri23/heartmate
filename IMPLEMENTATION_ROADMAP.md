# Implementation Roadmap

## ✅ **Completed**

1. **Fixed Profile/Settings Duplication**
   - ✅ Removed `BasicProfileInfoSection` from Settings
   - ✅ Added profile photo upload to Profile page (`EditProfilePanel`)
   - ✅ Moved basic info fields (DOB, gender, nationality, occupation) to Profile

2. **Notification System Fixes**
   - ✅ Fixed API client usage
   - ✅ Added WebSocket real-time notifications
   - ✅ Improved error handling

---

## 🚀 **Priority 1: Critical Implementations**

### **1. Redis Caching Strategy**

**Dependencies:**
```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<dependency>
    <groupId>redis.clients</groupId>
    <artifactId>jedis</artifactId>
</dependency>
```

**Configuration:**
```java
// RedisConfig.java
@Configuration
@EnableCaching
public class RedisConfig {
    
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName("localhost");
        config.setPort(6379);
        return new JedisConnectionFactory(config);
    }
    
    @Bean
    public RedisTemplate<String, Object> redisTemplate() {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory());
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        return template;
    }
    
    @Bean
    public CacheManager cacheManager() {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(5))
            .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));
        
        return RedisCacheManager.builder(redisConnectionFactory())
            .cacheDefaults(config)
            .withCacheConfiguration("listings", config.entryTtl(Duration.ofMinutes(5)))
            .withCacheConfiguration("recommendations", config.entryTtl(Duration.ofMinutes(15)))
            .withCacheConfiguration("matches", config.entryTtl(Duration.ofMinutes(30)))
            .withCacheConfiguration("profiles", config.entryTtl(Duration.ofMinutes(10)))
            .build();
    }
}
```

**Usage:**
```java
@Service
public class ListingService {
    
    @Cacheable(value = "listings", key = "#id")
    public ListingResponse getListing(UUID id) {
        // Database query
    }
    
    @CacheEvict(value = "listings", key = "#id")
    public void updateListing(UUID id, ListingRequest request) {
        // Update and evict cache
    }
    
    @Cacheable(value = "recommendations", key = "#userId")
    public List<ScoredListing> getRecommendedListings(UUID userId) {
        // Expensive computation
    }
}
```

---

### **2. Advanced Rate Limiting**

**Dependencies:**
```xml
<dependency>
    <groupId>com.github.vladimir-bukhtoyarov</groupId>
    <artifactId>bucket4j-core</artifactId>
    <version>8.7.0</version>
</dependency>
<dependency>
    <groupId>com.github.vladimir-bukhtoyarov</groupId>
    <artifactId>bucket4j-redis</artifactId>
    <version>8.7.0</version>
</dependency>
```

**Implementation:**
```java
// RateLimitConfig.java
@Configuration
public class RateLimitConfig {
    
    @Bean
    public RedisTemplate<String, byte[]> rateLimitRedisTemplate() {
        // Configure for rate limiting
    }
    
    @Bean
    public ProxyManager<String> proxyManager() {
        return Bucket4j.extension(Redis.class)
            .proxyManagerForRedis(rateLimitRedisTemplate.getConnectionFactory());
    }
}

// RateLimitInterceptor.java
@Component
public class RateLimitInterceptor implements HandlerInterceptor {
    
    @Autowired
    private ProxyManager<String> proxyManager;
    
    @Override
    public boolean preHandle(HttpServletRequest request, 
                           HttpServletResponse response, 
                           Object handler) {
        
        String userId = getUserId(request);
        String ip = getClientIp(request);
        String endpoint = request.getRequestURI();
        
        // User-based rate limiting
        if (userId != null) {
            Bucket userBucket = proxyManager.builder()
                .build(userId + ":" + endpoint, () -> {
                    return BucketConfiguration.builder()
                        .addLimit(Bandwidth.classic(100, Refill.intervally(100, Duration.ofMinutes(1))))
                        .build();
                });
            
            if (!userBucket.tryConsume(1)) {
                response.setStatus(429);
                response.setHeader("X-RateLimit-Limit", "100");
                response.setHeader("X-RateLimit-Remaining", "0");
                return false;
            }
        }
        
        // IP-based rate limiting
        Bucket ipBucket = proxyManager.builder()
            .build(ip + ":" + endpoint, () -> {
                return BucketConfiguration.builder()
                    .addLimit(Bandwidth.classic(50, Refill.intervally(50, Duration.ofMinutes(1))))
                    .build();
            });
        
        if (!ipBucket.tryConsume(1)) {
            response.setStatus(429);
            return false;
        }
        
        return true;
    }
}
```

**Endpoint-Specific Limits:**
```java
@RateLimited(
    userLimit = 5,
    ipLimit = 3,
    windowMinutes = 1
)
@PostMapping("/auth/login")
public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
    // Login logic
}
```

---

### **3. Test Coverage**

#### **Backend Unit Tests:**

```java
// NotificationServiceTest.java
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {
    
    @Mock
    private NotificationRepository notificationRepository;
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private SimpMessagingTemplate messagingTemplate;
    
    @InjectMocks
    private NotificationService notificationService;
    
    @Test
    void testCreateNotification() {
        // Test notification creation
    }
    
    @Test
    void testGetUnreadCount() {
        // Test unread count retrieval
    }
}
```

#### **Backend Integration Tests:**

```java
// ListingControllerIntegrationTest.java
@SpringBootTest
@AutoConfigureMockMvc
class ListingControllerIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    void testCreateListing() throws Exception {
        mockMvc.perform(post("/api/listings")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(listingJson))
            .andExpect(status().isCreated());
    }
}
```

#### **Frontend Tests:**

```javascript
// NotificationBell.test.jsx
import { render, screen, fireEvent } from '@testing-library/react';
import NotificationBell from './NotificationBell';

describe('NotificationBell', () => {
    test('displays unread count', () => {
        render(<NotificationBell />);
        // Test implementation
    });
    
    test('opens dropdown on click', () => {
        render(<NotificationBell />);
        fireEvent.click(screen.getByRole('button'));
        expect(screen.getByText('Notifications')).toBeInTheDocument();
    });
});
```

---

## 🎯 **Priority 2: Matching System Improvements**

### **1. Add Match Notifications**

```java
// MatchingService.java
public void createMatch(UUID user1Id, UUID user2Id, int compatibilityScore) {
    // ... existing match creation logic ...
    
    // Send notification to both users
    notificationService.notifyNewMatch(user1Id, match.getId(), user2FirstName, compatibilityScore);
    notificationService.notifyNewMatch(user2Id, match.getId(), user1FirstName, compatibilityScore);
}

// NotificationService.java
public void notifyNewMatch(UUID userId, UUID matchId, String matchedUserName, int score) {
    createNotification(userId, Notification.NotificationType.NEW_MATCH,
        "New Match Found!",
        "You matched with " + matchedUserName + " (" + score + "% compatibility)",
        matchId, "MATCH", "/admin/matches");
}
```

### **2. Match Explanation Feature**

```java
// MatchResponse.java
public class MatchResponse {
    private String explanation; // "You matched because: Similar budget, compatible schedules"
    
    // Generate explanation
    public String generateExplanation(Match match) {
        List<String> reasons = new ArrayList<>();
        if (match.getBudgetScore() > 80) reasons.add("similar budget");
        if (match.getLifestyleScore() > 80) reasons.add("compatible lifestyle");
        if (match.getScheduleScore() > 80) reasons.add("similar schedules");
        
        return "You matched because: " + String.join(", ", reasons);
    }
}
```

### **3. Recommended For You Feed**

```java
// StudentDashboardController.java
@GetMapping("/recommended-matches")
public ResponseEntity<List<MatchResponse>> getRecommendedMatches(
        @AuthenticationPrincipal UserDetails userDetails) {
    UUID userId = UUID.fromString(userDetails.getUsername());
    List<MatchResponse> matches = matchingService.getRecommendedMatches(userId, 10);
    return ResponseEntity.ok(matches);
}
```

### **4. Match Percentage on Listings**

```java
// ListingResponse.java
public class ListingResponse {
    private Integer compatibilityScore; // For students viewing listings
    private String compatibilityReason; // "85% match - Similar budget and location"
}
```

---

## 📋 **Implementation Checklist**

### **Phase 1: Infrastructure (Week 1)**
- [ ] Set up Redis server
- [ ] Configure Redis in Spring Boot
- [ ] Implement basic caching
- [ ] Set up rate limiting infrastructure
- [ ] Configure test environment

### **Phase 2: Core Features (Week 2)**
- [ ] Add match notifications
- [ ] Implement match explanations
- [ ] Add "Recommended For You" feed
- [ ] Show match percentage on listings
- [ ] Add online status tracking

### **Phase 3: Testing (Week 3)**
- [ ] Write unit tests (target: 80%)
- [ ] Write integration tests (target: 70%)
- [ ] Write frontend tests (target: 60%)
- [ ] Set up E2E tests
- [ ] Performance testing

### **Phase 4: Polish (Week 4)**
- [ ] File attachments in messages
- [ ] Notification preferences
- [ ] Performance optimization
- [ ] Documentation
- [ ] Deployment

---

## 🔍 **Testing Strategy**

### **Unit Tests:**
- Service layer: Business logic
- Repository layer: Custom queries
- Utility classes: Helper functions

### **Integration Tests:**
- API endpoints: Full request/response cycle
- Database operations: Transactions, constraints
- External services: Cloudinary, WebSocket

### **E2E Tests:**
- User registration flow
- Listing creation → Application → Lease
- Matching → Chat → Meetup
- Payment processing

---

## 📊 **Success Metrics**

1. **Performance:**
   - API response time: < 200ms (with caching)
   - Database queries: Reduced by 60%
   - Page load time: < 2s

2. **Reliability:**
   - Test coverage: > 75%
   - Rate limit effectiveness: Block 99% of abuse
   - Cache hit rate: > 70%

3. **User Experience:**
   - Match notifications: Real-time delivery
   - Recommendation accuracy: > 80% user satisfaction
   - Profile completion: > 90% users complete profile

