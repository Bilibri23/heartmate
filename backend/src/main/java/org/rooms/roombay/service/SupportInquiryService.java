package org.rooms.roombay.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rooms.roombay.dto.request.SupportInquiryRequest;
import org.rooms.roombay.dto.response.SupportInquiryResponse;
import org.rooms.roombay.entity.User;
import org.rooms.roombay.exception.ResourceNotFoundException;
import org.rooms.roombay.repository.UserRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SupportInquiryService {

    private final JdbcTemplate jdbcTemplate;
    private final UserRepository userRepository;

    @Transactional
    public void submit(UUID userId, SupportInquiryRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        UUID id = UUID.randomUUID();
        String category = request.getCategory() != null ? request.getCategory().trim() : null;
        if (category != null && category.length() > 64) {
            category = category.substring(0, 64);
        }
        jdbcTemplate.update(
                """
                INSERT INTO support_inquiries (id, user_id, email, category, subject, message)
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                id,
                userId,
                user.getEmail(),
                category,
                request.getSubject().trim(),
                request.getMessage().trim()
        );
        log.info("[Support] inquiry {} from user {} category={}", id, userId, category);
    }

    /** Paginated list for admin (Help and Support inbox). */
    public Map<String, Object> listForAdmin(int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        int safePage = Math.max(page, 0);
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM support_inquiries",
                Long.class
        );
        long totalE = total != null ? total : 0L;
        int offset = safePage * safeSize;

        List<SupportInquiryResponse> content = jdbcTemplate.query(
                """
                SELECT id, user_id, email, category, subject, message, created_at
                FROM support_inquiries
                ORDER BY created_at DESC
                LIMIT ? OFFSET ?
                """,
                (rs, i) -> SupportInquiryResponse.builder()
                        .id(UUID.fromString(rs.getString("id")))
                        .userId(UUID.fromString(rs.getString("user_id")))
                        .email(rs.getString("email"))
                        .category(rs.getString("category"))
                        .subject(rs.getString("subject"))
                        .message(rs.getString("message"))
                        .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
                        .build(),
                safeSize,
                offset
        );

        int totalPages = safeSize == 0 ? 0 : (int) Math.ceil((double) totalE / safeSize);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("content", content);
        body.put("totalElements", totalE);
        body.put("totalPages", totalPages);
        body.put("number", safePage);
        body.put("size", safeSize);
        body.put("last", safePage >= totalPages - 1 || totalPages == 0);
        body.put("first", safePage == 0);
        return body;
    }
}
