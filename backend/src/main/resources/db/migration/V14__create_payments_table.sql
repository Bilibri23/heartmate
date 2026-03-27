-- =====================================================
-- PAYMENTS TABLE
-- Mobile Money payment tracking for Cameroon (MTN MOMO, Orange Money)
-- =====================================================

CREATE TABLE payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lease_id UUID REFERENCES leases(id) ON DELETE SET NULL,
    payer_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    recipient_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    
    -- Payment Details
    amount INTEGER NOT NULL,
    payment_type VARCHAR(30) NOT NULL CHECK (payment_type IN (
        'DEPOSIT', 'FIRST_MONTH_RENT', 'MONTHLY_RENT', 
        'AGENCY_FEE', 'INITIAL_PAYMENT', 'REFUND'
    )),
    payment_method VARCHAR(30) NOT NULL CHECK (payment_method IN (
        'MTN_MOMO', 'ORANGE_MONEY', 'EXPRESS_UNION', 'BANK_TRANSFER', 'CASH'
    )),
    
    -- Mobile Money specific
    momo_phone_number VARCHAR(20),
    momo_transaction_id VARCHAR(100),
    momo_reference VARCHAR(100),
    
    -- Proof of payment
    payment_proof_url TEXT,
    payer_notes TEXT,
    
    -- Status
    status VARCHAR(20) DEFAULT 'PENDING' CHECK (status IN (
        'PENDING', 'SUBMITTED', 'VERIFIED', 'REJECTED', 'FAILED', 'REFUNDED', 'CANCELLED'
    )),
    
    -- Verification
    verified_by UUID REFERENCES users(id),
    verified_at TIMESTAMP,
    verification_notes TEXT,
    rejection_reason TEXT,
    
    -- Platform fees
    platform_fee INTEGER,
    landlord_payout INTEGER,
    
    -- Payout to landlord
    payout_status VARCHAR(20) DEFAULT 'PENDING' CHECK (payout_status IN (
        'PENDING', 'PROCESSING', 'COMPLETED', 'FAILED'
    )),
    payout_at TIMESTAMP,
    payout_reference VARCHAR(100),
    
    -- Reference code
    reference_code VARCHAR(50) UNIQUE NOT NULL,
    
    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX idx_payments_lease ON payments(lease_id);
CREATE INDEX idx_payments_payer ON payments(payer_id);
CREATE INDEX idx_payments_recipient ON payments(recipient_id);
CREATE INDEX idx_payments_status ON payments(status);
CREATE INDEX idx_payments_reference ON payments(reference_code);
CREATE INDEX idx_payments_method ON payments(payment_method);
CREATE INDEX idx_payments_payout_status ON payments(payout_status);

-- Comments
COMMENT ON TABLE payments IS 'Mobile Money payment tracking for Cameroon market';
COMMENT ON COLUMN payments.payment_method IS 'MTN_MOMO and ORANGE_MONEY are primary methods in Cameroon';
COMMENT ON COLUMN payments.platform_fee IS 'RoomBay commission (5% of payment)';
COMMENT ON COLUMN payments.reference_code IS 'Unique reference for payment (PAY-YYYYMMDD-XXXXX)';
