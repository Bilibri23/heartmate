-- One-off local Docker DB: admin for RAG ingest testing (password: NoblesseRoombay@2026)
INSERT INTO users (
  id, email, phone, password_hash, first_name, last_name, gender, role, account_status,
  email_verified, phone_verified, profile_completed, created_at
) VALUES (
  gen_random_uuid(),
  'noblesseb7@gmail.com',
  '+237699999991',
  '$2b$10$44.ThzYxlCabvGOAxwM8FOfmPGQueOyBgMiwhFNxpWlEtWf2Z6zwS',
  'RAG', 'Admin', 'MALE', 'ADMIN', 'ACTIVE',
  true, true, true, NOW()
)
ON CONFLICT (email) DO UPDATE SET
  password_hash = EXCLUDED.password_hash,
  role = 'ADMIN',
  account_status = 'ACTIVE',
  email_verified = true,
  phone_verified = true;
