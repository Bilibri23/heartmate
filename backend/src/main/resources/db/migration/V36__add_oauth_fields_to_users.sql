-- OAuth (e.g. Google Sign-In): link external subject to local user
ALTER TABLE users ADD COLUMN IF NOT EXISTS oauth_provider VARCHAR(32);
ALTER TABLE users ADD COLUMN IF NOT EXISTS oauth_subject VARCHAR(255);

CREATE UNIQUE INDEX IF NOT EXISTS idx_users_oauth_provider_subject
    ON users (oauth_provider, oauth_subject)
    WHERE oauth_subject IS NOT NULL;

-- OAuth-created accounts may not have a password until the user sets one
ALTER TABLE users ALTER COLUMN password_hash DROP NOT NULL;
