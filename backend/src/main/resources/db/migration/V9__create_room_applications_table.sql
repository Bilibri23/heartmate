-- Create room_applications table
CREATE TABLE room_applications (
    id UUID PRIMARY KEY,
    listing_id UUID NOT NULL,
    student_id UUID NOT NULL,
    message TEXT NOT NULL,
    move_in_date DATE NOT NULL,
    lease_duration_months INTEGER NOT NULL CHECK (lease_duration_months BETWEEN 1 AND 24),
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    landlord_response TEXT,
    landlord_viewed_at TIMESTAMP,
    reviewed_by UUID,
    reviewed_at TIMESTAMP,
    rejection_reason TEXT,
    expires_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    
    -- Foreign keys
    CONSTRAINT fk_application_listing FOREIGN KEY (listing_id) REFERENCES property_listings(id) ON DELETE CASCADE,
    CONSTRAINT fk_application_student FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_application_reviewer FOREIGN KEY (reviewed_by) REFERENCES users(id) ON DELETE SET NULL,
    
    -- Unique constraint
    CONSTRAINT unique_student_listing UNIQUE (student_id, listing_id)
);

-- Create indexes
CREATE INDEX idx_applications_student ON room_applications(student_id);
CREATE INDEX idx_applications_listing ON room_applications(listing_id);
CREATE INDEX idx_applications_status ON room_applications(status);
CREATE INDEX idx_applications_expires ON room_applications(expires_at);
CREATE INDEX idx_applications_created ON room_applications(created_at DESC);
CREATE INDEX idx_applications_reviewed ON room_applications(reviewed_at DESC);
CREATE INDEX idx_applications_landlord_lookup ON room_applications(listing_id, status);
