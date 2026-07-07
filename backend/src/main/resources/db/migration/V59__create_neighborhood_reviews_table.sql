-- RoomBay 2.0 — Phase 5: crowdsourced neighborhood assessment ratings.
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS neighborhood_reviews (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reviewer_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    city VARCHAR(100) NOT NULL,
    neighborhood VARCHAR(100) NOT NULL,
    safety_rating SMALLINT NOT NULL,
    amenities_rating SMALLINT NOT NULL,
    transport_rating SMALLINT NOT NULL,
    noise_rating SMALLINT NOT NULL,
    comment TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_neighborhood_reviews_reviewer_area
    ON neighborhood_reviews (reviewer_id, city, neighborhood);

CREATE INDEX IF NOT EXISTS idx_neighborhood_reviews_area
    ON neighborhood_reviews (city, neighborhood);
