-- =====================================================
-- STUDENT VERIFICATION TABLE
-- =====================================================
CREATE TABLE student_verification (
                                      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                      user_id UUID NOT NULL UNIQUE,
                                      university VARCHAR(100) NOT NULL,
                                      student_id VARCHAR(50) NOT NULL,
                                      faculty VARCHAR(100),
                                      department VARCHAR(100),
                                      year_of_study INTEGER,
                                      student_id_photo_url VARCHAR(500),
                                      status VARCHAR(20) DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'VERIFIED', 'REJECTED')),
                                      rejection_reason TEXT,
                                      verified_by UUID,
                                      verified_at TIMESTAMP,
                                      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                                      updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                                      CONSTRAINT fk_student_verification_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                                      CONSTRAINT fk_student_verification_verifier FOREIGN KEY (verified_by) REFERENCES users(id)
);

-- Create indexes
CREATE INDEX idx_student_verification_user_id ON student_verification(user_id);
CREATE INDEX idx_student_verification_status ON student_verification(status);

-- Comments
COMMENT ON TABLE student_verification IS 'Student verification requests';
COMMENT ON COLUMN student_verification.status IS 'PENDING: awaiting review, VERIFIED: approved, REJECTED: denied';
