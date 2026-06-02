-- Create runtime tables that exist as JPA entities but were missing from Flyway.
-- These are additive and idempotent so existing production data is preserved.

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- =====================================================
-- LANDLORD VERIFICATION
-- =====================================================
CREATE TABLE IF NOT EXISTS landlord_verification (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE,

    id_type VARCHAR(50) CHECK (id_type IN ('NATIONAL_ID', 'PASSPORT', 'DRIVERS_LICENSE', 'VOTER_CARD', 'RESIDENCE_PERMIT')),
    id_number VARCHAR(255),
    id_front_photo_url VARCHAR(500),
    id_back_photo_url VARCHAR(500),
    selfie_with_id_url VARCHAR(500),
    id_expiry_date VARCHAR(100),

    business_name VARCHAR(255),
    business_registration_number VARCHAR(255),
    business_registration_doc_url VARCHAR(500),
    tax_id_number VARCHAR(255),

    property_ownership_doc_url VARCHAR(500),
    utility_bill_url VARCHAR(500),

    address_line1 VARCHAR(255),
    address_line2 VARCHAR(255),
    city VARCHAR(100),
    region VARCHAR(100),
    postal_code VARCHAR(50),

    verification_level VARCHAR(50) DEFAULT 'NONE' CHECK (verification_level IN ('NONE', 'BASIC', 'IDENTITY', 'BUSINESS', 'PROPERTY', 'FULLY_VERIFIED')),
    identity_status VARCHAR(50) DEFAULT 'NOT_SUBMITTED' CHECK (identity_status IN ('NOT_SUBMITTED', 'PENDING', 'VERIFIED', 'REJECTED', 'EXPIRED')),
    business_status VARCHAR(50) DEFAULT 'NOT_SUBMITTED' CHECK (business_status IN ('NOT_SUBMITTED', 'PENDING', 'VERIFIED', 'REJECTED', 'EXPIRED')),
    property_status VARCHAR(50) DEFAULT 'NOT_SUBMITTED' CHECK (property_status IN ('NOT_SUBMITTED', 'PENDING', 'VERIFIED', 'REJECTED', 'EXPIRED')),

    identity_rejection_reason TEXT,
    business_rejection_reason TEXT,
    property_rejection_reason TEXT,

    reviewed_by UUID,
    reviewed_at TIMESTAMP,
    admin_notes TEXT,

    trust_score INTEGER DEFAULT 0,
    is_trusted_landlord BOOLEAN DEFAULT FALSE,
    total_listings INTEGER DEFAULT 0,
    successful_rentals INTEGER DEFAULT 0,
    reported_count INTEGER DEFAULT 0,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_landlord_verification_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_landlord_verification_reviewer FOREIGN KEY (reviewed_by) REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_landlord_verification_user_id ON landlord_verification(user_id);
CREATE INDEX IF NOT EXISTS idx_landlord_verification_identity_status ON landlord_verification(identity_status);
CREATE INDEX IF NOT EXISTS idx_landlord_verification_business_status ON landlord_verification(business_status);
CREATE INDEX IF NOT EXISTS idx_landlord_verification_property_status ON landlord_verification(property_status);
CREATE INDEX IF NOT EXISTS idx_landlord_verification_trusted ON landlord_verification(is_trusted_landlord);
CREATE INDEX IF NOT EXISTS idx_landlord_verification_trust_score ON landlord_verification(trust_score);
CREATE INDEX IF NOT EXISTS idx_landlord_verification_reported_count ON landlord_verification(reported_count);
CREATE INDEX IF NOT EXISTS idx_landlord_verification_created_at ON landlord_verification(created_at DESC);

-- =====================================================
-- SAVED SEARCHES
-- =====================================================
CREATE TABLE IF NOT EXISTS saved_searches (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    query TEXT,
    city VARCHAR(100),
    neighborhood VARCHAR(100),
    property_type VARCHAR(50),
    min_price INTEGER,
    max_price INTEGER,
    bedrooms INTEGER,
    bathrooms INTEGER,
    max_distance DOUBLE PRECISION,
    user_lat DOUBLE PRECISION,
    user_lon DOUBLE PRECISION,
    available_from VARCHAR(100),
    notify_new_listings BOOLEAN DEFAULT TRUE,
    notify_price_drops BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_checked_at TIMESTAMP,

    CONSTRAINT fk_saved_searches_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_saved_searches_user_id ON saved_searches(user_id);
CREATE INDEX IF NOT EXISTS idx_saved_searches_created_at ON saved_searches(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_saved_searches_notify_new ON saved_searches(notify_new_listings);
CREATE INDEX IF NOT EXISTS idx_saved_searches_notify_price ON saved_searches(notify_price_drops);

CREATE TABLE IF NOT EXISTS saved_search_amenities (
    saved_search_id UUID NOT NULL,
    amenity VARCHAR(255),

    CONSTRAINT fk_saved_search_amenities_saved_search FOREIGN KEY (saved_search_id) REFERENCES saved_searches(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_saved_search_amenities_saved_search_id ON saved_search_amenities(saved_search_id);

-- =====================================================
-- LISTING VIEWS
-- =====================================================
CREATE TABLE IF NOT EXISTS listing_views (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    listing_id UUID NOT NULL,
    duration_seconds INTEGER,
    source VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT fk_listing_views_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_listing_views_listing FOREIGN KEY (listing_id) REFERENCES property_listings(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_listing_views_user ON listing_views(user_id);
CREATE INDEX IF NOT EXISTS idx_listing_views_listing ON listing_views(listing_id);
CREATE INDEX IF NOT EXISTS idx_listing_views_created ON listing_views(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_listing_views_user_created ON listing_views(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_listing_views_user_listing ON listing_views(user_id, listing_id);

-- =====================================================
-- CONTENT FLAGS
-- =====================================================
CREATE TABLE IF NOT EXISTS content_flags (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    listing_id UUID NOT NULL,
    reporter_id UUID NOT NULL,
    reason VARCHAR(50) NOT NULL CHECK (reason IN ('INAPPROPRIATE_CONTENT', 'VIOLENCE', 'FRAUD', 'SPAM', 'COPYRIGHT', 'ILLEGAL_ACTIVITY', 'QUALITY_ISSUES', 'OTHER')),
    description TEXT,
    status VARCHAR(50) DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'RESOLVED')),
    reviewed_by UUID,
    reviewed_at TIMESTAMP,
    admin_notes TEXT,
    suspicion_score DOUBLE PRECISION,
    is_automated BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_content_flags_listing FOREIGN KEY (listing_id) REFERENCES property_listings(id) ON DELETE CASCADE,
    CONSTRAINT fk_content_flags_reporter FOREIGN KEY (reporter_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_content_flags_reviewer FOREIGN KEY (reviewed_by) REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_content_flags_listing_id ON content_flags(listing_id);
CREATE INDEX IF NOT EXISTS idx_content_flags_reporter_id ON content_flags(reporter_id);
CREATE INDEX IF NOT EXISTS idx_content_flags_status ON content_flags(status);
CREATE INDEX IF NOT EXISTS idx_content_flags_reason ON content_flags(reason);
CREATE INDEX IF NOT EXISTS idx_content_flags_created_at ON content_flags(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_content_flags_automated ON content_flags(is_automated);
CREATE INDEX IF NOT EXISTS idx_content_flags_suspicion_score ON content_flags(suspicion_score DESC);
CREATE UNIQUE INDEX IF NOT EXISTS idx_content_flags_unique_reporter_listing ON content_flags(listing_id, reporter_id);
