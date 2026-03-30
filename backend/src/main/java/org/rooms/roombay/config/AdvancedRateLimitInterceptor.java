package org.rooms.roombay.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Advanced rate limiting interceptor
 * Supports both user-based and IP-based rate limiting
 */
@Component
@Slf4j
public class AdvancedRateLimitInterceptor implements HandlerInterceptor {
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    // Constructor with optional RedisTemplate
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    public AdvancedRateLimitInterceptor(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    
    // Default constructor for when RedisTemplate is not available
    public AdvancedRateLimitInterceptor() {
        this.redisTemplate = null;
    }
    
    @Value("${ratelimit.user.requests-per-minute:100}")
    private int userLimitPerMinute;
    
    @Value("${ratelimit.ip.requests-per-minute:50}")
    private int ipLimitPerMinute;
    
    @Value("${ratelimit.login.requests-per-minute:10}")
    private int loginLimitPerMinute;
    
    @Value("${ratelimit.register.requests-per-minute:5}")
    private int registerLimitPerMinute;
    
    @Override
    public boolean preHandle(@org.springframework.lang.NonNull HttpServletRequest request, 
                            @org.springframework.lang.NonNull HttpServletResponse response, 
                            @org.springframework.lang.NonNull Object handler) {
        // Skip rate limiting for actuator endpoints
        String path = request.getRequestURI();
        if (path.startsWith("/actuator") || path.startsWith("/swagger") || path.startsWith("/api-docs")) {
            return true;
        }
        
        String userId = getUserId(request);
        String ip = getClientIp(request);
        String endpoint = path;
        
        // Endpoint-specific limits
        int userLimit = getUserLimitForEndpoint(endpoint);
        int ipLimit = getIpLimitForEndpoint(endpoint);
        
        // Check user-based rate limit (if authenticated)
        if (userId != null) {
            String userKey = "ratelimit:user:" + userId + ":" + endpoint;
            if (!checkRateLimit(userKey, userLimit, 60)) {
                log.warn("User rate limit exceeded: userId={}, endpoint={}", userId, endpoint);
                sendRateLimitResponse(response, "User rate limit exceeded", userLimit);
                return false;
            }
        }
        
        // Check IP-based rate limit
        String ipKey = "ratelimit:ip:" + ip + ":" + endpoint;
        if (!checkRateLimit(ipKey, ipLimit, 60)) {
            log.warn("IP rate limit exceeded: ip={}, endpoint={}", ip, endpoint);
            sendRateLimitResponse(response, "IP rate limit exceeded", ipLimit);
            return false;
        }
        
        return true;
    }
    
    private boolean checkRateLimit(String key, int limit, long windowSeconds) {
        try {
            // If Redis is not available, skip rate limiting (fail open)
            if (redisTemplate == null) {
                return true;
            }
            
            // Check if Redis connection is available
            try {
                redisTemplate.hasKey("test");
            } catch (Exception e) {
                // Redis not available, skip rate limiting silently
                return true;
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
            
            return true;
        } catch (Exception e) {
            // Only log at debug level to avoid noise when Redis is intentionally not available
            if (log.isDebugEnabled()) {
                log.debug("Rate limiting unavailable (allowing request): {}", e.getMessage());
            }
            // Allow request if Redis fails (fail open)
            return true;
        }
    }
    
    private int getUserLimitForEndpoint(String endpoint) {
        if (endpoint.contains("/auth/login")) {
            return loginLimitPerMinute;
        }
        if (endpoint.contains("/auth/register")) {
            return registerLimitPerMinute;
        }
        return userLimitPerMinute;
    }
    
    private int getIpLimitForEndpoint(String endpoint) {
        if (endpoint.contains("/auth/login")) {
            return loginLimitPerMinute / 2;
        }
        if (endpoint.contains("/auth/register")) {
            return registerLimitPerMinute / 2;
        }
        return ipLimitPerMinute;
    }
    
    private String getUserId(HttpServletRequest request) {
        try {
            // Try to get from security context
            Object principal = request.getUserPrincipal();
            if (principal != null) {
                return principal.getClass().getName();
            }
            // Try to get from header (if set by JWT filter)
            String userId = request.getHeader("X-User-Id");
            return userId;
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
}

