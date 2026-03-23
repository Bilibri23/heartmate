package org.rooms.roombuddy.ai.rag;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Bootstraps pgvector + RAG tables at runtime.
 * Flyway is disabled in dev in this repo; this ensures the assistant works locally.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AiRagSchemaInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

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
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS ai_chunks (
                    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                    document_id UUID NOT NULL REFERENCES ai_documents(id) ON DELETE CASCADE,
                    chunk_index INTEGER NOT NULL,
                    chunk_text TEXT NOT NULL,
                    embedding vector(1536) NOT NULL,
                    metadata jsonb,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """);
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_ai_chunks_document_id ON ai_chunks(document_id)");
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_ai_chunks_embedding_cos ON ai_chunks USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100)");

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
            log.info("[AI] RAG schema ready");
        } catch (Exception e) {
            // Don't block app startup if pgvector isn't installed yet.
            log.warn("[AI] Failed to initialize RAG schema (pgvector missing?): {}", e.getMessage());
        }
    }
}

