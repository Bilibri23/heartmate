package org.rooms.roombay.config;

import org.rooms.roombay.exception.RateLimitExceededException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-memory per-user limit for {@code POST /api/ai/chat} (no Redis required).
 */
@Component
public class AiChatRateLimiter {

    private static final long WINDOW_MS = 60_000L;

    @Value("${ratelimit.ai.chat.requests-per-minute:20}")
    private int maxPerMinute;

    private final Map<UUID, Window> windows = new ConcurrentHashMap<>();

    public void checkAllowed(UUID userId) {
        if (maxPerMinute <= 0 || userId == null) {
            return;
        }
        Window w = windows.computeIfAbsent(userId, u -> new Window());
        if (!w.allow(maxPerMinute)) {
            throw new RateLimitExceededException(
                    "AI assistant rate limit exceeded. Try again in a minute.",
                    (int) (WINDOW_MS / 1000));
        }
        if (windows.size() > 100_000) {
            windows.clear();
        }
    }

    private static final class Window {
        private final AtomicInteger count = new AtomicInteger(0);
        private volatile long windowStart = System.currentTimeMillis();

        synchronized boolean allow(int max) {
            long now = System.currentTimeMillis();
            if (now - windowStart > WINDOW_MS) {
                windowStart = now;
                count.set(1);
                return true;
            }
            return count.incrementAndGet() <= max;
        }
    }
}
