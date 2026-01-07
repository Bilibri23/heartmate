-- =====================================================
-- TEST DATA FOR RECOMMENDATION SYSTEM
-- Run this in your PostgreSQL database
-- =====================================================

-- First, let's get your current student user ID
-- You'll need to replace 'YOUR_STUDENT_USER_ID' below with your actual UUID

-- Create a test landlord user (if needed)
INSERT INTO users (id, email, phone, password_hash, first_name, last_name, gender, date_of_birth, role, account_status, email_verified, phone_verified, profile_completed, mtn_momo_number, momo_name, created_at, updated_at)
VALUES 
    (gen_random_uuid(), 'landlord1@test.com', '237670000001', '$2a$10$dummyhashedpassword', 'Jean', 'Landlord', 'MALE', '1980-01-01', 'LANDLORD', 'ACTIVE', true, true, true, '670000001', 'Jean Landlord', NOW(), NOW()),
    (gen_random_uuid(), 'landlord2@test.com', '237670000002', '$2a$10$dummyhashedpassword', 'Marie', 'Owner', 'FEMALE', '1985-05-15', 'LANDLORD', 'ACTIVE', true, true, true, '670000002', 'Marie Owner', NOW(), NOW())
ON CONFLICT (email) DO NOTHING;

-- Get landlord IDs (we'll use them below)
DO $$
DECLARE
    landlord1_id UUID;
    landlord2_id UUID;
BEGIN
    -- Get landlord IDs
    SELECT id INTO landlord1_id FROM users WHERE email = 'landlord1@test.com';
    SELECT id INTO landlord2_id FROM users WHERE email = 'landlord2@test.com';

    -- Insert test listings
    -- Listing 1: Cheap studio in Ngoa-Ekellé (near Yaoundé I)
    INSERT INTO property_listings (
        id, landlord_id, title, description, property_type, 
        rent_amount, deposit, agency_fees,
        region, city, neighborhood, address,
        latitude, longitude, distance_to_university,
        bedrooms, bathrooms, square_meters,
        amenities, available_from,
        status, verified, featured, landlord_whatsapp,
        views_count, favorites_count,
        created_at, updated_at
    ) VALUES (
        gen_random_uuid(), landlord1_id,
        'Cozy Studio Near University of Yaoundé I',
        'Perfect for students! Furnished studio with WiFi, electricity, and water included. Walking distance to campus.',
        'STUDIO',
        35000, 70000, 15000,
        'Centre', 'Yaoundé', 'Ngoa-Ekellé', 'Behind Total Station, Ngoa-Ekellé',
        3.8480, 11.5021, 1.5,
        1, 1, 25,
        ARRAY['WiFi', 'Water', 'Electricity', 'Furnished', 'Security'],
        CURRENT_DATE,
        'ACTIVE', true, false, '237670000001',
        15, 3,
        NOW(), NOW()
    );

    -- Listing 2: Moderate private room in Ngoa-Ekellé
    INSERT INTO property_listings (
        id, landlord_id, title, description, property_type,
        rent_amount, deposit, agency_fees,
        region, city, neighborhood, address,
        latitude, longitude, distance_to_university,
        bedrooms, bathrooms, square_meters,
        amenities, available_from,
        status, verified, featured, landlord_whatsapp,
        views_count, favorites_count,
        created_at, updated_at
    ) VALUES (
        gen_random_uuid(), landlord1_id,
        'Modern Private Room with Balcony',
        'Spacious private room in a shared apartment. Great neighborhood, close to shops and transport.',
        'PRIVATE_ROOM',
        45000, 90000, 20000,
        'Centre', 'Yaoundé', 'Ngoa-Ekellé', 'Carrefour Nlongkak, Ngoa-Ekellé',
        3.8490, 11.5031, 2.0,
        1, 1, 20,
        ARRAY['WiFi', 'Water', 'Electricity', 'Parking', 'Kitchen'],
        CURRENT_DATE + INTERVAL '7 days',
        'ACTIVE', true, true, '237670000001',
        25, 5,
        NOW(), NOW()
    );

    -- Listing 3: Expensive apartment in Bastos (too expensive for typical student)
    INSERT INTO property_listings (
        id, landlord_id, title, description, property_type,
        rent_amount, deposit, agency_fees,
        region, city, neighborhood, address,
        latitude, longitude, distance_to_university,
        bedrooms, bathrooms, square_meters,
        amenities, available_from,
        status, verified, featured, landlord_whatsapp,
        views_count, favorites_count,
        created_at, updated_at
    ) VALUES (
        gen_random_uuid(), landlord2_id,
        'Luxury 2-Bedroom Apartment in Bastos',
        'High-end apartment with modern amenities, 24/7 security, and pool access.',
        'APARTMENT',
        150000, 300000, 50000,
        'Centre', 'Yaoundé', 'Bastos', 'Avenue Kennedy, Bastos',
        3.8650, 11.5180, 5.5,
        2, 2, 80,
        ARRAY['WiFi', 'Water', 'Electricity', 'Parking', 'Security', 'Pool', 'Gym', 'Generator'],
        CURRENT_DATE + INTERVAL '14 days',
        'ACTIVE', true, true, '237670000002',
        50, 8,
        NOW(), NOW()
    );

    -- Listing 4: Affordable shared room in Messa
    INSERT INTO property_listings (
        id, landlord_id, title, description, property_type,
        rent_amount, deposit, agency_fees,
        region, city, neighborhood, address,
        latitude, longitude, distance_to_university,
        bedrooms, bathrooms, square_meters,
        amenities, available_from,
        status, verified, featured, landlord_whatsapp,
        views_count, favorites_count,
        created_at, updated_at
    ) VALUES (
        gen_random_uuid(), landlord2_id,
        'Budget-Friendly Shared Room in Messa',
        'Great for students on a budget. Shared with one other student. Clean and safe.',
        'SHARED_ROOM',
        25000, 50000, 10000,
        'Centre', 'Yaoundé', 'Messa', 'Quartier Messa, near market',
        3.8520, 11.5100, 3.0,
        1, 1, 15,
        ARRAY['Water', 'Electricity', 'Security'],
        CURRENT_DATE,
        'ACTIVE', true, false, '237670000002',
        10, 2,
        NOW(), NOW()
    );

    -- Listing 5: Studio in Soa (near University of Yaoundé II)
    INSERT INTO property_listings (
        id, landlord_id, title, description, property_type,
        rent_amount, deposit, agency_fees,
        region, city, neighborhood, address,
        latitude, longitude, distance_to_university,
        bedrooms, bathrooms, square_meters,
        amenities, available_from,
        status, verified, featured, landlord_whatsapp,
        views_count, favorites_count,
        created_at, updated_at
    ) VALUES (
        gen_random_uuid(), landlord1_id,
        'Student Studio Near UY II Campus',
        'Perfect location for Yaoundé II students. Quiet neighborhood, WiFi included.',
        'STUDIO',
        40000, 80000, 15000,
        'Centre', 'Yaoundé', 'Soa', 'Campus Soa, University Road',
        3.9100, 11.4500, 0.5,
        1, 1, 22,
        ARRAY['WiFi', 'Water', 'Electricity', 'Furnished'],
        CURRENT_DATE + INTERVAL '3 days',
        'ACTIVE', true, false, '237670000001',
        20, 4,
        NOW(), NOW()
    );

    RAISE NOTICE 'Test listings created successfully!';
END $$;

-- =====================================================
-- Now set YOUR student preferences
-- Replace 'YOUR_EMAIL@test.com' with your actual student email
-- =====================================================

DO $$
DECLARE
    student_id UUID;
BEGIN
    -- Get your student user ID (replace with your actual email)
    SELECT id INTO student_id FROM users WHERE role = 'STUDENT' LIMIT 1;
    
    IF student_id IS NULL THEN
        RAISE EXCEPTION 'No student user found. Please use your actual student email.';
    END IF;

    -- Insert/Update student preferences
    INSERT INTO roommate_preferences (
        id, user_id,
        min_budget, max_budget,
        preferred_locations, max_distance_from_campus,
        cleanliness_level, noise_tolerance, social_level,
        sleep_schedule, study_time_preference,
        smoking, drinking, pets, guests, cooking,
        deal_breakers,
        preferred_gender, min_age, max_age,
        same_university, same_faculty,
        looking_for_roommate,
        created_at, updated_at
    ) VALUES (
        gen_random_uuid(), student_id,
        30000, 60000,
        ARRAY['Ngoa-Ekellé', 'Messa', 'Soa'], 3.0,
        4, 3, 3,
        'FLEXIBLE', 'EVENING',
        false, false, false, true, true,
        'smoking, loud music',
        'ANY', 18, 30,
        true, false,
        true,
        NOW(), NOW()
    )
    ON CONFLICT (user_id) DO UPDATE SET
        min_budget = 30000,
        max_budget = 60000,
        preferred_locations = ARRAY['Ngoa-Ekellé', 'Messa', 'Soa'],
        max_distance_from_campus = 3.0,
        looking_for_roommate = true,
        updated_at = NOW();

    RAISE NOTICE 'Student preferences set for user: %', student_id;
END $$;

-- =====================================================
-- VERIFICATION
-- =====================================================

-- Check what was created
SELECT 'LANDLORDS:' as info, COUNT(*) as count FROM users WHERE role = 'LANDLORD';
SELECT 'LISTINGS:' as info, COUNT(*) as count FROM property_listings WHERE status = 'ACTIVE' AND verified = true;
SELECT 'STUDENT PREFERENCES:' as info, COUNT(*) as count FROM roommate_preferences;

-- Show the listings
SELECT 
    title,
    property_type,
    rent_amount,
    neighborhood,
    city,
    verified,
    status
FROM property_listings
WHERE status = 'ACTIVE' AND verified = true
ORDER BY rent_amount;

COMMIT;
