package org.rooms.roombay.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.rooms.roombay.exception.BadRequestException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class AiChatLogRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void insert(UUID userId, String persona, String userMessage, String assistantAnswer, List<String> citationChunkIds) {
        try {
            String citationsJson = objectMapper.writeValueAsString(citationChunkIds == null ? List.of() : citationChunkIds);
            jdbcTemplate.update(
                    "INSERT INTO ai_chat_logs (user_id, persona, user_message, assistant_answer, citation_chunk_ids) VALUES (?, ?, ?, ?, ?::jsonb)",
                    userId, persona, userMessage, assistantAnswer, citationsJson
            );
        } catch (Exception e) {
            throw new BadRequestException("Failed to persist AI chat log: " + e.getMessage());
        }
    }

    public List<Map<String, Object>> listRecent(int limit) {
        int cap = Math.max(1, Math.min(limit, 500));
        return jdbcTemplate.queryForList(
                """
                SELECT id,
                       user_id,
                       persona,
                       user_message,
                       assistant_answer,
                       COALESCE(jsonb_array_length(citation_chunk_ids), 0) AS citation_count,
                       created_at
                FROM ai_chat_logs
                ORDER BY created_at DESC
                LIMIT ?
                """,
                cap
        );
    }

    public Map<String, Object> getCoverageStats() {
        Map<String, Object> totals = jdbcTemplate.queryForMap(
                """
                SELECT COUNT(*) AS total_questions,
                       COUNT(*) FILTER (
                         WHERE COALESCE(jsonb_array_length(COALESCE(citation_chunk_ids, '[]'::jsonb)), 0) > 0
                       ) AS grounded_questions,
                       COUNT(*) FILTER (
                         WHERE COALESCE(jsonb_array_length(COALESCE(citation_chunk_ids, '[]'::jsonb)), 0) = 0
                       ) AS ungrounded_questions
                FROM ai_chat_logs
                """
        );

        List<Map<String, Object>> byPersona = jdbcTemplate.queryForList(
                """
                SELECT persona,
                       COUNT(*) AS question_count,
                       COUNT(*) FILTER (
                         WHERE COALESCE(jsonb_array_length(COALESCE(citation_chunk_ids, '[]'::jsonb)), 0) > 0
                       ) AS grounded_count
                FROM ai_chat_logs
                GROUP BY persona
                ORDER BY question_count DESC, persona
                """
        );

        List<Map<String, Object>> recentUngrounded = jdbcTemplate.queryForList(
                """
                SELECT persona,
                       user_message,
                       created_at
                FROM ai_chat_logs
                WHERE COALESCE(jsonb_array_length(COALESCE(citation_chunk_ids, '[]'::jsonb)), 0) = 0
                ORDER BY created_at DESC
                LIMIT 10
                """
        );

        return Map.of(
                "totals", totals,
                "byPersona", byPersona,
                "recentUngrounded", recentUngrounded
        );
    }
}
