-- RoomBay 2.0 — Phase 2: verified Realtor role.
-- realtor_profiles holds the agency/agent profile for a user whose role is REALTOR;
-- realtor_verification_documents holds the KYC documents an admin reviews before verifying.
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS realtor_profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    agency_name VARCHAR(160),
    business_registration_number VARCHAR(120),
    city VARCHAR(120),
    areas_covered TEXT,
    phone_number VARCHAR(40),
    whatsapp_number VARCHAR(40),
    bio TEXT,
    verification_status VARCHAR(40) NOT NULL DEFAULT 'PENDING',
    rejection_reason TEXT,
    reviewed_by UUID,
    reviewed_at TIMESTAMP,
    trust_score INTEGER NOT NULL DEFAULT 0,
    total_listings INTEGER NOT NULL DEFAULT 0,
    successful_rentals INTEGER NOT NULL DEFAULT 0,
    complaint_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_realtor_profiles_status ON realtor_profiles (verification_status);
CREATE INDEX IF NOT EXISTS idx_realtor_profiles_city ON realtor_profiles (city);

CREATE TABLE IF NOT EXISTS realtor_verification_documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    realtor_id UUID NOT NULL REFERENCES realtor_profiles(id) ON DELETE CASCADE,
    document_type VARCHAR(60) NOT NULL,
    document_url TEXT NOT NULL,
    status VARCHAR(40) NOT NULL DEFAULT 'PENDING',
    reviewed_by UUID,
    reviewed_at TIMESTAMP,
    rejection_reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_realtor_docs_realtor ON realtor_verification_documents (realtor_id);
CREATE INDEX IF NOT EXISTS idx_realtor_docs_status ON realtor_verification_documents (status);
