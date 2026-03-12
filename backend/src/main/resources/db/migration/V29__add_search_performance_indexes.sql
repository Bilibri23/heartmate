-- =====================================================
-- SEARCH PERFORMANCE INDEXES
-- =====================================================
-- Composite index for the most common search pattern: active + verified listings
-- filtered by city and price range, ordered by creation date
CREATE INDEX IF NOT EXISTS idx_listings_search_main
    ON property_listings (status, verified, city, rent_amount, created_at DESC)
    WHERE status = 'ACTIVE' AND verified = TRUE;

-- GIN index on amenities array for containment queries (@> operator)
-- Enables efficient filtering like: WHERE amenities @> ARRAY['Furnished', 'WiFi']
CREATE INDEX IF NOT EXISTS idx_listings_amenities_gin
    ON property_listings USING GIN (amenities);

-- Composite index for bedrooms/bathrooms filtering (common secondary filter)
CREATE INDEX IF NOT EXISTS idx_listings_rooms
    ON property_listings (bedrooms, bathrooms)
    WHERE status = 'ACTIVE' AND verified = TRUE;

-- Spatial index placeholder: if PostGIS is enabled, uncomment:
-- CREATE INDEX IF NOT EXISTS idx_listings_location
--     ON property_listings USING GIST (
--         ST_SetSRID(ST_MakePoint(longitude, latitude), 4326)
--     )
--     WHERE status = 'ACTIVE' AND verified = TRUE
--       AND latitude IS NOT NULL AND longitude IS NOT NULL;

COMMENT ON INDEX idx_listings_search_main IS 'Partial composite index for the primary search pattern';
COMMENT ON INDEX idx_listings_amenities_gin IS 'GIN index for fast amenities array containment queries';
COMMENT ON INDEX idx_listings_rooms IS 'Composite index for bedroom/bathroom filtering on active listings';
