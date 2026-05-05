-- GraphRAG: entities, chunk links, and edges (Postgres-native graph layer on top of ai_chunks)
--
-- Base RAG tables must exist first. They are defined in V32; this block repeats them with IF NOT EXISTS
-- so this migration still succeeds when applied alone (e.g. manual SQL run) or when V32 was never applied.

CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS ai_documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source TEXT NOT NULL,
    title TEXT,
    checksum TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

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

CREATE INDEX IF NOT EXISTS idx_ai_chunks_embedding_cos
ON ai_chunks USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);

-- GraphRAG tables

CREATE TABLE IF NOT EXISTS ai_entities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    canonical_key TEXT NOT NULL,
    entity_type TEXT NOT NULL,
    label TEXT,
    source_document_id UUID NOT NULL REFERENCES ai_documents(id) ON DELETE CASCADE,
    metadata JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,npm run dev
    CONSTRAINT uq_ai_entities_canonical UNIQUE (canonical_key)
);

CREATE INDEX IF NOT EXISTS idx_ai_entities_document ON ai_entities(source_document_id);
CREATE INDEX IF NOT EXISTS idx_ai_entities_type ON ai_entities(entity_type);

CREATE TABLE IF NOT EXISTS ai_chunk_entities (
    chunk_id UUID NOT NULL REFERENCES ai_chunks(id) ON DELETE CASCADE,
    entity_id UUID NOT NULL REFERENCES ai_entities(id) ON DELETE CASCADE,
    weight REAL NOT NULL DEFAULT 1.0,
    PRIMARY KEY (chunk_id, entity_id)
);

CREATE INDEX IF NOT EXISTS idx_ai_chunk_entities_entity ON ai_chunk_entities(entity_id);

CREATE TABLE IF NOT EXISTS ai_edges (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    src_entity_id UUID NOT NULL REFERENCES ai_entities(id) ON DELETE CASCADE,
    dst_entity_id UUID NOT NULL REFERENCES ai_entities(id) ON DELETE CASCADE,
    relation_type TEXT NOT NULL,
    weight REAL NOT NULL DEFAULT 1.0,
    metadata JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_ai_edges_unique UNIQUE (src_entity_id, dst_entity_id, relation_type)
);

CREATE INDEX IF NOT EXISTS idx_ai_edges_src ON ai_edges(src_entity_id);
CREATE INDEX IF NOT EXISTS idx_ai_edges_dst ON ai_edges(dst_entity_id);
