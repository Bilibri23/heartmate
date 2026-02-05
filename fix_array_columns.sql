-- =====================================================
-- RUN THIS SCRIPT DIRECTLY IN YOUR DATABASE BEFORE STARTING THE APP
-- Stop the backend first, then run this, then start the backend
-- =====================================================

-- profiles.languages - drop and recreate as JSONB
ALTER TABLE profiles DROP COLUMN IF EXISTS languages;
ALTER TABLE profiles ADD COLUMN languages JSONB DEFAULT '[]'::JSONB;

-- roommate_preferences.preferred_locations
ALTER TABLE roommate_preferences DROP COLUMN IF EXISTS preferred_locations;
ALTER TABLE roommate_preferences ADD COLUMN preferred_locations JSONB DEFAULT '[]'::JSONB;

-- listing_preferences.preferred_locations  
ALTER TABLE listing_preferences DROP COLUMN IF EXISTS preferred_locations;
ALTER TABLE listing_preferences ADD COLUMN preferred_locations JSONB DEFAULT '[]'::JSONB;

-- listing_preferences.property_types
ALTER TABLE listing_preferences DROP COLUMN IF EXISTS property_types;
ALTER TABLE listing_preferences ADD COLUMN property_types JSONB DEFAULT '[]'::JSONB;

-- property_listings.amenities
ALTER TABLE property_listings DROP COLUMN IF EXISTS amenities;
ALTER TABLE property_listings ADD COLUMN amenities JSONB DEFAULT '[]'::JSONB;

-- Verify the columns are now JSONB
SELECT table_name, column_name, data_type
FROM information_schema.columns 
WHERE (table_name = 'profiles' AND column_name = 'languages')
   OR (table_name = 'roommate_preferences' AND column_name = 'preferred_locations')
   OR (table_name = 'listing_preferences' AND column_name IN ('preferred_locations', 'property_types'))
   OR (table_name = 'property_listings' AND column_name = 'amenities');
