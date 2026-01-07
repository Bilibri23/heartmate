-- =====================================================
-- DISPUTES TABLE
-- Dispute resolution between students and landlords
-- =====================================================

CREATE TABLE disputes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lease_id UUID NOT NULL REFERENCES leases(id) ON DELETE RESTRICT,
    opened_by UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    against_user UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    
    -- Dispute details
    category VARCHAR(30) NOT NULL CHECK (category IN (
        'PAYMENT_ISSUE', 'PROPERTY_CONDITION', 'DEPOSIT_REFUND',
        'LEASE_VIOLATION', 'HARASSMENT', 'MISREPRESENTATION',
        'MAINTENANCE', 'NOISE_COMPLAINT', 'OTHER'
    )),
    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    
    -- Status and priority
    status VARCHAR(30) DEFAULT 'OPEN' CHECK (status IN (
        'OPEN', 'UNDER_REVIEW', 'AWAITING_RESPONSE', 
        'MEDIATION', 'RESOLVED', 'CLOSED', 'ESCALATED'
    )),
    priority VARCHAR(20) DEFAULT 'MEDIUM' CHECK (priority IN (
        'LOW', 'MEDIUM', 'HIGH', 'URGENT'
    )),
    
    -- Evidence
    evidence_urls TEXT,
    
    -- Admin handling
    assigned_to UUID REFERENCES users(id),
    assigned_at TIMESTAMP,
    
    -- Resolution
    outcome VARCHAR(30) CHECK (outcome IN (
        'RESOLVED_FOR_STUDENT', 'RESOLVED_FOR_LANDLORD', 'MUTUAL_AGREEMENT',
        'NO_ACTION', 'PARTIAL_REFUND', 'FULL_REFUND', 
        'WARNING_ISSUED', 'ACCOUNT_SUSPENDED'
    )),
    resolution_notes TEXT,
    resolved_at TIMESTAMP,
    resolved_by UUID REFERENCES users(id),
    
    -- Refund
    refund_amount INTEGER,
    refund_approved BOOLEAN DEFAULT FALSE,
    
    -- Reference code
    reference_code VARCHAR(50) UNIQUE,
    
    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX idx_disputes_lease ON disputes(lease_id);
CREATE INDEX idx_disputes_opened_by ON disputes(opened_by);
CREATE INDEX idx_disputes_against ON disputes(against_user);
CREATE INDEX idx_disputes_status ON disputes(status);
CREATE INDEX idx_disputes_priority ON disputes(priority);
CREATE INDEX idx_disputes_assigned ON disputes(assigned_to);
CREATE INDEX idx_disputes_reference ON disputes(reference_code);
CREATE INDEX idx_disputes_category ON disputes(category);

-- Composite index for active disputes
CREATE INDEX idx_disputes_active ON disputes(status, priority, created_at) 
    WHERE status NOT IN ('RESOLVED', 'CLOSED');

-- Comments
COMMENT ON TABLE disputes IS 'Dispute resolution system for conflicts between users';
COMMENT ON COLUMN disputes.category IS 'Type of dispute: payment, property, harassment, etc.';
COMMENT ON COLUMN disputes.priority IS 'URGENT for harassment, HIGH for payment issues';
COMMENT ON COLUMN disputes.evidence_urls IS 'Comma-separated list of evidence URLs';
COMMENT ON COLUMN disputes.reference_code IS 'Unique reference (DSP-YYYYMMDD-XXXX)';
