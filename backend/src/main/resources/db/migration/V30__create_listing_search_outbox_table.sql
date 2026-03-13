-- =====================================================
-- LISTING SEARCH OUTBOX TABLE
-- =====================================================
-- Transactional outbox used to keep the external search index
-- (Elasticsearch/OpenSearch/Meilisearch) in sync with the primary DB.
-- Each row represents a listing-level change that should be reflected
-- in the search index.

CREATE TABLE listing_search_outbox (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    listing_id UUID NOT NULL,
    event_type VARCHAR(50) NOT NULL, -- LISTING_CREATED, LISTING_UPDATED, LISTING_DELETED
    payload JSONB NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    processed_at TIMESTAMP NULL,
    processing_attempts INTEGER DEFAULT 0
);

CREATE INDEX idx_listing_search_outbox_pending
    ON listing_search_outbox (processed_at, created_at)
    WHERE processed_at IS NULL;

COMMENT ON TABLE listing_search_outbox IS 'Outbox for syncing listings to the search index service';
COMMENT ON COLUMN listing_search_outbox.event_type IS 'Type of event: LISTING_CREATED, LISTING_UPDATED, LISTING_DELETED';
COMMENT ON COLUMN listing_search_outbox.payload IS 'JSON snapshot of the listing relevant for the search index';

