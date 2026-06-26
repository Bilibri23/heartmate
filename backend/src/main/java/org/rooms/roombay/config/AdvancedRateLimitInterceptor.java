package org.rooms.roombay.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Advanced rate limiting interceptor
 * Supports both user-based and IP-based rate limiting
 */
@Component
@Slf4j
public class AdvancedRateLimitInterceptor implements HandlerInterceptor {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final Map<String, LocalRateLimitEntry> localCounters = new ConcurrentHashMap<>();
    private volatile RateLimitMode currentMode = RateLimitMode.LOCAL;
    
    // Constructor with optional RedisTemplate
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    public AdvancedRateLimitInterceptor(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.currentMode = redisTemplate != null ? RateLimitMode.REDIS : RateLimitMode.LOCAL;
    }
    
    // Default constructor for when RedisTemplate is not available
    public AdvancedRateLimitInterceptor() {
        this.redisTemplate = null;
    }
    
    @Value("${ratelimit.user.requests-per-minute:100}")
    private int userLimitPerMinute;
    
    @Value("${ratelimit.ip.requests-per-minute:50}")
    private int ipLimitPerMinute;

    @Value("${ratelimit.redis.required:false}")
    private boolean redisRequired;

    @Value("${spring.profiles.active:}")
    private String activeProfiles;
    
    @Value("${ratelimit.login.requests-per-minute:10}")
    private int loginLimitPerMinute;
    
    @Value("${ratelimit.register.requests-per-minute:5}")
    private int registerLimitPerMinute;

    @Value("${ratelimit.ai.chat.requests-per-minute:20}")
    private int aiChatLimitPerMinute;

    @Value("${ratelimit.support.requests-per-minute:10}")
    private int supportLimitPerMinute;

    @Value("${ratelimit.messaging.requests-per-minute:60}")
    private int messagingLimitPerMinute;

    @Value("${ratelimit.upload.requests-per-minute:20}")
    private int uploadLimitPerMinute;

    @Value("${ratelimit.listing-create.requests-per-minute:10}")
    private int listingCreateLimitPerMinute;

    @Value("${ratelimit.admin.requests-per-minute:1000}")
    private int adminLimitPerMinute;

    @Value("${ratelimit.notifications.requests-per-minute:300}")
    private int notificationsLimitPerMinute;
    
    @Override
    public boolean preHandle(@org.springframework.lang.NonNull HttpServletRequest request, 
                            @org.springframework.lang.NonNull HttpServletResponse response, 
                            @org.springframework.lang.NonNull Object handler) {
        // Skip rate limiting for actuator endpoints
        String path = request.getRequestURI();
        if (path.startsWith("/actuator") || path.startsWith("/swagger") || path.startsWith("/api-docs")) {
            return true;
        }

        if (isAdminUser()) {
            return true;
        }
        
        String userId = getUserId(request);
        String ip = getClientIp(request);
        String endpoint = path;
        String rateLimitKey = request.getMethod() + ":" + endpoint;
        
        // Endpoint-specific limits
        int userLimit = getUserLimitForEndpoint(endpoint, request.getMethod());
        int ipLimit = getIpLimitForEndpoint(endpoint, request.getMethod());
        
        // Check user-based rate limit (if authenticated)
        if (userId != null) {
            String userKey = "ratelimit:user:" + userId + ":" + rateLimitKey;
            if (!checkRateLimit(userKey, userLimit, 60)) {
                log.warn("User rate limit exceeded: userId={}, endpoint={}", userId, endpoint);
                sendRateLimitResponse(response, "User rate limit exceeded", userLimit);
                return false;
            }
        }
        
        // Check IP-based rate limit
        String ipKey = "ratelimit:ip:" + ip + ":" + rateLimitKey;
        if (!checkRateLimit(ipKey, ipLimit, 60)) {
            log.warn("IP rate limit exceeded: ip={}, endpoint={}", ip, endpoint);
            sendRateLimitResponse(response, "IP rate limit exceeded", ipLimit);
            return false;
        }
        
        return true;
    }
    
    private boolean checkRateLimit(String key, int limit, long windowSeconds) {
        try {
            if (redisTemplate == null) {
                if (isRedisRequired()) {
                    currentMode = RateLimitMode.UNAVAILABLE;
                    return false;
                }
                currentMode = RateLimitMode.LOCAL;
                return checkLocalRateLimit(key, limit, windowSeconds);
            }
            
            // Check if Redis connection is available
            try {
                redisTemplate.hasKey("test");
            } catch (Exception e) {
                if (isRedisRequired()) {
                    currentMode = RateLimitMode.UNAVAILABLE;
                    return false;
                }
                currentMode = RateLimitMode.LOCAL;
                return checkLocalRateLimit(key, limit, windowSeconds);
            }
            
            String countStr = (String) redisTemplate.opsForValue().get(key);
            int count = countStr != null ? Integer.parseInt(countStr) : 0;
            
            if (count >= limit) {
                return false;
            }
            
            // Increment counter
            redisTemplate.opsForValue().increment(key);
            // Set expiration if this is the first request
            if (count == 0) {
                redisTemplate.expire(key, Duration.ofSeconds(windowSeconds));
            }
            
            currentMode = RateLimitMode.REDIS;
            return true;
        } catch (Exception e) {
            if (isRedisRequired()) {
                currentMode = RateLimitMode.UNAVAILABLE;
                return false;
            }
            // Only log at debug level to avoid noise when Redis is intentionally not available
            if (log.isDebugEnabled()) {
                log.debug("Redis rate limiting unavailable; using local limiter: {}", e.getMessage());
            }
            currentMode = RateLimitMode.LOCAL;
            return checkLocalRateLimit(key, limit, windowSeconds);
        }
    }

