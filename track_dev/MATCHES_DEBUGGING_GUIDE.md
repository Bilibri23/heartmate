# Matches Feature Debugging Guide

## Date: Nov 22, 2025 - 10:11 PM

## Issue: "Nothing Found" Despite Similar Preferences

### Matching Algorithm Overview

The matching system uses a **weighted compatibility score** with a **60% minimum threshold**.

#### Score Weights:
- **Budget**: 30%
- **Lifestyle**: 25% (cleanliness, noise, social level)
- **Schedule**: 20% (sleep schedule, study time)
- **Location**: 15% (preferred locations, distance from campus)
- **Habits**: 10% (smoking, drinking, pets, guests)

**Minimum Compatibility**: 60% to create a match

### Common Reasons for "Nothing Found"

#### 1. **lookingForRoommate Flag Not Set**
**Most Common Issue!**

```java
// In MatchingService.java line 57-60
if (!userPreferences.getLookingForRoommate()) {
    log.info("User {} is not looking for roommate", userId);
    return new ArrayList<>(); // Returns empty!
}
```

**Check:** Both students must have `lookingForRoommate = true`

#### 2. **Match Already Exists**
```java
// Line 76-78
if (matchRepository.existsMatchBetweenUsers(userId, otherUserId)) {
    continue; // Skips if match already created
}
```

**Check:** Database for existing matches between the two users

#### 3. **Deal-Breakers Triggered**
```java
// Line 81-83
if (hasDealBreakers(userPreferences, otherPreferences)) {
    continue; // Skips this potential match
}
```

**Deal-breakers that block matches:**
- "smoking" in deal-breakers + other user smokes = NO MATCH
- "drinking" in deal-breakers + other user drinks = NO MATCH
- "pets" in deal-breakers + other user has pets = NO MATCH
- "parties" in deal-breakers + other user allows guests = NO MATCH

#### 4. **Compatibility Score Below 60%**
```java
// Line 89
if (score.getOverallScore() >= MIN_COMPATIBILITY_THRESHOLD) {
    // Only creates match if score >= 60%
}
```

### Detailed Score Calculation

#### Budget Score (30% weight)
```java
// Requires BOTH users to have minBudget and maxBudget set
if (pref1.getMinBudget() == null || pref1.getMaxBudget() == null ||
    pref2.getMinBudget() == null || pref2.getMaxBudget() == null) {
    return 50; // Neutral score
}
```

**Best Match:** Overlapping budget ranges
- Example: User1 (50k-100k) + User2 (75k-125k) = High score
- Example: User1 (50k-100k) + User2 (200k-300k) = Low score

#### Lifestyle Score (25% weight)
Compares 3 factors (1-5 scale):
- **Cleanliness Level**: Difference of 1 = 75%, Difference of 2 = 50%
- **Noise Tolerance**: Same calculation
- **Social Level**: Same calculation

**Best Match:** All three within 1 point of each other

#### Schedule Score (20% weight)
- **Sleep Schedule**: Same = 100%, One FLEXIBLE = 75%, Different = 25%
- **Study Time**: Same = 100%, One FLEXIBLE = 75%, Different = 50%

**Best Match:** Same schedules or at least one FLEXIBLE

#### Location Score (15% weight)
- **Common Locations**: +50 points if any overlap
- **Distance from Campus**: Both < 5km = +20, Both < 10km = +10

**Best Match:** At least one common preferred location

#### Habits Score (10% weight)
- **Smoking**: Same = 100%, Different = 0%
- **Drinking**: Same = 100%, Different = 50%
- **Pets**: Same = 100%, Different = 50%
- **Guests**: Same = 100%, Different = 75%

**Best Match:** All habits match

### Score Penalties

#### Gender Preference Mismatch
```java
// Line 255-258
if (!matchesGenderPreference(pref1, user2.getGender()) ||
    !matchesGenderPreference(pref2, user1.getGender())) {
    overallScore *= 0.5; // 50% REDUCTION!
}
```

**Example:** If overall score is 80%, but gender doesn't match → 40% (FAILS threshold)

#### Age Preference Mismatch
```java
// Line 261-264
if (!matchesAgePreference(pref1, user2.getDateOfBirth()) ||
    !matchesAgePreference(pref2, user1.getDateOfBirth())) {
    overallScore *= 0.7; // 30% REDUCTION
}
```

**Example:** If overall score is 80%, but age doesn't match → 56% (FAILS threshold)

## Debugging Steps

### Step 1: Check Database Preferences

```sql
-- Check if both users have preferences
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
WHERE user_id IN ('user1_uuid', 'user2_uuid');
```

**Check:**
- ✅ `looking_for_roommate = true` for BOTH
- ✅ `min_budget` and `max_budget` are NOT NULL
- ✅ `preferred_locations` has at least one location
- ✅ All lifestyle fields are set (1-5)
- ✅ `deal_breakers` is empty or doesn't conflict

### Step 2: Check User Profiles

```sql
-- Check user details for age/gender matching
SELECT 
    id,
    first_name,
    gender,
    date_of_birth
FROM users
WHERE id IN ('user1_uuid', 'user2_uuid');
```

**Check:**
- ✅ `gender` is set
- ✅ `date_of_birth` is set
- ✅ Ages fall within each other's `min_age` and `max_age` preferences

### Step 3: Check Existing Matches

```sql
-- Check if match already exists
SELECT * FROM matches
WHERE (user1_id = 'user1_uuid' AND user2_id = 'user2_uuid')
   OR (user1_id = 'user2_uuid' AND user2_id = 'user1_uuid');
```

