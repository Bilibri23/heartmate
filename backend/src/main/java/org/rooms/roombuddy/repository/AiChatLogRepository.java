package org.rooms.roombay.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.rooms.roombay.exception.BadRequestException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
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
}

