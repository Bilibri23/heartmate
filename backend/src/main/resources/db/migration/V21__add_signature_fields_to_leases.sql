-- Add digital signature fields to leases table
ALTER TABLE leases ADD COLUMN IF NOT EXISTS student_signature TEXT;
ALTER TABLE leases ADD COLUMN IF NOT EXISTS landlord_signature TEXT;
ALTER TABLE leases ADD COLUMN IF NOT EXISTS student_signature_type VARCHAR(20);
ALTER TABLE leases ADD COLUMN IF NOT EXISTS landlord_signature_type VARCHAR(20);

COMMENT ON COLUMN leases.student_signature IS 'Base64 encoded image of student signature';
COMMENT ON COLUMN leases.landlord_signature IS 'Base64 encoded image of landlord signature';
COMMENT ON COLUMN leases.student_signature_type IS 'Type of signature: drawn or typed';
COMMENT ON COLUMN leases.landlord_signature_type IS 'Type of signature: drawn or typed';
