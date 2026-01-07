-- =====================================================
-- LEASES TABLE
-- Represents formal lease agreements between students and landlords
-- =====================================================

CREATE TABLE leases (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    listing_id UUID NOT NULL REFERENCES property_listings(id) ON DELETE RESTRICT,
    student_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    landlord_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    application_id UUID REFERENCES room_applications(id) ON DELETE SET NULL,
    
    -- Lease Terms
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    monthly_rent INTEGER NOT NULL,
    deposit_amount INTEGER,
    agency_fees INTEGER,
    total_initial_payment INTEGER,
    
    -- Status
    status VARCHAR(30) DEFAULT 'PENDING_PAYMENT' CHECK (status IN (
        'PENDING_PAYMENT', 'PENDING_SIGNATURES', 'ACTIVE', 
        'COMPLETED', 'TERMINATED', 'CANCELLED', 'DISPUTED'
    )),
    
    -- Signatures
    student_accepted_at TIMESTAMP,
    landlord_accepted_at TIMESTAMP,
    student_accepted_terms BOOLEAN DEFAULT FALSE,
    landlord_accepted_terms BOOLEAN DEFAULT FALSE,
    
    -- Terms document
    terms_content TEXT,
    
    -- Move dates
    actual_move_in_date DATE,
    actual_move_out_date DATE,
    
    -- Termination
    termination_reason VARCHAR(30) CHECK (termination_reason IN (
        'MUTUAL_AGREEMENT', 'STUDENT_REQUEST', 'LANDLORD_REQUEST',
        'NON_PAYMENT', 'LEASE_VIOLATION', 'PROPERTY_ISSUE', 'OTHER'
    )),
    termination_notes TEXT,
    terminated_by UUID REFERENCES users(id),
    terminated_at TIMESTAMP,
    
    -- Reference code
    reference_code VARCHAR(50) UNIQUE,
    
    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX idx_leases_student ON leases(student_id);
CREATE INDEX idx_leases_landlord ON leases(landlord_id);
CREATE INDEX idx_leases_listing ON leases(listing_id);
CREATE INDEX idx_leases_status ON leases(status);
CREATE INDEX idx_leases_reference ON leases(reference_code);
CREATE INDEX idx_leases_dates ON leases(start_date, end_date);

-- Comments
COMMENT ON TABLE leases IS 'Formal lease agreements between students and landlords';
COMMENT ON COLUMN leases.status IS 'Lease lifecycle: PENDING_PAYMENT -> PENDING_SIGNATURES -> ACTIVE -> COMPLETED/TERMINATED';
COMMENT ON COLUMN leases.reference_code IS 'Unique reference code for payments (RB-YYYYMMDD-XXXX)';
