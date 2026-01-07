-- V18: Add Mobile Money payment fields to users table for landlords

ALTER TABLE users ADD COLUMN IF NOT EXISTS mtn_momo_number VARCHAR(20);
ALTER TABLE users ADD COLUMN IF NOT EXISTS orange_money_number VARCHAR(20);
ALTER TABLE users ADD COLUMN IF NOT EXISTS momo_name VARCHAR(100);

COMMENT ON COLUMN users.mtn_momo_number IS 'MTN Mobile Money number for receiving payments';
COMMENT ON COLUMN users.orange_money_number IS 'Orange Money number for receiving payments';
COMMENT ON COLUMN users.momo_name IS 'Name registered on Mobile Money account';
