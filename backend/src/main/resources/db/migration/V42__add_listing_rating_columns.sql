-- =====================================================
-- ADD MISSING RATING COLUMNS TO PROPERTY_LISTINGS
-- =====================================================
-- These columns exist in the entity but were missed in V7

ALTER TABLE property_listings
    ADD COLUMN IF NOT EXISTS average_rating DECIMAL(3,2) DEFAULT 0.0,
    ADD COLUMN IF NOT EXISTS review_count INTEGER DEFAULT 0;

COMMENT ON COLUMN property_listings.average_rating IS 'Average rating from reviews (0-5)';
COMMENT ON COLUMN property_listings.review_count IS 'Total number of reviews';
