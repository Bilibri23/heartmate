-- See all your users
SELECT id, email, first_name, last_name, role FROM users;

-- Pick ANY user and make them admin (replace with actual email)
UPDATE users
SET role = 'ADMIN',
    account_status = 'ACTIVE',
    email_verified = true,
    phone_verified = true
WHERE email = 'mam@gmail.com';

-- Verify
SELECT email, first_name, last_name, role, account_status
FROM users
WHERE role = 'ADMIN';