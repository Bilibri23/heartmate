CREATE EXTENSION IF NOT EXISTS pgcrypto;

DO $$
DECLARE
    pk_name text;
BEGIN
    SELECT conname INTO pk_name
    FROM pg_constraint
    WHERE conrelid = 'audit_log'::regclass
      AND contype = 'p';

    IF pk_name IS NOT NULL THEN
        EXECUTE format('ALTER TABLE audit_log DROP CONSTRAINT IF EXISTS %I', pk_name);
    END IF;
END $$;

ALTER TABLE audit_log
    ALTER COLUMN id TYPE uuid USING gen_random_uuid(),
    ALTER COLUMN id SET DEFAULT gen_random_uuid(),
    ALTER COLUMN id SET NOT NULL;

ALTER TABLE audit_log
    ADD PRIMARY KEY (id);

ALTER TABLE audit_log
    ALTER COLUMN user_id TYPE uuid USING (
        CASE
            WHEN user_id::text ~* '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$'
            THEN user_id::text::uuid
            ELSE NULL
        END
    ),
    ALTER COLUMN entity_id TYPE uuid USING (
        CASE
            WHEN entity_id::text ~* '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$'
            THEN entity_id::text::uuid
            ELSE NULL
        END
    );

ALTER TABLE audit_log
    ADD COLUMN IF NOT EXISTS user_email varchar(255),
    ADD COLUMN IF NOT EXISTS user_role varchar(64),
    ADD COLUMN IF NOT EXISTS ip_address varchar(128),
    ADD COLUMN IF NOT EXISTS user_agent text,
    ADD COLUMN IF NOT EXISTS status varchar(32) NOT NULL DEFAULT 'SUCCESS',
    ADD COLUMN IF NOT EXISTS error_message text;

CREATE INDEX IF NOT EXISTS idx_audit_user_id ON audit_log(user_id);
CREATE INDEX IF NOT EXISTS idx_audit_action ON audit_log(action);
CREATE INDEX IF NOT EXISTS idx_audit_entity_type ON audit_log(entity_type);
CREATE INDEX IF NOT EXISTS idx_audit_entity ON audit_log(entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_audit_created_at ON audit_log(created_at DESC);
