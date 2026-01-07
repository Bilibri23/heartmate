
-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- =====================================================
-- USERS TABLE (Minimal to start)
-- =====================================================
CREATE TABLE users (
                       id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                       email VARCHAR(255) UNIQUE NOT NULL,
                       phone VARCHAR(20) UNIQUE NOT NULL,
                       password_hash VARCHAR(255) NOT NULL,
                       first_name VARCHAR(100) NOT NULL,
                       last_name VARCHAR(100) NOT NULL,
                       gender VARCHAR(20) CHECK (gender IN ('MALE', 'FEMALE')),
                       date_of_birth DATE,
                       role VARCHAR(20) DEFAULT 'STUDENT' CHECK (role IN ('STUDENT', 'LANDLORD', 'ADMIN')),
                       account_status VARCHAR(20) DEFAULT 'PENDING' CHECK (account_status IN ('PENDING', 'ACTIVE', 'SUSPENDED', 'DEACTIVATED')),
                       email_verified BOOLEAN DEFAULT FALSE,
                       phone_verified BOOLEAN DEFAULT FALSE,
                       profile_completed BOOLEAN DEFAULT FALSE,
                       last_active TIMESTAMP,
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for performance
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_phone ON users(phone);
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_status ON users(account_status);

-- =====================================================
-- INSERT SAMPLE DATA (for testing)
-- =====================================================
-- Note: Password is 'password123' (bcrypt hashed)
INSERT INTO users (email, phone, password_hash, first_name, last_name, gender, date_of_birth, role, account_status, email_verified)
VALUES
    ('john.doe@example.com', '+237677123456', '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07xd00DMxs.AQubh4a', 'John', 'Doe', 'MALE', '2000-01-15', 'STUDENT', 'ACTIVE', true),
    ('jane.smith@example.com', '+237677234567', '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07xd00DMxs.AQubh4a', 'Jane', 'Smith', 'FEMALE', '1999-05-20', 'STUDENT', 'ACTIVE', true);

-- =====================================================
-- COMMENTS FOR DOCUMENTATION
-- =====================================================
COMMENT ON TABLE users IS 'Main user table storing student, landlord, and admin accounts';
COMMENT ON COLUMN users.password_hash IS 'BCrypt hashed password';
COMMENT ON COLUMN users.account_status IS 'PENDING: awaiting verification, ACTIVE: can use platform, SUSPENDED: temporarily blocked, DEACTIVATED: user disabled account';