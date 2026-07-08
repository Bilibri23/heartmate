-- Hotfix: the original users.role CHECK constraint (V1) only allowed
-- STUDENT/LANDLORD/ADMIN and was never widened when REALTOR was added as a
-- 4th UserRole — REALTOR registration was failing the DB constraint (503).
DO $$
DECLARE
    existing_constraint text;
BEGIN
    SELECT conname INTO existing_constraint
    FROM pg_constraint
    WHERE conrelid = 'users'::regclass
      AND contype = 'c'
      AND pg_get_constraintdef(oid) ILIKE '%role%STUDENT%';

    IF existing_constraint IS NOT NULL THEN
        EXECUTE format('ALTER TABLE users DROP CONSTRAINT %I', existing_constraint);
    END IF;
END $$;

ALTER TABLE users ADD CONSTRAINT users_role_check
    CHECK (role IN ('STUDENT', 'LANDLORD', 'REALTOR', 'ADMIN'));
