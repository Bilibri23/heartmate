-- Fix corrupted array columns that contain JSON objects instead of PostgreSQL arrays
-- The data might be stored as JSON objects {"key":"value"} or JSON arrays ["value"]
-- PostgreSQL arrays should look like {value1,value2} without quotes around keys

-- Fix profiles.languages column - reset any malformed data
-- JSON objects contain ":" which valid PostgreSQL arrays won't have (unless value contains colon, but that's rare for languages)
UPDATE profiles 
SET languages = '{}'::TEXT[] 
WHERE languages IS NOT NULL 
  AND (
    languages::TEXT LIKE '{"%'           -- Starts with {" (JSON object)
    OR languages::TEXT LIKE '[%'         -- Starts with [ (JSON array)
    OR languages::TEXT ~ '^\\{[^}]*":"'  -- Contains ": pattern (JSON key-value)
  );

-- Fix roommate_preferences.preferred_locations column  
UPDATE roommate_preferences 
SET preferred_locations = '{}'::TEXT[] 
WHERE preferred_locations IS NOT NULL 
  AND (
    preferred_locations::TEXT LIKE '{"%'
    OR preferred_locations::TEXT LIKE '[%'
    OR preferred_locations::TEXT ~ '^\\{[^}]*":"'
  );

-- Fix listing_preferences.preferred_locations column
UPDATE listing_preferences 
SET preferred_locations = '{}'::TEXT[] 
WHERE preferred_locations IS NOT NULL 
  AND (
    preferred_locations::TEXT LIKE '{"%'
    OR preferred_locations::TEXT LIKE '[%'
    OR preferred_locations::TEXT ~ '^\\{[^}]*":"'
  );

-- Fix listing_preferences.property_types column
UPDATE listing_preferences 
SET property_types = '{}'::TEXT[] 
WHERE property_types IS NOT NULL 
  AND (
    property_types::TEXT LIKE '{"%'
    OR property_types::TEXT LIKE '[%'
    OR property_types::TEXT ~ '^\\{[^}]*":"'
  );

-- Fix property_listings.amenities column
UPDATE property_listings 
SET amenities = '{}'::TEXT[] 
WHERE amenities IS NOT NULL 
  AND (
    amenities::TEXT LIKE '{"%'
    OR amenities::TEXT LIKE '[%'
    OR amenities::TEXT ~ '^\\{[^}]*":"'
  );
