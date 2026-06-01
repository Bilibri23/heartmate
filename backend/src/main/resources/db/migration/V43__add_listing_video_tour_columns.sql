-- =====================================================
-- ADD VIDEO TOUR COLUMNS TO PROPERTY_LISTINGS
-- =====================================================
-- These columns exist in the entity but were missed in V7

ALTER TABLE property_listings
    ADD COLUMN IF NOT EXISTS video_tour_url VARCHAR(500),
    ADD COLUMN IF NOT EXISTS video_tour_embed_code TEXT,
    ADD COLUMN IF NOT EXISTS virtual_tour_provider VARCHAR(50),
    ADD COLUMN IF NOT EXISTS video_tour_thumbnail VARCHAR(500),
    ADD COLUMN IF NOT EXISTS video_tour_duration INTEGER;

COMMENT ON COLUMN property_listings.video_tour_url IS 'URL to video tour (YouTube, Vimeo, etc.)';
COMMENT ON COLUMN property_listings.video_tour_embed_code IS 'HTML embed code for video tour';
COMMENT ON COLUMN property_listings.virtual_tour_provider IS 'Virtual tour provider: panoee, kuula, zillow, cloudpano, generic';
COMMENT ON COLUMN property_listings.video_tour_thumbnail IS 'Thumbnail URL for video tour';
COMMENT ON COLUMN property_listings.video_tour_duration IS 'Video tour duration in seconds';
