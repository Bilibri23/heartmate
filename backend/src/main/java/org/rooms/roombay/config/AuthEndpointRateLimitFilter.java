package org.rooms.roombay.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-memory sliding-window limits for sensitive auth endpoints (no Redis required).
 * Stricter limits apply to login to slow brute-force and bot traffic.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class AuthEndpointRateLimitFilter extends OncePerRequestFilter {

    @Value("${ratelimit.auth.login-max-per-hour:30}")
    private int loginMaxPerHour;

    @Value("${ratelimit.auth.register-max-per-minute:8}")
    private int registerMaxPerMinute;

    @Value("${ratelimit.auth.refresh-max-per-minute:60}")
    private int refreshMaxPerMinute;

    @Value("${ratelimit.auth.forgot-password-max-per-hour:10}")
    private int forgotPasswordMaxPerHour;

    @Value("${ratelimit.auth.reset-password-max-per-hour:10}")
    private int resetPasswordMaxPerHour;

    private final Map<String, RateLimitEntry> requestCounts = new ConcurrentHashMap<>();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return !(uri.startsWith("/api/auth/login")
                || uri.startsWith("/api/auth/register")
                || uri.startsWith("/api/auth/refresh")
                || uri.startsWith("/api/auth/forgot-password")
                || uri.startsWith("/api/auth/reset-password"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String clientKey = getClientKey(request);
        String endpoint = request.getRequestURI();

        int max;
        long windowMs;
        if (endpoint.contains("login")) {
            max = loginMaxPerHour;
            windowMs = 3_600_000L;
        } else if (endpoint.contains("register")) {
            max = registerMaxPerMinute;
            windowMs = 60_000L;
        } else if (endpoint.contains("refresh")) {
            max = refreshMaxPerMinute;
            windowMs = 60_000L;
        } else if (endpoint.contains("reset-password")) {
            max = resetPasswordMaxPerHour;
            windowMs = 3_600_000L;
        } else {
            max = forgotPasswordMaxPerHour;
            windowMs = 3_600_000L;
        }

        String key = clientKey + ":" + endpoint.split("\\?")[0];
        RateLimitEntry entry = requestCounts.computeIfAbsent(key, k -> new RateLimitEntry());
        cleanupOldEntries();

        if (!entry.allowRequest(max, windowMs)) {
            log.warn("Auth rate limit exceeded for client on {}", endpoint);
            response.setStatus(429);
            response.setContentType("application/json");
            response.setHeader("Retry-After", String.valueOf(windowMs / 1000));
            response.getWriter().write("{\"error\":\"Too many requests\",\"message\":\"Please try again later\",\"retryAfterSeconds\":"
                    + (windowMs / 1000) + "}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String getClientKey(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void cleanupOldEntries() {
        if (requestCounts.size() > 50_000) {
            long now = System.currentTimeMillis();
            requestCounts.entrySet().removeIf(e -> now - e.getValue().getLastRequestTime() > 3_600_000L);
        }
    }

    private static final class RateLimitEntry {
        private final AtomicInteger count = new AtomicInteger(0);
        private volatile long windowStart = System.currentTimeMillis();
        private volatile long lastRequestTime = System.currentTimeMillis();

        synchronized boolean allowRequest(int maxRequests, long windowMs) {
            long now = System.currentTimeMillis();
            lastRequestTime = now;
            if (now - windowStart > windowMs) {
                windowStart = now;
                count.set(1);
                return true;
            }
            return count.incrementAndGet() <= maxRequests;
        }

        long getLastRequestTime() {
            return lastRequestTime;
        }
    }
}
