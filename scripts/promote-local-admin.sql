-- Promote an existing user to platform ADMIN (registration never creates ADMIN).
-- 1. Replace the email with your RoomBay login email.
-- 2. Run against your Postgres DB, e.g.:
--    psql "postgresql://postgres:postgres@localhost:5433/roomconnect_db" -f scripts/promote-local-admin.sql
--
-- Or run the UPDATE line only in any SQL client.

UPDATE users
SET role = 'ADMIN',
    account_status = 'ACTIVE'
WHERE email = 'noblesseb7@gmail.com';

-- Verify:
-- SELECT email, role, account_status FROM users WHERE email = 'your-email@example.com';
