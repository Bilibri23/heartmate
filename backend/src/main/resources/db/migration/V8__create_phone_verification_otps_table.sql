-- =====================================================
-- PHONE VERIFICATION OTPs TABLE
-- =====================================================
CREATE TABLE phone_verification_otps (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    phone_number VARCHAR(20) NOT NULL,
    otp_code VARCHAR(6) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    verified BOOLEAN DEFAULT FALSE,
    verified_at TIMESTAMP,
    attempts INTEGER DEFAULT 0,
    max_attempts INTEGER DEFAULT 3,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    
    CONSTRAINT fk_phone_verification_otps_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create indexes
CREATE INDEX idx_phone_verification_otps_user_id ON phone_verification_otps(user_id);
CREATE INDEX idx_phone_verification_otps_phone_number ON phone_verification_otps(phone_number);
CREATE INDEX idx_phone_verification_otps_otp_code ON phone_verification_otps(otp_code);
CREATE INDEX idx_phone_verification_otps_expires_at ON phone_verification_otps(expires_at);

-- Comments
COMMENT ON TABLE phone_verification_otps IS 'OTP codes for phone/WhatsApp verification';
COMMENT ON COLUMN phone_verification_otps.otp_code IS '6-digit OTP code';
COMMENT ON COLUMN phone_verification_otps.expires_at IS 'OTP expiration timestamp (5 minutes from creation)';
COMMENT ON COLUMN phone_verification_otps.attempts IS 'Number of verification attempts';
COMMENT ON COLUMN phone_verification_otps.max_attempts IS 'Maximum allowed attempts before OTP is invalidated';

