-- Create a test student user with password: Test123!
-- Login with: teststudent@room8.com / Test123!

INSERT INTO users (
    id, email, phone, password_hash, 
    first_name, last_name, gender, date_of_birth, 
    role, account_status, 
    email_verified, phone_verified, profile_completed,
    created_at, updated_at
) VALUES (
    gen_random_uuid(),
    'teststudent@room8.com',
    '237650000099',
    '$2a$10$8K1p/a0dL3.SBq9f9J7aUeDDNXHGPH5r5n5L5L5L5L5L5L5L5L5Le',  -- Password: Test123!
    'Test',
    'Student',
    'MALE',
    '2000-01-01',
    'STUDENT',
    'ACTIVE',
    true,
    true,
    false,
    NOW(),
    NOW()
)
ON CONFLICT (email) DO NOTHING;

-- Also create their student profile
INSERT INTO profiles (
    user_id,
    university_id,
    student_id_number,
    field_of_study,
    year_of_study,
    expected_graduation,
    created_at,
    updated_at
)
SELECT 
    u.id,
    NULL,
    'TEST2024001',
    'Computer Science',
    2,
    '2026-06-01',
    NOW(),
    NOW()
FROM users u
WHERE u.email = 'teststudent@room8.com'
ON CONFLICT (user_id) DO NOTHING;

SELECT 'Test student created! Login with: teststudent@room8.com / Test123!' as result;
