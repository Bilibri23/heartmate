ALTER TABLE profiles
    ADD COLUMN IF NOT EXISTS city VARCHAR(120);

COMMENT ON COLUMN profiles.city IS 'Optional display city for landlord/student public profile';
