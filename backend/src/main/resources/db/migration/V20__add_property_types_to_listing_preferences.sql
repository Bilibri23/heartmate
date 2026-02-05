ALTER TABLE listing_preferences
ADD COLUMN IF NOT EXISTS property_types TEXT[];
