package org.rooms.roombay.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rooms.roombay.dto.response.AiIngestResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiIngestRunService {

    private final JdbcTemplate jdbcTemplate;

    public UUID start() {
        try {
            UUID id = UUID.randomUUID();
            jdbcTemplate.update("INSERT INTO ai_ingest_run(id, status, started_at) VALUES (?, 'RUNNING', ?)", id, LocalDateTime.now());
            return id;
        } catch (Exception ex) {
            log.debug("AI ingest run start not recorded: {}", ex.getMessage());
            return null;
        }
    }

    public void success(UUID id, AiIngestResponse response) {
        if (id == null) {
            return;
        }
        try {
            jdbcTemplate.update("""
                    UPDATE ai_ingest_run
                    SET status = 'SUCCESS',
                        documents_processed = ?,
                        chunks_inserted = ?,
                        graph_entities_written = ?,
                        graph_edges_written = ?,
                        finished_at = ?,
                        error_message = NULL
                    WHERE id = ?
                    """,
                    response.getDocumentsProcessed(),
                    response.getChunksInserted(),
                    response.getGraphEntitiesWritten(),
                    response.getGraphEdgesWritten(),
                    LocalDateTime.now(),
                    id);
        } catch (Exception ex) {
            log.debug("AI ingest run success not recorded: {}", ex.getMessage());
        }
    }

    public void failure(UUID id, Throwable throwable) {
        if (id == null) {
            return;
        }
        try {
            String message = throwable != null ? AppErrorLogService.sanitizeForOpsLog(throwable.getMessage()) : "Unknown AI ingest failure";
            if (message != null && message.length() > 2_000) {
                message = message.substring(0, 2_000);
            }
            jdbcTemplate.update("""
                    UPDATE ai_ingest_run
                    SET status = 'FAILED',
                        finished_at = ?,
                        error_message = ?
                    WHERE id = ?
                    """, LocalDateTime.now(), message, id);
        } catch (Exception ex) {
            log.debug("AI ingest run failure not recorded: {}", ex.getMessage());
        }
    }

    public Map<String, Object> lastSuccessful() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT id, documents_processed AS "documentsProcessed", chunks_inserted AS "chunksInserted",
                       graph_entities_written AS "graphEntitiesWritten", graph_edges_written AS "graphEdgesWritten",
                       started_at AS "startedAt", finished_at AS "finishedAt"
                FROM ai_ingest_run
                WHERE status = 'SUCCESS'
                ORDER BY finished_at DESC NULLS LAST, started_at DESC
                LIMIT 1
                """);
        return rows.isEmpty() ? null : rows.get(0);
    }
}
