-- V11: Add User Management Fields
-- Add fields for user suspension, tracking, and moderation

ALTER TABLE users ADD COLUMN IF NOT EXISTS suspended BOOLEAN DEFAULT FALSE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS suspended_at TIMESTAMP;
ALTER TABLE users ADD COLUMN IF NOT EXISTS suspended_by UUID;
ALTER TABLE users ADD COLUMN IF NOT EXISTS suspension_reason TEXT;
ALTER TABLE users ADD COLUMN IF NOT EXISTS last_login_at TIMESTAMP;
ALTER TABLE users ADD COLUMN IF NOT EXISTS login_count INTEGER DEFAULT 0;
ALTER TABLE users ADD COLUMN IF NOT EXISTS account_status VARCHAR(50) DEFAULT 'ACTIVE';

-- Create index for faster queries
CREATE INDEX IF NOT EXISTS idx_users_suspended ON users(suspended);
CREATE INDEX IF NOT EXISTS idx_users_account_status ON users(account_status);
CREATE INDEX IF NOT EXISTS idx_users_role ON users(role);
CREATE INDEX IF NOT EXISTS idx_users_last_login ON users(last_login_at);

-- Add foreign key constraint for suspended_by
ALTER TABLE users ADD CONSTRAINT fk_users_suspended_by 
    FOREIGN KEY (suspended_by) REFERENCES users(id) ON DELETE SET NULL;

-- Add comment
COMMENT ON COLUMN users.suspended IS 'Whether the user account is currently suspended';
COMMENT ON COLUMN users.suspended_at IS 'Timestamp when the account was suspended';
COMMENT ON COLUMN users.suspended_by IS 'Admin user ID who suspended this account';
COMMENT ON COLUMN users.suspension_reason IS 'Reason for account suspension';
COMMENT ON COLUMN users.last_login_at IS 'Last successful login timestamp';
COMMENT ON COLUMN users.login_count IS 'Total number of successful logins';
COMMENT ON COLUMN users.account_status IS 'Account status: ACTIVE, SUSPENDED, PENDING, DELETED';
