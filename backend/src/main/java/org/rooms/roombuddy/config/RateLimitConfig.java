package org.rooms.roombuddy.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple in-memory rate limiting for auth endpoints
 * Prevents brute-force attacks without external dependencies
 */
@Configuration
@Slf4j
public class RateLimitConfig {
    
    // Development-friendly limits - increase for production
    private static final int MAX_REQUESTS_PER_MINUTE = 30;
    private static final int MAX_LOGIN_ATTEMPTS_PER_HOUR = 20;
    
    @Bean
    public FilterRegistrationBean<RateLimitFilter> rateLimitFilter() {
        FilterRegistrationBean<RateLimitFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new RateLimitFilter());
        registrationBean.addUrlPatterns("/api/auth/login", "/api/auth/register", "/api/auth/forgot-password");
        registrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registrationBean;
    }
    
    public static class RateLimitFilter implements Filter {
        
        private final Map<String, RateLimitEntry> requestCounts = new ConcurrentHashMap<>();
        
        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                throws IOException, ServletException {
            
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            
            String clientKey = getClientKey(httpRequest);
            String endpoint = httpRequest.getRequestURI();
            
            RateLimitEntry entry = requestCounts.computeIfAbsent(clientKey, k -> new RateLimitEntry());
            
            // Clean up old entries periodically
            cleanupOldEntries();
            
            // Check rate limit based on endpoint
            int maxRequests = endpoint.contains("login") ? MAX_LOGIN_ATTEMPTS_PER_HOUR : MAX_REQUESTS_PER_MINUTE;
            long windowMs = endpoint.contains("login") ? 3600000L : 60000L; // 1 hour for login, 1 min for others
            
            if (!entry.allowRequest(maxRequests, windowMs)) {
                log.warn("Rate limit exceeded for client: {} on endpoint: {}", clientKey, endpoint);
                httpResponse.setStatus(429);
                httpResponse.setContentType("application/json");
                httpResponse.getWriter().write("{\"error\":\"Too many requests\",\"message\":\"Please try again later\",\"retryAfterSeconds\":" + (windowMs / 1000) + "}");
                return;
            }
            
            chain.doFilter(request, response);
        }
        
        private String getClientKey(HttpServletRequest request) {
            // Try to get real IP behind proxy
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                return xForwardedFor.split(",")[0].trim();
            }
            return request.getRemoteAddr();
        }
        
        private void cleanupOldEntries() {
            long now = System.currentTimeMillis();
            requestCounts.entrySet().removeIf(entry -> 
                now - entry.getValue().getLastRequestTime() > 3600000L); // Remove entries older than 1 hour
        }
    }
    
    public static class RateLimitEntry {
        private final AtomicInteger count = new AtomicInteger(0);
        private volatile long windowStart = System.currentTimeMillis();
        private volatile long lastRequestTime = System.currentTimeMillis();
        
        public synchronized boolean allowRequest(int maxRequests, long windowMs) {
            long now = System.currentTimeMillis();
            lastRequestTime = now;
            
            // Reset window if expired
            if (now - windowStart > windowMs) {
                windowStart = now;
                count.set(1);
                return true;
            }
            
            // Check if under limit
            if (count.incrementAndGet() <= maxRequests) {
                return true;
            }
            
            return false;
        }
        
        public long getLastRequestTime() {
            return lastRequestTime;
        }
    }
}
