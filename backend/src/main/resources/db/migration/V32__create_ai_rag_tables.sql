-- AI RAG tables (pgvector)
-- Note: Flyway may be disabled in dev; see AiRagSchemaInitializer for runtime bootstrap.

CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS ai_documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source TEXT NOT NULL,         -- e.g. docs/PRODUCT-BACKLOG.md
    title TEXT,
    checksum TEXT,                -- content hash to skip re-ingest
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Default embedding dimension: 1536 (OpenAI text-embedding-3-small)
CREATE TABLE IF NOT EXISTS ai_chunks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id UUID NOT NULL REFERENCES ai_documents(id) ON DELETE CASCADE,
    chunk_index INTEGER NOT NULL,
    chunk_text TEXT NOT NULL,
    embedding vector(1536) NOT NULL,
    metadata jsonb,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_ai_chunks_document_id ON ai_chunks(document_id);

-- Vector index (cosine distance). Tune lists based on dataset size.
CREATE INDEX IF NOT EXISTS idx_ai_chunks_embedding_cos
ON ai_chunks USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);

