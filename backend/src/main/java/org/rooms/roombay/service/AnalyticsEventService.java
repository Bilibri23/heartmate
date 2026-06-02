package org.rooms.roombay.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rooms.roombay.security.SecurityUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsEventService {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final JdbcTemplate jdbcTemplate;

    public void emit(String eventType) {
        emit(eventType, safeCurrentUserId(), safeCurrentRole(), null, null);
    }

    public void emit(String eventType, UUID userId, String role, UUID listingId, Map<String, ?> metadata) {
        try {
            String metadataJson = metadata == null || metadata.isEmpty() ? null : JSON.writeValueAsString(metadata);
            jdbcTemplate.update("""
                    INSERT INTO analytics_event(event_type, user_id, role, listing_id, metadata)
                    VALUES (?, ?, ?, ?, CAST(? AS jsonb))
                    """, eventType, userId, role, listingId, metadataJson);
        } catch (Exception ex) {
            log.debug("Analytics event skipped for {}: {}", eventType, ex.getMessage());
        }
    }

    public Map<String, Long> countsByTypeSince(LocalDateTime since) {
        return jdbcTemplate.query("""
                SELECT event_type, COUNT(*) AS total
                FROM analytics_event
                WHERE created_at >= ?
                GROUP BY event_type
                """, rs -> {
            java.util.Map<String, Long> counts = new java.util.HashMap<>();
            while (rs.next()) {
                counts.put(rs.getString("event_type"), rs.getLong("total"));
            }
            return counts;
        }, since);
    }

    public long countSince(String eventType, LocalDateTime since) {
        Long count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM analytics_event
                WHERE event_type = ? AND created_at >= ?
                """, Long.class, eventType, since);
        return count != null ? count : 0;
    }

    private static UUID safeCurrentUserId() {
        try {
            return SecurityUtils.isAuthenticated() ? SecurityUtils.getCurrentUserId() : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String safeCurrentRole() {
        try {
            return SecurityUtils.isAuthenticated() ? SecurityUtils.getCurrentUserRole() : null;
        } catch (Exception ignored) {
            return null;
        }
    }
}
