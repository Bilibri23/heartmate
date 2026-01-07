# Sprint 1 Checklist - Quick Reference

## 🎯 Sprint 1 Goals (Week 1)
Complete user authentication, profile management, and student verification foundation.

---

## ✅ Completed Items

### Profile Management (US-004)
- [x] Profile entity created
- [x] Profile repository created
- [x] Profile service with CRUD operations
- [x] Profile controller with Swagger docs
- [x] Profile DTOs (Request/Response)
- [x] Database migration (V2)
- [x] WhatsApp number validation (+237 format)
- [x] Emergency contact fields
- [x] Profile visibility settings (PUBLIC, VERIFIED_ONLY, PRIVATE)
- [x] Profile completion flag
- [x] Exception handling
- [x] Validation

**Status:** ✅ **COMPLETE**

---

## 🚧 In Progress / Blocked

### User Registration (US-001)
- [x] User entity with all fields
- [x] User repository
- [x] Database schema
- [x] BCrypt password encoder
- [ ] Registration controller endpoint
- [ ] Registration service
- [ ] Registration DTOs
- [ ] Phone number validation (+237)
- [ ] Email verification (basic structure)
- [ ] OAuth2/JWT token generation

**Status:** ⚠️ **60% COMPLETE** - Entity ready, API missing

### User Login (US-002)
- [ ] Login controller endpoint
- [ ] Login service
- [ ] OAuth2 authentication
- [ ] JWT access token (1-hour expiry)
- [ ] JWT refresh token (7-day expiry)
- [ ] Token refresh endpoint
- [ ] Last active timestamp update
- [ ] Login with email OR phone

**Status:** ❌ **0% COMPLETE** - Blocked by OAuth2/JWT setup

---

## ❌ Not Started

### Password Reset (US-003)
- [ ] Password reset controller
- [ ] Password reset service
- [ ] Email service configuration
- [ ] Reset token generation
- [ ] Reset token expiration (1 hour)
- [ ] Email template for reset link
- [ ] Reset password endpoint

**Status:** ❌ **NOT STARTED**

### Student Verification (US-008)
- [ ] Student verification entity
- [ ] Student verification repository
- [ ] Student verification service
- [ ] Student verification controller
- [ ] Database migration (V3)
- [ ] File upload service (Cloudinary)
- [ ] Student ID photo upload
- [ ] Verification status enum (PENDING, VERIFIED, REJECTED)
- [ ] Rejection reason field

**Status:** ❌ **NOT STARTED**

---

## 🔧 Infrastructure Components Needed

### Authentication & Security
- [ ] OAuth2 Resource Server configuration
- [ ] JWT token provider service
- [ ] RSA 2048-bit key pair generation
- [ ] UserDetailsService implementation
- [ ] Role-based access control (RBAC)
- [ ] Security filter chain updates
- [ ] Token validation filter

### Services
- [ ] Email service (Spring Mail)
- [ ] File upload service (Cloudinary)
- [ ] Notification service (future)

### Database
- [ ] V3__create_student_verification_table.sql
- [ ] V4__create_roommate_preferences_table.sql (Sprint 2)
- [ ] V5__create_matches_table.sql (Sprint 2)

---

## 📋 Immediate Action Items (Today)

### Priority 1: Authentication (BLOCKING)
1. **Create AuthController** (1 hour)
   ```java
   POST /api/auth/register
   POST /api/auth/login
   POST /api/auth/refresh
   POST /api/auth/forgot-password
   POST /api/auth/reset-password
   ```

2. **Create AuthService** (2 hours)
   - Registration logic
   - Login logic
   - Token generation
   - Password validation

3. **Set up JWT** (3-4 hours)
   - OAuth2 Resource Server
   - JWT token provider
   - RSA key generation
   - Token validation

4. **Create Auth DTOs** (1 hour)
   - RegisterRequest
   - LoginRequest
   - AuthResponse
   - PasswordResetRequest

### Priority 2: Student Verification (3-4 hours)
1. **Database Migration** (30 min)
   - Create student_verification table

2. **Entity & Repository** (1 hour)
   - StudentVerification entity
   - StudentVerificationRepository

3. **Service & Controller** (2 hours)
   - VerificationService
   - VerificationController
   - File upload integration

### Priority 3: Password Reset (2-3 hours)
1. **Email Service** (1 hour)
   - Spring Mail configuration
   - Email templates

2. **Reset Flow** (2 hours)
   - Forgot password endpoint
   - Reset password endpoint
   - Token generation

---

## 🎯 Definition of Done (Per Feature)

### User Registration (US-001)
- [ ] Registration endpoint working
- [ ] Phone number validated (+237 format)
- [ ] Password hashed with BCrypt
- [ ] User saved to database
- [ ] Returns user ID (or JWT token)
- [ ] Email verification link sent (basic)
- [ ] Tested in Postman
- [ ] Documented in Swagger

### User Login (US-002)
- [ ] Login endpoint working
- [ ] Accepts email OR phone
- [ ] Validates password
- [ ] Returns JWT access token (1-hour)
- [ ] Returns JWT refresh token (7-day)
- [ ] Updates last_active timestamp
- [ ] Tested in Postman
- [ ] Documented in Swagger

### Token Refresh (FR-AUTH-004)
- [ ] Refresh endpoint working
- [ ] Validates refresh token
- [ ] Returns new access token
- [ ] Tested in Postman
- [ ] Documented in Swagger

### Password Reset (US-003)
- [ ] Forgot password endpoint working
- [ ] Reset token generated (1-hour expiry)
- [ ] Email sent with reset link
- [ ] Reset password endpoint working
- [ ] Password updated in database
- [ ] Tested in Postman
- [ ] Documented in Swagger

### Student Verification (US-008)
- [ ] Submit verification endpoint working
- [ ] Student ID photo uploaded to Cloudinary
- [ ] Verification saved with PENDING status
- [ ] Database migration created
- [ ] Tested in Postman
- [ ] Documented in Swagger

---

## 📊 Sprint 1 Progress Tracker

### Overall Completion: ~40%

- **Profile Management:** ✅ 100% (Complete)
- **User Registration:** ⚠️ 60% (Entity ready, API needed)
- **User Login:** ❌ 0% (Not started)
- **Password Reset:** ❌ 0% (Not started)
- **Student Verification:** ❌ 0% (Not started)

### Estimated Remaining Time: 15-20 hours

- Authentication: 8-12 hours
- Student Verification: 3-4 hours
- Password Reset: 2-3 hours
- Testing & Bug Fixes: 2-3 hours

---

## 🚨 Critical Blockers

1. **OAuth2/JWT Setup** - Nothing else can work without authentication
2. **Email Service** - Required for password reset and email verification
3. **Cloudinary Service** - Required for student ID photo upload

---

## 💡 Quick Wins (Low Hanging Fruit)

1. **Create Auth DTOs** (30 min) - Easy, no dependencies
2. **Create StudentVerification Entity** (30 min) - Easy, follows existing pattern
3. **Create Database Migration V3** (30 min) - Easy, follows existing pattern
4. **Update ProfileService to enforce visibility** (1 hour) - Easy improvement

---

## 📝 Notes

- Profile management is production-ready
- Focus on authentication first (blocking everything else)
- Consider simplifying OAuth2 if time is tight (use Spring Security JWT)
- Test as you build, don't wait until the end
- Keep Swagger documentation updated

---

**Last Updated:** $(date)
**Next Update:** After authentication implementation

