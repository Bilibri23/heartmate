-- Fix corrupted array columns that might have been stored as JSON objects
-- This migration converts any malformed data to proper PostgreSQL arrays

-- Fix profiles.languages column - handle JSON objects (start with {")
UPDATE profiles 
SET languages = '{}'::TEXT[] 
WHERE languages IS NULL 
   OR languages::TEXT = '{}' 
   OR languages::TEXT = ''
   OR languages::TEXT LIKE '{"%'
   OR languages::TEXT LIKE '{"_%';

-- Fix roommate_preferences.preferred_locations column  
UPDATE roommate_preferences 
SET preferred_locations = '{}'::TEXT[] 
WHERE preferred_locations IS NULL 
   OR preferred_locations::TEXT = '{}' 
   OR preferred_locations::TEXT = ''
   OR preferred_locations::TEXT LIKE '{"%'
   OR preferred_locations::TEXT LIKE '{"_%';

-- Fix listing_preferences.preferred_locations column
UPDATE listing_preferences 
SET preferred_locations = '{}'::TEXT[] 
WHERE preferred_locations IS NULL 
   OR preferred_locations::TEXT = '{}' 
   OR preferred_locations::TEXT = ''
   OR preferred_locations::TEXT LIKE '{"%'
   OR preferred_locations::TEXT LIKE '{"_%';

-- Fix listing_preferences.property_types column
UPDATE listing_preferences 
SET property_types = '{}'::TEXT[] 
WHERE property_types IS NULL 
   OR property_types::TEXT = '{}' 
   OR property_types::TEXT = ''
   OR property_types::TEXT LIKE '{"%'
   OR property_types::TEXT LIKE '{"_%';

-- Fix property_listings.amenities column
UPDATE property_listings 
SET amenities = '{}'::TEXT[] 
WHERE amenities IS NULL 
   OR amenities::TEXT = '{}' 
   OR amenities::TEXT = ''
   OR amenities::TEXT LIKE '{"%'
   OR amenities::TEXT LIKE '{"_%';

-- Add comment
COMMENT ON TABLE profiles IS 'User profiles with fixed array columns';
