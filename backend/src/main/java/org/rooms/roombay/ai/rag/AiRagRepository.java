package org.rooms.roombay.ai.rag;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.rooms.roombay.exception.BadRequestException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

@Repository
@RequiredArgsConstructor
@Slf4j
public class AiRagRepository {

    private static final ObjectMapper JSON = new ObjectMapper();

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

    @Value
    @Builder
    public static class SimilarChunk {
        ChunkRow chunk;
        double distance;
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

    /**
     * Removes GraphRAG entities (and edges) scoped to this document. Call after {@link #deleteChunksForDocument}
     * so chunk_entity rows are already gone.
     */
    public void deleteEntitiesForDocument(UUID documentId) {
        try {
            jdbcTemplate.update("DELETE FROM ai_entities WHERE source_document_id = ?", documentId);
        } catch (Exception e) {
            // Table may not exist on very old DBs
            log.debug("[AI] deleteEntitiesForDocument skipped: {}", e.getMessage());
        }
    }

    public UUID insertChunk(UUID documentId, int chunkIndex, String chunkText, List<Double> embedding, Map<String, Object> metadata) {
        try {
            String vectorLiteral = toVectorLiteral(embedding);
            String metadataJson = metadata == null ? null : JSON.writeValueAsString(metadata);

            return jdbcTemplate.queryForObject(
                    """
                    INSERT INTO ai_chunks (document_id, chunk_index, chunk_text, embedding, metadata)
                    VALUES (?, ?, ?, ?::vector, ?::jsonb)
                    RETURNING id
                    """,
                    UUID.class,
                    documentId, chunkIndex, chunkText, vectorLiteral, metadataJson
            );
        } catch (Exception e) {
            throw new BadRequestException("Failed to insert AI chunk: " + e.getMessage());
        }
    }

    public UUID upsertEntity(String canonicalKey, String entityType, String label, UUID sourceDocumentId, Map<String, Object> metadata) {
        try {
            String metaJson = metadata == null || metadata.isEmpty() ? null : JSON.writeValueAsString(metadata);
            return jdbcTemplate.queryForObject(
                    """
                    INSERT INTO ai_entities (canonical_key, entity_type, label, source_document_id, metadata)
                    VALUES (?, ?, ?, ?, ?::jsonb)
                    ON CONFLICT (canonical_key) DO UPDATE SET
                      label = COALESCE(EXCLUDED.label, ai_entities.label),
                      metadata = COALESCE(EXCLUDED.metadata, ai_entities.metadata)
                    RETURNING id
                    """,
                    UUID.class,
                    canonicalKey,
                    entityType,
                    label,
                    sourceDocumentId,
                    metaJson
            );
        } catch (Exception e) {
            throw new BadRequestException("Failed to upsert AI entity: " + e.getMessage());
        }
    }

    public void linkChunkToEntity(UUID chunkId, UUID entityId, float weight) {
        jdbcTemplate.update(
                """
                INSERT INTO ai_chunk_entities (chunk_id, entity_id, weight) VALUES (?, ?, ?)
                ON CONFLICT (chunk_id, entity_id) DO UPDATE SET weight = EXCLUDED.weight
                """,
                chunkId,
                entityId,
                weight
        );
    }

    public void mergeEdge(UUID srcEntityId, UUID dstEntityId, String relationType, float weight, Map<String, Object> metadata) {
        try {
            String metaJson = metadata == null || metadata.isEmpty() ? null : JSON.writeValueAsString(metadata);
            jdbcTemplate.update(
                    """
                    INSERT INTO ai_edges (src_entity_id, dst_entity_id, relation_type, weight, metadata)
                    VALUES (?, ?, ?, ?, ?::jsonb)
                    ON CONFLICT (src_entity_id, dst_entity_id, relation_type) DO UPDATE SET
                      weight = GREATEST(ai_edges.weight, EXCLUDED.weight),
                      metadata = COALESCE(EXCLUDED.metadata, ai_edges.metadata)
                    """,
                    srcEntityId,
                    dstEntityId,
                    relationType,
                    weight,
                    metaJson
            );
        } catch (Exception e) {
            log.debug("[AI] mergeEdge failed: {}", e.getMessage());
        }
    }

    public List<UUID> findEntityIdsForChunkIds(Collection<UUID> chunkIds) {
        if (chunkIds == null || chunkIds.isEmpty()) {
            return List.of();
        }
        String sql = "SELECT DISTINCT entity_id FROM ai_chunk_entities WHERE chunk_id IN (" +
                String.join(",", Collections.nCopies(chunkIds.size(), "?")) + ")";
        return jdbcTemplate.query(sql, (rs, row) -> UUID.fromString(rs.getString("entity_id")), chunkIds.toArray());
    }

