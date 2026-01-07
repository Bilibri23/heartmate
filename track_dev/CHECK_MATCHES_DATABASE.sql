-- Check Matches Database - Debugging Script
-- Date: Nov 22, 2025 - 10:43 PM
-- User ID: d0c39663-702a-46da-96af-5230ee369c87

-- ===================================
-- STEP 1: Check User Details
-- ===================================
SELECT 
    id,
    first_name,
    last_name,
    email,
    gender,
    date_of_birth,
    role
FROM users
WHERE id = 'd0c39663-702a-46da-96af-5230ee369c87';

-- ===================================
-- STEP 2: Check Preferences for This User
-- ===================================
SELECT 
    user_id,
    looking_for_roommate,
    min_budget,
    max_budget,
    preferred_locations,
    cleanliness_level,
    noise_tolerance,
    social_level,
    sleep_schedule,
    study_time_preference,
    smoking,
    drinking,
    pets,
    guests,
    deal_breakers,
    preferred_gender,
    min_age,
    max_age
FROM roommate_preferences
WHERE user_id = 'd0c39663-702a-46da-96af-5230ee369c87';

-- ===================================
-- STEP 3: Check ALL Students with Preferences
-- ===================================
SELECT 
    rp.user_id,
    u.first_name,
    u.last_name,
    rp.looking_for_roommate,
    rp.min_budget,
    rp.max_budget,
    rp.preferred_locations
FROM roommate_preferences rp
JOIN users u ON rp.user_id = u.id
WHERE u.role = 'STUDENT'
ORDER BY rp.looking_for_roommate DESC, u.first_name;

-- ===================================
-- STEP 4: Check Existing Matches
-- ===================================
SELECT 
    m.id,
    m.user1_id,
    u1.first_name as user1_name,
    m.user2_id,
    u2.first_name as user2_name,
    m.compatibility_score,
    m.status,
    m.created_at
FROM matches m
JOIN users u1 ON m.user1_id = u1.id
JOIN users u2 ON m.user2_id = u2.id
WHERE m.user1_id = 'd0c39663-702a-46da-96af-5230ee369c87'
   OR m.user2_id = 'd0c39663-702a-46da-96af-5230ee369c87';

-- ===================================
-- STEP 5: Check ALL Matches in System
-- ===================================
SELECT 
    m.id,
    u1.first_name as user1_name,
    u2.first_name as user2_name,
    m.compatibility_score,
    m.status,
    m.created_at
FROM matches m
JOIN users u1 ON m.user1_id = u1.id
JOIN users u2 ON m.user2_id = u2.id
ORDER BY m.created_at DESC;

-- ===================================
-- STEP 6: Find Potential Matches Manually
-- ===================================
-- This shows all students looking for roommates (excluding current user)
SELECT 
    u.id,
    u.first_name,
    u.last_name,
    rp.looking_for_roommate,
    rp.min_budget,
    rp.max_budget,
    rp.preferred_locations,
    rp.cleanliness_level,
    rp.noise_tolerance,
    rp.social_level
FROM users u
JOIN roommate_preferences rp ON u.id = rp.user_id
WHERE u.role = 'STUDENT'
  AND rp.looking_for_roommate = true
  AND u.id != 'd0c39663-702a-46da-96af-5230ee369c87'
ORDER BY u.first_name;

-- ===================================
-- DIAGNOSTIC QUERIES
-- ===================================

-- Count students with preferences
SELECT COUNT(*) as total_students_with_preferences
FROM roommate_preferences rp
JOIN users u ON rp.user_id = u.id
WHERE u.role = 'STUDENT';

-- Count students looking for roommates
SELECT COUNT(*) as students_looking_for_roommates
FROM roommate_preferences rp
JOIN users u ON rp.user_id = u.id
WHERE u.role = 'STUDENT'
  AND rp.looking_for_roommate = true;

-- Count total matches in system
SELECT COUNT(*) as total_matches
FROM matches;

-- ===================================
-- FIXES (if needed)
-- ===================================

-- FIX 1: Set looking_for_roommate to true
-- Uncomment to run:
-- UPDATE roommate_preferences
-- SET looking_for_roommate = true
-- WHERE user_id = 'd0c39663-702a-46da-96af-5230ee369c87';

-- FIX 2: Delete existing matches for this user (to test again)
-- Uncomment to run:
-- DELETE FROM matches
-- WHERE user1_id = 'd0c39663-702a-46da-96af-5230ee369c87'
--    OR user2_id = 'd0c39663-702a-46da-96af-5230ee369c87';

-- FIX 3: Delete ALL matches (nuclear option for testing)
-- Uncomment to run:
-- DELETE FROM matches;

-- ===================================
-- EXPECTED RESULTS
-- ===================================

-- STEP 1: Should return 1 row with user details
-- STEP 2: Should return 1 row with preferences, looking_for_roommate = true
-- STEP 3: Should return at least 2 rows (current user + other student)
-- STEP 4: May return 0 rows if no matches exist yet (this is OK)
-- STEP 5: Shows all matches in system
-- STEP 6: Should return at least 1 row (the other student)

-- If STEP 6 returns 0 rows:
--   - No other students are looking for roommates
--   - Create another student account and set preferences

-- If STEP 6 returns rows but STEP 4 returns 0:
--   - Matches haven't been created yet
--   - Click "Find Matches" button
--   - Check backend logs for compatibility scores
