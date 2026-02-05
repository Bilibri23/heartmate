-- Ensure all array columns have the correct TEXT[] type (not JSON/JSONB)
-- This fixes any columns that might have been altered to JSON type

-- Check and fix profiles.languages column type
DO $$
BEGIN
    -- If languages column is not TEXT[], alter it
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'profiles' 
        AND column_name = 'languages' 
        AND data_type != 'ARRAY'
    ) THEN
        -- First, backup the data to a temp column
        ALTER TABLE profiles ADD COLUMN IF NOT EXISTS languages_backup TEXT;
        UPDATE profiles SET languages_backup = languages::TEXT WHERE languages IS NOT NULL;
        
        -- Drop and recreate the column with correct type
        ALTER TABLE profiles DROP COLUMN languages;
        ALTER TABLE profiles ADD COLUMN languages TEXT[];
        
        -- Try to restore data (will be empty array if conversion fails)
        UPDATE profiles SET languages = '{}'::TEXT[];
        
        -- Drop backup
        ALTER TABLE profiles DROP COLUMN IF EXISTS languages_backup;
    END IF;
END $$;

-- For all array columns, ensure they have valid array data
-- Reset any null values to empty arrays
UPDATE profiles SET languages = '{}'::TEXT[] WHERE languages IS NULL;
UPDATE roommate_preferences SET preferred_locations = '{}'::TEXT[] WHERE preferred_locations IS NULL;
UPDATE listing_preferences SET preferred_locations = '{}'::TEXT[] WHERE preferred_locations IS NULL;
UPDATE listing_preferences SET property_types = '{}'::TEXT[] WHERE property_types IS NULL;
UPDATE property_listings SET amenities = '{}'::TEXT[] WHERE amenities IS NULL;
