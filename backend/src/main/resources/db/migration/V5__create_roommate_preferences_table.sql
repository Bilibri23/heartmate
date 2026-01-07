-- =====================================================
-- ROOMMATE PREFERENCES TABLE
-- =====================================================
CREATE TABLE roommate_preferences (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE,
    
    -- Budget Preferences
    min_budget INTEGER, -- in XAF (Central African Franc)
    max_budget INTEGER, -- in XAF
    
    -- Location Preferences
    preferred_locations TEXT[], -- Array of neighborhood names
    max_distance_from_campus DECIMAL(10, 2), -- in kilometers
    
    -- Lifestyle Preferences (1-5 scale)
    cleanliness_level INTEGER CHECK (cleanliness_level >= 1 AND cleanliness_level <= 5),
    noise_tolerance INTEGER CHECK (noise_tolerance >= 1 AND noise_tolerance <= 5),
    social_level INTEGER CHECK (social_level >= 1 AND social_level <= 5),
    
    -- Schedule Preferences
    sleep_schedule VARCHAR(20) CHECK (sleep_schedule IN ('EARLY_BIRD', 'NIGHT_OWL', 'FLEXIBLE')),
    study_time_preference VARCHAR(20) CHECK (study_time_preference IN ('MORNING', 'AFTERNOON', 'EVENING', 'NIGHT', 'FLEXIBLE')),
    
    -- Habit Preferences
    smoking BOOLEAN,
    drinking BOOLEAN,
    pets BOOLEAN,
    guests BOOLEAN,
    cooking BOOLEAN,
    
    -- Deal-breakers
    deal_breakers TEXT, -- Free text for deal-breakers
    
    -- Roommate Preferences
    preferred_gender VARCHAR(20) CHECK (preferred_gender IN ('MALE', 'FEMALE', 'ANY')),
    min_age INTEGER,
    max_age INTEGER,
    same_university BOOLEAN DEFAULT FALSE,
    same_faculty BOOLEAN DEFAULT FALSE,
    
    -- Status
    looking_for_roommate BOOLEAN DEFAULT TRUE,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT fk_roommate_preferences_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create indexes
CREATE INDEX idx_roommate_preferences_user_id ON roommate_preferences(user_id);
CREATE INDEX idx_roommate_preferences_looking ON roommate_preferences(looking_for_roommate);
CREATE INDEX idx_roommate_preferences_budget ON roommate_preferences(min_budget, max_budget);

-- Comments
COMMENT ON TABLE roommate_preferences IS 'Roommate preferences for matching algorithm';
COMMENT ON COLUMN roommate_preferences.min_budget IS 'Minimum budget in XAF (Central African Franc)';
COMMENT ON COLUMN roommate_preferences.max_budget IS 'Maximum budget in XAF';
COMMENT ON COLUMN roommate_preferences.preferred_locations IS 'Array of preferred neighborhood names';
COMMENT ON COLUMN roommate_preferences.max_distance_from_campus IS 'Maximum distance from campus in kilometers';
COMMENT ON COLUMN roommate_preferences.cleanliness_level IS 'Cleanliness preference (1=Don''t mind mess, 5=Very clean)';
COMMENT ON COLUMN roommate_preferences.noise_tolerance IS 'Noise tolerance (1=Very quiet, 5=Don''t mind noise)';
COMMENT ON COLUMN roommate_preferences.social_level IS 'Social level (1=Very private, 5=Very social)';
COMMENT ON COLUMN roommate_preferences.sleep_schedule IS 'Sleep schedule preference';
COMMENT ON COLUMN roommate_preferences.study_time_preference IS 'Preferred study time';
COMMENT ON COLUMN roommate_preferences.deal_breakers IS 'Free text for deal-breakers (e.g., "No smokers", "No parties")';
COMMENT ON COLUMN roommate_preferences.looking_for_roommate IS 'Whether user is currently looking for a roommate';

