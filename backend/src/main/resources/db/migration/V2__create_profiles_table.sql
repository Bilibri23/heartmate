-- =====================================================
-- PROFILES TABLE
-- =====================================================
CREATE TABLE profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE,
    bio TEXT,
    profile_photo_url VARCHAR(500),
    languages TEXT[],
    whatsapp_number VARCHAR(20),
    emergency_contact_name VARCHAR(100),
    emergency_contact_phone VARCHAR(20),
    emergency_contact_relationship VARCHAR(50),
    visibility VARCHAR(20) DEFAULT 'PUBLIC' CHECK (visibility IN ('PUBLIC', 'VERIFIED_ONLY', 'PRIVATE')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT fk_profiles_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create indexes for performance
CREATE INDEX idx_profiles_user_id ON profiles(user_id);
CREATE INDEX idx_profiles_visibility ON profiles(visibility);

-- =====================================================
-- COMMENTS FOR DOCUMENTATION
-- =====================================================
COMMENT ON TABLE profiles IS 'Minimal user profile for display and contact only - NOT for matching. All roommate matching logic is handled in roommate_preferences table.';
COMMENT ON COLUMN profiles.bio IS 'User biography for display purposes';
COMMENT ON COLUMN profiles.profile_photo_url IS 'Profile photo URL for display';
COMMENT ON COLUMN profiles.languages IS 'Array of languages spoken: English, French, Pidgin (minor 5% matching weight for language barrier prevention)';
COMMENT ON COLUMN profiles.whatsapp_number IS 'WhatsApp contact number in format +237XXXXXXXXX (primary contact method in Cameroon)';
COMMENT ON COLUMN profiles.emergency_contact_name IS 'Emergency contact name (required for safety/landlord requirements)';
COMMENT ON COLUMN profiles.emergency_contact_phone IS 'Emergency contact phone in format +237XXXXXXXXX';
COMMENT ON COLUMN profiles.emergency_contact_relationship IS 'Relationship to emergency contact (parent, sibling, friend, etc.)';
COMMENT ON COLUMN profiles.visibility IS 'Profile visibility: PUBLIC (everyone), VERIFIED_ONLY (verified students only), PRIVATE (connections only)';

