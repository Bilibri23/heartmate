DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'roommate_preferences'
          AND column_name = 'preferred_locations'
          AND udt_name = '_text'
    ) THEN
        ALTER TABLE roommate_preferences
            ALTER COLUMN preferred_locations DROP DEFAULT,
            ALTER COLUMN preferred_locations TYPE jsonb
                USING to_jsonb(COALESCE(preferred_locations, ARRAY[]::text[]));
    END IF;

    ALTER TABLE roommate_preferences
        ALTER COLUMN preferred_locations SET DEFAULT '[]'::jsonb;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'listing_preferences'
          AND column_name = 'preferred_locations'
          AND udt_name = '_text'
    ) THEN
        ALTER TABLE listing_preferences
            ALTER COLUMN preferred_locations DROP DEFAULT,
            ALTER COLUMN preferred_locations TYPE jsonb
                USING to_jsonb(COALESCE(preferred_locations, ARRAY[]::text[]));
    END IF;

    ALTER TABLE listing_preferences
        ALTER COLUMN preferred_locations SET DEFAULT '[]'::jsonb;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'listing_preferences'
          AND column_name = 'property_types'
          AND udt_name = '_text'
    ) THEN
        ALTER TABLE listing_preferences
            ALTER COLUMN property_types DROP DEFAULT,
            ALTER COLUMN property_types TYPE jsonb
                USING to_jsonb(COALESCE(property_types, ARRAY[]::text[]));
    END IF;

    ALTER TABLE listing_preferences
        ALTER COLUMN property_types SET DEFAULT '[]'::jsonb;
END $$;
