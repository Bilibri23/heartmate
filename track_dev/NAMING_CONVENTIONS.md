# 📝 RoomBuddy Naming Conventions

**Date:** November 28, 2025  
**Status:** ✅ Confirmed

---

## 🎯 Core Terminology

### **User Roles:**

| Original Spec | RoomBuddy | Description |
|---------------|-----------|-------------|
| Seeker | **STUDENT** | Users looking for rooms/roommates |
| Lister | **LANDLORD** | Users offering rooms/properties |
| Moderator | **ADMIN** | Platform administrators |

### **Entities:**

| Original Spec | RoomBuddy | Notes |
|---------------|-----------|-------|
| listings | **PropertyListing** | Keep current naming |
| seeker_profiles | **StudentPreferences** | To be created |
| listing_preferences | **LandlordPreferences** | To be created |
| applications | **RoomApplication** | To be created |

---

## 🔧 Implementation Mapping

### **Database Tables:**
```sql
-- Users
users (role: STUDENT, LANDLORD, ADMIN)

-- Listings
property_listing (owner = landlord)

-- Preferences
student_preferences (user_id references students)
landlord_preferences (listing_id references property_listing)

-- Applications
room_applications (student_id, listing_id)

-- Messaging
conversations (student_id, landlord_id, listing_id)
messages (conversation_id, sender_id, receiver_id)

-- Moderation
content_flags (listing_id, user_id, reporter_id)
```

### **API Endpoints:**
```
# Students (Seekers)
GET  /api/students/{id}/profile
PUT  /api/students/{id}/preferences
GET  /api/students/{id}/matches

# Landlords (Listers)
GET  /api/landlords/{id}/listings
POST /api/landlords/{id}/listings
GET  /api/landlords/{id}/applications

# Listings
GET  /api/listings
POST /api/listings
GET  /api/listings/{id}
PUT  /api/listings/{id}

# Applications
POST /api/applications
GET  /api/applications/{id}
PUT  /api/applications/{id}/status
```

### **Frontend Components:**
```
StudentDashboard (not SeekerDashboard)
LandlordDashboard (not ListerDashboard)
PropertyListingCard (not ListingCard)
StudentPreferencesForm
LandlordAnalyticsPage
```

---

## ✅ Consistency Rules

### **Java Classes:**
- `User` entity with `UserRole` enum (STUDENT, LANDLORD, ADMIN)
- `PropertyListing` entity (not `Listing`)
- `StudentPreferences` (not `SeekerProfile`)
- `RoomApplication` (not `Application`)

### **Database:**
- Table names: snake_case
- `property_listing` (not `listings`)
- `student_preferences` (not `seeker_profiles`)
- `room_applications` (not `applications`)

### **API:**
- Endpoints: kebab-case
- `/api/students` (not `/api/seekers`)
- `/api/landlords` (not `/api/listers`)
- `/api/property-listings` or `/api/listings` (both acceptable)

### **Frontend:**
- Components: PascalCase
- `StudentDashboard`, `LandlordDashboard`
- Routes: kebab-case
- `/admin/student/preferences`
- `/admin/landlord/analytics`

---

## 🎨 UI Text

### **User-Facing Labels:**
- "Students" (not "Seekers")
- "Landlords" (not "Listers")
- "Properties" or "Listings" (both acceptable)
- "Room Applications" (not "Applications")
- "Find Roommates" (student perspective)
- "Find Tenants" (landlord perspective)

### **Button Labels:**
```
Student Actions:
- "Apply for Room"
- "Save to Favorites"
- "Message Landlord"
- "View Matches"

Landlord Actions:
- "Create Listing"
- "View Applications"
- "Message Student"
- "Manage Properties"
```

---

## 📊 Context Mapping

### **Original Spec → RoomBuddy:**

```javascript
// Original Spec
{
  "seeker": {
    "role": "SEEKER",
    "profile": "seeker_profiles",
    "actions": ["search", "apply", "favorite"]
  },
  "lister": {
    "role": "LISTER",
    "profile": "lister_profiles",
    "actions": ["create_listing", "manage_applicants"]
  }
}

// RoomBuddy Implementation
{
  "student": {
    "role": "STUDENT",
    "profile": "student_preferences",
    "actions": ["search", "apply", "favorite"]
  },
  "landlord": {
    "role": "LANDLORD",
    "profile": "landlord_preferences",
    "actions": ["create_listing", "manage_applications"]
  }
}
```

---

## 🔄 Translation Guide

When reading the original spec, translate as follows:

| Spec Term | RoomBuddy Term | Example |
|-----------|----------------|---------|
| Seeker | Student | "Seeker applies" → "Student applies" |
| Lister | Landlord | "Lister creates listing" → "Landlord creates listing" |
| seeker_profiles | student_preferences | Table name |
| listing_preferences | landlord_preferences | Table name |
| Renter | Student | "Renter profile" → "Student profile" |
| Room Owner | Landlord | "Room owner dashboard" → "Landlord dashboard" |

---

## ✅ Benefits of Our Naming

1. **Clarity:** "Student" and "Landlord" are immediately clear
2. **Context:** Fits university/student housing market
3. **Consistency:** Already used throughout codebase
4. **Branding:** Aligns with "RoomBuddy" student focus
5. **Simplicity:** Easier to understand than generic "Seeker/Lister"

---

## 🎯 Summary

**CONFIRMED NAMING:**
- ✅ STUDENT (not Seeker)
- ✅ LANDLORD (not Lister)
- ✅ PropertyListing (not Listing)
- ✅ StudentPreferences (not SeekerProfile)
- ✅ RoomApplication (not Application)

**This is our standard. All new code should follow these conventions.**

---

**Last Updated:** November 28, 2025  
**Status:** ✅ Official Standard