    public RateLimitMode getCurrentMode() {
        if (redisTemplate == null && !isRedisRequired()) {
            return RateLimitMode.LOCAL;
        }
        return currentMode;
    }

    private boolean isRedisRequired() {
        return redisRequired || (activeProfiles != null && java.util.Arrays.stream(activeProfiles.split(","))
                .map(String::trim)
                .anyMatch("prod"::equalsIgnoreCase) && redisRequired);
    }

    private boolean checkLocalRateLimit(String key, int limit, long windowSeconds) {
        cleanupLocalCounters();
        LocalRateLimitEntry entry = localCounters.computeIfAbsent(key, ignored -> new LocalRateLimitEntry());
        return entry.allow(limit, windowSeconds * 1000L);
    }

    private void cleanupLocalCounters() {
        if (localCounters.size() < 50_000) {
            return;
        }
        long cutoff = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(10);
        localCounters.entrySet().removeIf(e -> e.getValue().lastSeen < cutoff);
    }
    
    private int getUserLimitForEndpoint(String endpoint, String method) {
        if (endpoint.contains("/auth/login")) {
            return loginLimitPerMinute;
        }
        if (endpoint.contains("/auth/register")) {
            return registerLimitPerMinute;
        }
        if (endpoint.contains("/ai/chat")) {
            return aiChatLimitPerMinute;
        }
        if (endpoint.contains("/support")) {
            return supportLimitPerMinute;
        }
        if (endpoint.contains("/messages")) {
            return messagingLimitPerMinute;
        }
        if (endpoint.contains("/upload")) {
            return uploadLimitPerMinute;
        }
        if (isListingCreateEndpoint(endpoint, method)) {
            return listingCreateLimitPerMinute;
        }
        if (endpoint.startsWith("/api/admin")) {
            return adminLimitPerMinute;
        }
        if (endpoint.contains("/notifications/unread-count")) {
            return notificationsLimitPerMinute;
        }
        return userLimitPerMinute;
    }
    
    private int getIpLimitForEndpoint(String endpoint, String method) {
        if (endpoint.contains("/auth/login")) {
            return loginLimitPerMinute / 2;
        }
        if (endpoint.contains("/auth/register")) {
            return registerLimitPerMinute / 2;
        }
        if (endpoint.contains("/ai/chat")) {
            return Math.max(aiChatLimitPerMinute / 2, 1);
        }
        if (endpoint.contains("/support")) {
            return Math.max(supportLimitPerMinute / 2, 1);
        }
        if (endpoint.contains("/messages")) {
            return messagingLimitPerMinute;
        }
        if (endpoint.contains("/upload")) {
            return Math.max(uploadLimitPerMinute / 2, 1);
        }
        if (isListingCreateEndpoint(endpoint, method)) {
            return Math.max(listingCreateLimitPerMinute / 2, 1);
        }
        if (endpoint.startsWith("/api/admin")) {
            return adminLimitPerMinute;
        }
        if (endpoint.contains("/notifications/unread-count")) {
            return notificationsLimitPerMinute;
        }
        return ipLimitPerMinute;
    }

    private static boolean isAdminUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getAuthorities() == null) {
            return false;
        }
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
    }

    private static boolean isListingCreateEndpoint(String endpoint, String method) {
        return "POST".equalsIgnoreCase(method)
                && ("/api/listings".equals(endpoint) || endpoint.endsWith("/api/listings"));
    }
    
    private String getUserId(HttpServletRequest request) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof UUID uid) {
                return uid.toString();
            }
            if (auth != null && auth.getPrincipal() instanceof String s) {
                try {
                    UUID.fromString(s);
                    return s;
                } catch (IllegalArgumentException ignored) {
                    // fall through
                }
            }
            return request.getHeader("X-User-Id");
        } catch (Exception e) {
            return null;
        }
    }
    
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }
    
    private void sendRateLimitResponse(HttpServletResponse response, String message, int limit) {
        try {
            response.setStatus(429);
            response.setContentType("application/json");
            response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
            response.setHeader("X-RateLimit-Remaining", "0");
            response.setHeader("Retry-After", "60");
            response.getWriter().write(String.format(
                "{\"error\":\"Too Many Requests\",\"message\":\"%s\",\"retryAfterSeconds\":60}",
                message
            ));
        } catch (Exception e) {
            log.error("Error sending rate limit response: {}", e.getMessage());
        }
    }

    private static final class LocalRateLimitEntry {
        private final AtomicInteger count = new AtomicInteger();
        private volatile long windowStart = System.currentTimeMillis();
        private volatile long lastSeen = System.currentTimeMillis();

        synchronized boolean allow(int limit, long windowMs) {
            long now = System.currentTimeMillis();
            lastSeen = now;
            if (now - windowStart >= windowMs) {
                windowStart = now;
                count.set(1);
                return true;
            }
            return count.incrementAndGet() <= limit;
        }
    }

    public enum RateLimitMode {
        REDIS,
        LOCAL,
        UNAVAILABLE
    }
}
