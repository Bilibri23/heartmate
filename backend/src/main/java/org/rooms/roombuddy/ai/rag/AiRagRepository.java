package org.rooms.roombuddy.ai.rag;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.rooms.roombuddy.exception.BadRequestException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

@Repository
@RequiredArgsConstructor
public class AiRagRepository {

    private final JdbcTemplate jdbcTemplate;

    @Value
    @Builder
    public static class DocumentRow {
        UUID id;
        String source;
        String title;
        String checksum;
    }

    @Value
    @Builder
    public static class ChunkRow {
        UUID id;
        UUID documentId;
        int chunkIndex;
        String chunkText;
        String source;
        String title;
    }

    public static String sha256(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] out = md.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(out.length * 2);
            for (byte b : out) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new BadRequestException("Failed to hash document");
        }
    }

    public Optional<DocumentRow> findDocumentBySource(String source) {
        List<DocumentRow> rows = jdbcTemplate.query(
                "SELECT id, source, title, checksum FROM ai_documents WHERE source = ?",
                (rs, i) -> DocumentRow.builder()
                        .id(UUID.fromString(rs.getString("id")))
                        .source(rs.getString("source"))
                        .title(rs.getString("title"))
                        .checksum(rs.getString("checksum"))
                        .build(),
                source
        );
        return rows.stream().findFirst();
    }

    public UUID upsertDocument(String source, String title, String checksum) {
        Optional<DocumentRow> existing = findDocumentBySource(source);
        if (existing.isPresent()) {
            jdbcTemplate.update(
                    "UPDATE ai_documents SET title = ?, checksum = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                    title, checksum, existing.get().getId()
            );
            return existing.get().getId();
        }

        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO ai_documents (id, source, title, checksum) VALUES (?, ?, ?, ?)",
                id, source, title, checksum
        );
        return id;
    }

    public void deleteChunksForDocument(UUID documentId) {
        jdbcTemplate.update("DELETE FROM ai_chunks WHERE document_id = ?", documentId);
    }

    public void insertChunk(UUID documentId, int chunkIndex, String chunkText, List<Double> embedding, Map<String, Object> metadata) {
        try {
            String vectorLiteral = toVectorLiteral(embedding);
            String metadataJson = metadata == null ? null : new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(metadata);

            jdbcTemplate.update(
                    "INSERT INTO ai_chunks (document_id, chunk_index, chunk_text, embedding, metadata) VALUES (?, ?, ?, ?::vector, ?::jsonb)",
                    documentId, chunkIndex, chunkText, vectorLiteral, metadataJson
            );
        } catch (Exception e) {
            throw new BadRequestException("Failed to insert AI chunk: " + e.getMessage());
        }
    }

    public List<ChunkRow> topKSimilar(List<Double> queryEmbedding, int k) {
        try {
            String vectorLiteral = toVectorLiteral(queryEmbedding);

            RowMapper<ChunkRow> mapper = (rs, i) -> ChunkRow.builder()
                    .id(UUID.fromString(rs.getString("id")))
                    .documentId(UUID.fromString(rs.getString("document_id")))
                    .chunkIndex(rs.getInt("chunk_index"))
                    .chunkText(rs.getString("chunk_text"))
                    .source(rs.getString("source"))
                    .title(rs.getString("title"))
                    .build();

            return jdbcTemplate.query(
                    """
                    SELECT c.id, c.document_id, c.chunk_index, c.chunk_text, d.source, d.title
                    FROM ai_chunks c
                    JOIN ai_documents d ON d.id = c.document_id
                    ORDER BY c.embedding <=> ?::vector
                    LIMIT ?
                    """,
                    mapper,
                    vectorLiteral, k
            );
        } catch (Exception e) {
            throw new BadRequestException("Vector search failed: " + e.getMessage());
        }
    }

    private static String toVectorLiteral(List<Double> embedding) {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < embedding.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append(embedding.get(i));
        }
        sb.append(']');
        return sb.toString();
    }
}