**If exists:** Match already created, won't create duplicate

### Step 4: Manual Score Calculation

Use this checklist to estimate compatibility:

#### Budget (30%)
- [ ] Both have min/max budget set?
- [ ] Budgets overlap? (High score)
- [ ] Budgets close? (Medium score)
- [ ] Budgets far apart? (Low score)

#### Lifestyle (25%)
- [ ] Cleanliness levels within 1 point?
- [ ] Noise tolerance within 1 point?
- [ ] Social level within 1 point?

#### Schedule (20%)
- [ ] Same sleep schedule OR one FLEXIBLE?
- [ ] Same study time OR one FLEXIBLE?

#### Location (15%)
- [ ] At least one common preferred location?
- [ ] Both close to campus (< 10km)?

#### Habits (10%)
- [ ] Smoking preference matches?
- [ ] Drinking preference matches?
- [ ] Pets preference matches?
- [ ] Guests preference matches?

#### Penalties
- [ ] Gender preferences match? (If not: -50%)
- [ ] Age preferences match? (If not: -30%)

**Estimated Score:**
```
Base = (Budget*0.3 + Lifestyle*0.25 + Schedule*0.2 + Location*0.15 + Habits*0.1)
Final = Base * GenderPenalty * AgePenalty
```

**Must be >= 60% to match!**

## Common Fixes

### Fix 1: Ensure lookingForRoommate is True

**Frontend:** Check PreferencesWizard.jsx
```javascript
// Line 39 in initial state
lookingForRoommate: true,  // Must be true!
```

**Backend:** Check database
```sql
UPDATE roommate_preferences 
SET looking_for_roommate = true 
WHERE user_id IN ('user1_uuid', 'user2_uuid');
```

### Fix 2: Set All Required Fields

**Minimum Required:**
- ✅ `minBudget` and `maxBudget`
- ✅ At least one `preferredLocation`
- ✅ `cleanlinessLevel`, `noiseTolerance`, `socialLevel` (1-5)
- ✅ `sleepSchedule` and `studyTimePreference`
- ✅ `smoking`, `drinking`, `pets`, `guests` (true/false)

### Fix 3: Avoid Deal-Breakers

**Don't add deal-breakers unless necessary:**
- Remove "smoking" from deal-breakers if testing
- Remove "drinking" from deal-breakers if testing
- Keep deal-breakers field empty for testing

### Fix 4: Set Compatible Gender/Age Preferences

**For testing, use:**
- `preferredGender = 'ANY'`
- `minAge = 18`
- `maxAge = 100`

This avoids the 50% and 30% penalties.

### Fix 5: Ensure Overlapping Preferences

**For guaranteed match (testing):**

**Student 1:**
```json
{
  "minBudget": 50000,
  "maxBudget": 100000,
  "preferredLocations": ["Bastos", "Ngoa-Ekelle"],
  "cleanlinessLevel": 4,
  "noiseTolerance": 3,
  "socialLevel": 4,
  "sleepSchedule": "NIGHT_OWL",
  "studyTimePreference": "EVENING",
  "smoking": false,
  "drinking": false,
  "pets": false,
  "guests": true,
  "preferredGender": "ANY",
  "minAge": 18,
  "maxAge": 100,
  "lookingForRoommate": true
}
```

**Student 2:**
```json
{
  "minBudget": 60000,
  "maxBudget": 120000,
  "preferredLocations": ["Bastos", "Mvog-Ada"],  // "Bastos" overlaps!
  "cleanlinessLevel": 4,  // Same
  "noiseTolerance": 3,    // Same
  "socialLevel": 4,       // Same
  "sleepSchedule": "NIGHT_OWL",  // Same
  "studyTimePreference": "EVENING",  // Same
  "smoking": false,  // Same
  "drinking": false,  // Same
  "pets": false,     // Same
  "guests": true,    // Same
  "preferredGender": "ANY",
  "minAge": 18,
  "maxAge": 100,
  "lookingForRoommate": true
}
```

**Expected Score:** ~95% (well above 60% threshold)

## Testing Workflow

1. **Create Student 1**
   - Sign up
   - Complete preferences with values above
   - Verify in database

2. **Create Student 2**
   - Sign up
   - Complete preferences with values above
   - Verify in database

3. **Find Matches (Student 1)**
   - Go to Matches page
   - Click "Find Matches"
   - Should see Student 2 with ~95% compatibility

4. **Find Matches (Student 2)**
   - Login as Student 2
   - Go to Matches page
   - Click "Find Matches"
   - Should see Student 1 with ~95% compatibility

## Backend Logs to Check

Enable debug logging:
```java
log.info("Finding matches for user: {}", userId);
log.info("User preferences: {}", userPreferences);
log.info("Found {} potential candidates", allPreferences.size());
log.info("Compatibility score: {}", score.getOverallScore());
log.info("Found {} matches for user: {}", savedMatches.size(), userId);
```

Look for:
- "User is not looking for roommate" → Fix lookingForRoommate
- "Match already exists" → Check database
- Low compatibility scores → Adjust preferences

## Summary

**Most likely issue:** `lookingForRoommate = false` or one of the students

**Quick fix for testing:**
1. Set `lookingForRoommate = true` for both
2. Use identical preferences
3. Set `preferredGender = 'ANY'`
4. Remove all deal-breakers
5. Click "Find Matches"

**Should work immediately!** 🎉
