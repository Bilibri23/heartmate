-- Add gov ID fields for generalized tenant verification (not just student)
-- Keep university/student_id for backward compatibility with existing records

ALTER TABLE student_verification
  ALTER COLUMN university DROP NOT NULL,
  ALTER COLUMN student_id DROP NOT NULL;

ALTER TABLE student_verification ADD COLUMN id_type VARCHAR(50);
ALTER TABLE student_verification ADD COLUMN id_number VARCHAR(100);
ALTER TABLE student_verification ADD COLUMN id_photo_url VARCHAR(500);
ALTER TABLE student_verification ADD COLUMN selfie_photo_url VARCHAR(500);

COMMENT ON COLUMN student_verification.id_type IS 'Gov ID type: PASSPORT, NATIONAL_ID, DRIVERS_LICENSE';
COMMENT ON COLUMN student_verification.id_number IS 'ID document number';
COMMENT ON COLUMN student_verification.id_photo_url IS 'Photo of gov ID document';
COMMENT ON COLUMN student_verification.selfie_photo_url IS 'Selfie for liveness verification';
