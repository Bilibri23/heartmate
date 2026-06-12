-- Listing taxonomy (rent/sale, stay type, furnishing) and shared-accommodation fields.

ALTER TABLE property_listings
    ADD COLUMN IF NOT EXISTS listing_purpose VARCHAR(20) NOT NULL DEFAULT 'RENT',
    ADD COLUMN IF NOT EXISTS stay_type VARCHAR(20) NOT NULL DEFAULT 'LONG_TERM',
    ADD COLUMN IF NOT EXISTS price_period VARCHAR(20) NOT NULL DEFAULT 'MONTH',
    ADD COLUMN IF NOT EXISTS sale_price INTEGER,
    ADD COLUMN IF NOT EXISTS min_stay_days INTEGER,
    ADD COLUMN IF NOT EXISTS max_stay_days INTEGER,
    ADD COLUMN IF NOT EXISTS furnishing_status VARCHAR(20) NOT NULL DEFAULT 'UNFURNISHED',
    ADD COLUMN IF NOT EXISTS is_shared_accommodation BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS roommates_wanted BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS available_roommate_slots INTEGER,
    ADD COLUMN IF NOT EXISTS shared_space_notes TEXT,
    ADD COLUMN IF NOT EXISTS roommate_compatibility_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS preferred_roommate_gender VARCHAR(20);

-- Backfill furnishing from legacy boolean + amenities
UPDATE property_listings
SET furnishing_status = CASE
    WHEN amenities IS NOT NULL AND 'Furnished' = ANY(amenities) THEN 'FURNISHED'
    WHEN is_unfurnished = TRUE THEN 'UNFURNISHED'
    ELSE 'SEMI_FURNISHED'
END
WHERE furnishing_status = 'UNFURNISHED'
  AND (amenities IS NOT NULL AND 'Furnished' = ANY(amenities) OR is_unfurnished = FALSE);

UPDATE property_listings
SET is_shared_accommodation = TRUE,
    roommates_wanted = TRUE
WHERE property_type = 'SHARED_ROOM';

COMMENT ON COLUMN property_listings.listing_purpose IS 'RENT or SALE';
COMMENT ON COLUMN property_listings.stay_type IS 'SHORT_TERM, LONG_TERM, or FLEXIBLE (rentals only)';
COMMENT ON COLUMN property_listings.price_period IS 'NIGHT, WEEK, MONTH, or TOTAL';
COMMENT ON COLUMN property_listings.furnishing_status IS 'FURNISHED, SEMI_FURNISHED, or UNFURNISHED';
