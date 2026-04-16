package org.rooms.roombay.ai.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Bootstraps pgvector + RAG tables at runtime.
 * Flyway is disabled in dev in this repo; this ensures the assistant works locally.
 */
@Component
@Slf4j
public class AiRagSchemaInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;
    private final int embeddingDimensions;

    public AiRagSchemaInitializer(
            JdbcTemplate jdbcTemplate,
            @Value("${roombay.ai.embedding-dimensions:768}") int embeddingDimensions) {
        this.jdbcTemplate = jdbcTemplate;
        this.embeddingDimensions = embeddingDimensions;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS vector");
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS ai_documents (
                    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                    source TEXT NOT NULL,
                    title TEXT,
                    checksum TEXT,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """);
            ensureAiChunksTable(embeddingDimensions);
            ensureGraphRagTables();
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS ai_chat_logs (
                    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                    user_id UUID NOT NULL,
                    persona TEXT NOT NULL,
                    user_message TEXT NOT NULL,
                    assistant_answer TEXT NOT NULL,
                    citation_chunk_ids jsonb,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """);
            log.info("[AI] RAG schema ready (embedding dimensions={})", embeddingDimensions);
        } catch (Exception e) {
            // Don't block app startup if pgvector isn't installed yet.
            log.warn("[AI] Failed to initialize RAG schema (pgvector missing?): {}", e.getMessage());
        }
    }

    private void ensureAiChunksTable(int dims) {
        Boolean exists = jdbcTemplate.queryForObject(
                """
                SELECT EXISTS (
                    SELECT 1 FROM information_schema.tables
                    WHERE table_schema = 'public' AND table_name = 'ai_chunks'
                )
                """,
                Boolean.class
        );
        if (!Boolean.TRUE.equals(exists)) {
            jdbcTemplate.execute(
                    "CREATE TABLE ai_chunks ("
                            + " id UUID PRIMARY KEY DEFAULT gen_random_uuid(),"
                            + " document_id UUID NOT NULL REFERENCES ai_documents(id) ON DELETE CASCADE,"
                            + " chunk_index INTEGER NOT NULL,"
                            + " chunk_text TEXT NOT NULL,"
                            + " embedding vector(" + dims + ") NOT NULL,"
                            + " metadata jsonb,"
                            + " created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
                            + ")"
            );
            createChunkIndexes();
            return;
        }

        String colType = jdbcTemplate.query(
                """
                SELECT format_type(a.atttypid, a.atttypmod) AS ft
                FROM pg_attribute a
                JOIN pg_class c ON a.attrelid = c.oid
                WHERE c.relname = 'ai_chunks' AND a.attname = 'embedding'
                  AND a.attnum > 0 AND NOT a.attisdropped
                """,
                rs -> rs.next() ? rs.getString("ft") : null
        );
        String expected = "vector(" + dims + ")";
        if (expected.equals(colType)) {
            createChunkIndexes();
            return;
        }

        log.warn(
                "[AI] ai_chunks.embedding is {} but roombay.ai.embedding-dimensions={}; clearing chunks and altering column",
                colType,
                dims
        );
        jdbcTemplate.execute("DROP INDEX IF EXISTS idx_ai_chunks_embedding_cos");
        jdbcTemplate.update("DELETE FROM ai_chunks");
        jdbcTemplate.execute("ALTER TABLE ai_chunks ALTER COLUMN embedding TYPE vector(" + dims + ")");
        createChunkIndexes();
    }

    private void createChunkIndexes() {
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_ai_chunks_document_id ON ai_chunks(document_id)");
        jdbcTemplate.execute(
                "CREATE INDEX IF NOT EXISTS idx_ai_chunks_embedding_cos ON ai_chunks USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100)"
        );
    }

    /**
     * GraphRAG tables (see also V37__ai_graph_rag_tables.sql when Flyway is enabled).
     */
    private void ensureGraphRagTables() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS ai_entities (
                    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                    canonical_key TEXT NOT NULL,
                    entity_type TEXT NOT NULL,
                    label TEXT,
                    source_document_id UUID NOT NULL REFERENCES ai_documents(id) ON DELETE CASCADE,
                    metadata JSONB,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    CONSTRAINT uq_ai_entities_canonical UNIQUE (canonical_key)
                )
                """);
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_ai_entities_document ON ai_entities(source_document_id)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_ai_entities_type ON ai_entities(entity_type)");

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS ai_chunk_entities (
                    chunk_id UUID NOT NULL REFERENCES ai_chunks(id) ON DELETE CASCADE,
                    entity_id UUID NOT NULL REFERENCES ai_entities(id) ON DELETE CASCADE,
                    weight REAL NOT NULL DEFAULT 1.0,
                    PRIMARY KEY (chunk_id, entity_id)
                )
                """);
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_ai_chunk_entities_entity ON ai_chunk_entities(entity_id)");

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS ai_edges (
                    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                    src_entity_id UUID NOT NULL REFERENCES ai_entities(id) ON DELETE CASCADE,
                    dst_entity_id UUID NOT NULL REFERENCES ai_entities(id) ON DELETE CASCADE,
                    relation_type TEXT NOT NULL,
                    weight REAL NOT NULL DEFAULT 1.0,
                    metadata JSONB,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    CONSTRAINT uq_ai_edges_unique UNIQUE (src_entity_id, dst_entity_id, relation_type)
                )
                """);
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_ai_edges_src ON ai_edges(src_entity_id)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_ai_edges_dst ON ai_edges(dst_entity_id)");
    }
}