    /**
     * Chunks that share entities with the seed entity set (excluding seed chunks), ranked by overlap count.
     */
    public List<UUID> findNeighborChunkIds(Collection<UUID> seedChunkIds, Collection<UUID> entityIds, int limit) {
        if (entityIds == null || entityIds.isEmpty() || limit <= 0) {
            return List.of();
        }
        List<Object> args = new ArrayList<>(entityIds);
        StringBuilder sql = new StringBuilder(
                "SELECT cce.chunk_id FROM ai_chunk_entities cce WHERE cce.entity_id IN (");
        sql.append(String.join(",", Collections.nCopies(entityIds.size(), "?")));
        sql.append(")");
        if (seedChunkIds != null && !seedChunkIds.isEmpty()) {
            sql.append(" AND cce.chunk_id NOT IN (");
            sql.append(String.join(",", Collections.nCopies(seedChunkIds.size(), "?")));
            sql.append(")");
            args.addAll(seedChunkIds);
        }
        sql.append(" GROUP BY cce.chunk_id ORDER BY COUNT(*) DESC, cce.chunk_id LIMIT ?");
        args.add(limit);
        return jdbcTemplate.query(sql.toString(), (rs, row) -> UUID.fromString(rs.getString("chunk_id")), args.toArray());
    }

    public List<ChunkRow> loadChunksByIds(Collection<UUID> chunkIds) {
        if (chunkIds == null || chunkIds.isEmpty()) {
            return List.of();
        }
        String in = String.join(",", Collections.nCopies(chunkIds.size(), "?"));
        RowMapper<ChunkRow> mapper = (rs, i) -> ChunkRow.builder()
                .id(UUID.fromString(rs.getString("id")))
                .documentId(UUID.fromString(rs.getString("document_id")))
                .chunkIndex(rs.getInt("chunk_index"))
                .chunkText(rs.getString("chunk_text"))
                .source(rs.getString("source"))
                .title(rs.getString("title"))
                .build();
        String sql = "SELECT c.id, c.document_id, c.chunk_index, c.chunk_text, d.source, d.title "
                + "FROM ai_chunks c JOIN ai_documents d ON d.id = c.document_id WHERE c.id IN (" + in + ")";
        return jdbcTemplate.query(sql, mapper, chunkIds.toArray());
    }

    public long countDocuments() {
        try {
            Long n = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ai_documents", Long.class);
            return n != null ? n : 0L;
        } catch (Exception e) {
            return 0L;
        }
    }

    public long countChunks() {
        try {
            Long n = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ai_chunks", Long.class);
            return n != null ? n : 0L;
        } catch (Exception e) {
            return 0L;
        }
    }

    public Map<String, Long> graphStats() {
        Map<String, Long> out = new LinkedHashMap<>();
        try {
            Long entities = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ai_entities", Long.class);
            Long edges = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ai_edges", Long.class);
            Long links = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ai_chunk_entities", Long.class);
            out.put("entityCount", entities != null ? entities : 0L);
            out.put("edgeCount", edges != null ? edges : 0L);
            out.put("chunkEntityLinkCount", links != null ? links : 0L);
        } catch (Exception e) {
            out.put("entityCount", 0L);
            out.put("edgeCount", 0L);
            out.put("chunkEntityLinkCount", 0L);
        }
        return out;
    }

    public List<ChunkRow> topKSimilar(List<Double> queryEmbedding, int k) {
        return topKSimilarWithDistance(queryEmbedding, k).stream().map(SimilarChunk::getChunk).toList();
    }

    public List<SimilarChunk> topKSimilarWithDistance(List<Double> queryEmbedding, int k) {
        try {
            if (queryEmbedding.isEmpty() || k <= 0) {
                return List.of();
            }
            String vectorLiteral = toVectorLiteral(queryEmbedding);
            int queryDims = queryEmbedding.size();
            Integer storedDims = jdbcTemplate.queryForObject(
                    "SELECT COALESCE(MAX(vector_dims(embedding)), 0) FROM ai_chunks",
                    Integer.class
            );
            if (storedDims != null && storedDims > 0 && storedDims != queryDims) {
                return List.of();
            }

            RowMapper<SimilarChunk> mapper = (rs, i) -> SimilarChunk.builder()
                    .chunk(ChunkRow.builder()
                            .id(UUID.fromString(rs.getString("id")))
                            .documentId(UUID.fromString(rs.getString("document_id")))
                            .chunkIndex(rs.getInt("chunk_index"))
                            .chunkText(rs.getString("chunk_text"))
                            .source(rs.getString("source"))
                            .title(rs.getString("title"))
                            .build())
                    .distance(rs.getDouble("dist"))
                    .build();

            return jdbcTemplate.query(
                    """
                    SELECT c.id, c.document_id, c.chunk_index, c.chunk_text, d.source, d.title,
                           (c.embedding <=> ?::vector) AS dist
                    FROM ai_chunks c
                    JOIN ai_documents d ON d.id = c.document_id
                    WHERE vector_dims(c.embedding) = ?
                    ORDER BY dist
                    LIMIT ?
                    """,
                    mapper,
                    vectorLiteral, queryDims, k
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

