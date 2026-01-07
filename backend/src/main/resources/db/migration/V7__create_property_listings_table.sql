-- =====================================================
-- PROPERTY LISTINGS TABLE
-- =====================================================
CREATE TABLE property_listings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    landlord_id UUID NOT NULL,
    
    -- Basic Information
    title VARCHAR(255) NOT NULL,
    description TEXT,
    property_type VARCHAR(50) CHECK (property_type IN ('APARTMENT', 'HOUSE', 'STUDIO', 'SHARED_ROOM', 'PRIVATE_ROOM')),
    
    -- Pricing
    rent_amount INTEGER NOT NULL, -- in XAF
    deposit INTEGER, -- in XAF
    agency_fees INTEGER, -- in XAF
    
    -- Location
    region VARCHAR(100),
    city VARCHAR(100),
    neighborhood VARCHAR(100),
    address TEXT,
    latitude DECIMAL(10, 8),
    longitude DECIMAL(11, 8),
    distance_to_university DECIMAL(10, 2), -- in kilometers
    
    -- Property Details
    bedrooms INTEGER,
    bathrooms INTEGER,
    square_meters INTEGER,
    floor INTEGER,
    
    -- Amenities (stored as array)
    amenities TEXT[],
    
    -- Availability
    available_from DATE,
    available_to DATE,
    
    -- Status
    status VARCHAR(20) DEFAULT 'DRAFT' CHECK (status IN ('DRAFT', 'PENDING', 'ACTIVE', 'RENTED', 'INACTIVE', 'DELETED')),
    verified BOOLEAN DEFAULT FALSE,
    featured BOOLEAN DEFAULT FALSE,
    
    -- Contact
    landlord_whatsapp VARCHAR(20),
    
    -- Statistics
    views_count INTEGER DEFAULT 0,
    favorites_count INTEGER DEFAULT 0,
    
    -- Admin
    verified_by UUID,
    verified_at TIMESTAMP,
    rejection_reason TEXT,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    
    CONSTRAINT fk_property_listings_landlord FOREIGN KEY (landlord_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_property_listings_verifier FOREIGN KEY (verified_by) REFERENCES users(id)
);

-- Create indexes
CREATE INDEX idx_property_listings_landlord_id ON property_listings(landlord_id);
CREATE INDEX idx_property_listings_status ON property_listings(status);
CREATE INDEX idx_property_listings_city ON property_listings(city);
CREATE INDEX idx_property_listings_neighborhood ON property_listings(neighborhood);
CREATE INDEX idx_property_listings_rent_amount ON property_listings(rent_amount);
CREATE INDEX idx_property_listings_property_type ON property_listings(property_type);
CREATE INDEX idx_property_listings_verified ON property_listings(verified);
CREATE INDEX idx_property_listings_featured ON property_listings(featured);
CREATE INDEX idx_property_listings_created_at ON property_listings(created_at);

-- Comments
COMMENT ON TABLE property_listings IS 'Property listings posted by landlords';
COMMENT ON COLUMN property_listings.rent_amount IS 'Monthly rent in XAF (Central African Franc)';
COMMENT ON COLUMN property_listings.deposit IS 'Security deposit in XAF';
COMMENT ON COLUMN property_listings.agency_fees IS 'Agency fees in XAF';
COMMENT ON COLUMN property_listings.distance_to_university IS 'Distance to nearest university in kilometers';
COMMENT ON COLUMN property_listings.amenities IS 'Array of amenities: WiFi, Water, Electricity, Parking, Security, Furnished';
COMMENT ON COLUMN property_listings.status IS 'DRAFT: not published, PENDING: awaiting approval, ACTIVE: live, RENTED: rented out, INACTIVE: temporarily unavailable, DELETED: removed';
COMMENT ON COLUMN property_listings.verified IS 'Whether listing is verified by admin';

-- =====================================================
-- LISTING PHOTOS TABLE
-- =====================================================
CREATE TABLE listing_photos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    listing_id UUID NOT NULL,
    photo_url VARCHAR(500) NOT NULL,
    is_primary BOOLEAN DEFAULT FALSE,
    display_order INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    
    CONSTRAINT fk_listing_photos_listing FOREIGN KEY (listing_id) REFERENCES property_listings(id) ON DELETE CASCADE
);

-- Create indexes
CREATE INDEX idx_listing_photos_listing_id ON listing_photos(listing_id);
CREATE INDEX idx_listing_photos_is_primary ON listing_photos(is_primary);
CREATE INDEX idx_listing_photos_display_order ON listing_photos(display_order);

-- Comments
COMMENT ON TABLE listing_photos IS 'Photos for property listings';
COMMENT ON COLUMN listing_photos.is_primary IS 'Whether this is the primary/cover photo';
COMMENT ON COLUMN listing_photos.display_order IS 'Order for displaying photos';

-- =====================================================
-- FAVORITES TABLE
-- =====================================================
CREATE TABLE listing_favorites (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    listing_id UUID NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    
    CONSTRAINT fk_listing_favorites_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_listing_favorites_listing FOREIGN KEY (listing_id) REFERENCES property_listings(id) ON DELETE CASCADE,
    CONSTRAINT unique_user_listing_favorite UNIQUE (user_id, listing_id)
);

-- Create indexes
CREATE INDEX idx_listing_favorites_user_id ON listing_favorites(user_id);
CREATE INDEX idx_listing_favorites_listing_id ON listing_favorites(listing_id);

-- Comments
COMMENT ON TABLE listing_favorites IS 'User favorites for property listings';

