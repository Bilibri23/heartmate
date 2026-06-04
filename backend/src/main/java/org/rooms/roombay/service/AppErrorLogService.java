package org.rooms.roombay.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rooms.roombay.security.SecurityUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppErrorLogService {

    private static final int MAX_MESSAGE_LENGTH = 2_000;
    private static final int MAX_STACK_LENGTH = 6_000;
    private static final Pattern BEARER = Pattern.compile("(?i)bearer\\s+[a-z0-9._~+/=-]+");
    private static final Pattern JWT = Pattern.compile("(?i)\\beyJ[a-z0-9_-]+\\.[a-z0-9_-]+\\.[a-z0-9_-]+\\b");
    private static final Pattern SECRET_PAIR = Pattern.compile("(?i)(authorization|cookie|set-cookie|jwt|token|refresh[_-]?token|reset[_-]?token|verification[_-]?token|password|secret|payment[_-]?secret|api[_-]?key|cloudinary[_-]?secret|oauth[_-]?secret|client[_-]?secret|database[_-]?url)\\s*[:=]\\s*[^,;\\s}\\]]+");
    private static final Pattern DATABASE_URL = Pattern.compile("(?i)(jdbc:postgresql://|postgres(?:ql)?://)[^\\s,;\\]}]+");
    private static final Pattern SENSITIVE_URL = Pattern.compile("(?i)https?://[^\\s,;\\]}]*(government|identity|verification|document|passport|national[_-]?id|selfie|payment-proof|payment_proof|cloudinary)[^\\s,;\\]}]*");

    private final JdbcTemplate jdbcTemplate;

    public void log(String level, String source, String message, String path, Throwable throwable) {
        log(level, source, message, path, safeCurrentUserId(), safeCurrentRole(), throwable);
    }

    public void log(String level, String source, String message, String path, UUID userId, String role, Throwable throwable) {
        try {
            jdbcTemplate.update("""
                    INSERT INTO app_error_log(level, source, message, path, user_id, role, stack_trace)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """,
                    safe(level, 20),
                    safe(source, 80),
                    safe(message, MAX_MESSAGE_LENGTH),
                    safe(path, 500),
                    userId,
                    safe(role, 50),
                    throwable != null ? stackTrace(throwable) : null);
        } catch (Exception ex) {
            log.warn("Failed to write app_error_log: {}", ex.getMessage());
        }
    }

    public List<Map<String, Object>> recent(int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 200);
        return jdbcTemplate.queryForList("""
                SELECT id, level, source, message, path, user_id AS "userId", role, stack_trace AS "stackTrace", created_at AS "createdAt"
                FROM app_error_log
                ORDER BY created_at DESC
                LIMIT ?
                """, safeLimit);
    }

    public long countRecentServerErrors(LocalDateTime since) {
        Long count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM app_error_log
                WHERE created_at >= ?
                  AND (level IN ('ERROR', 'CRITICAL') OR message ILIKE '%500%' OR message ILIKE '%503%')
                """, Long.class, since);
        return count != null ? count : 0;
    }

    public long countBySourceSince(String source, LocalDateTime since) {
        Long count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM app_error_log
                WHERE source = ? AND created_at >= ?
                """, Long.class, source, since);
        return count != null ? count : 0;
    }

    public int deleteOlderThan(LocalDateTime cutoff) {
        try {
            return jdbcTemplate.update("DELETE FROM app_error_log WHERE created_at < ?", cutoff);
        } catch (Exception ex) {
            log.warn("Failed to delete old app_error_log rows: {}", ex.getMessage());
            return 0;
        }
    }

    static String sanitizeForOpsLog(String value) {
        if (value == null) {
            return null;
        }
        String sanitized = BEARER.matcher(value).replaceAll("Bearer [REDACTED]");
        sanitized = JWT.matcher(sanitized).replaceAll("[JWT_REDACTED]");
        sanitized = SECRET_PAIR.matcher(sanitized).replaceAll("$1=[REDACTED]");
        sanitized = DATABASE_URL.matcher(sanitized).replaceAll("[DATABASE_URL_REDACTED]");
        sanitized = SENSITIVE_URL.matcher(sanitized).replaceAll("[SENSITIVE_URL_REDACTED]");
        return sanitized;
    }

    private static String safe(String value, int maxLength) {
        String sanitized = sanitizeForOpsLog(value);
        if (sanitized == null) {
            return null;
        }
        if (sanitized.length() <= maxLength) {
            return sanitized;
        }
        return sanitized.substring(0, maxLength);
    }

    private static String stackTrace(Throwable throwable) {
        StringWriter sw = new StringWriter();
        throwable.printStackTrace(new PrintWriter(sw));
        return safe(sw.toString(), MAX_STACK_LENGTH);
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
