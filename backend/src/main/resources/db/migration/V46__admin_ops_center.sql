CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS app_error_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    level VARCHAR(20) NOT NULL,
    source VARCHAR(80) NOT NULL,
    message TEXT NOT NULL,
    path VARCHAR(500),
    user_id UUID,
    role VARCHAR(50),
    stack_trace TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS analytics_event (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type VARCHAR(100) NOT NULL,
    user_id UUID,
    role VARCHAR(50),
    listing_id UUID,
    metadata JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ai_ingest_run (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    status VARCHAR(30) NOT NULL,
    documents_processed INTEGER NOT NULL DEFAULT 0,
    chunks_inserted INTEGER NOT NULL DEFAULT 0,
    graph_entities_written INTEGER NOT NULL DEFAULT 0,
    graph_edges_written INTEGER NOT NULL DEFAULT 0,
    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    finished_at TIMESTAMP,
    error_message TEXT
);

CREATE INDEX IF NOT EXISTS idx_app_error_log_created_at ON app_error_log(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_app_error_log_level_created_at ON app_error_log(level, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_app_error_log_source_created_at ON app_error_log(source, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_analytics_event_created_at ON analytics_event(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_analytics_event_type_created_at ON analytics_event(event_type, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_analytics_event_user_created_at ON analytics_event(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_analytics_event_listing_created_at ON analytics_event(listing_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_ai_ingest_run_status_finished_at ON ai_ingest_run(status, finished_at DESC);
CREATE INDEX IF NOT EXISTS idx_ai_ingest_run_started_at ON ai_ingest_run(started_at DESC);
