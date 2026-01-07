# Phone/WhatsApp Verification Implementation

## ✅ Implementation Complete

### Overview
Implemented WhatsApp OTP verification to ensure phone numbers are valid and reachable. This prevents users from registering with fake or incorrect phone numbers.

---

## 🎯 Features Implemented

### 1. Database Schema
- ✅ `phone_verification_otps` table (V8 migration)
- ✅ Stores OTP codes with expiration and attempt tracking
- ✅ Links to users table

### 2. Entities & Repositories
- ✅ `PhoneVerificationOtp` entity
- ✅ `PhoneVerificationOtpRepository` with custom queries
- ✅ User entity already has `phoneVerified` field

### 3. Services
- ✅ `PhoneVerificationService` - Core verification logic
- ✅ `WhatsAppService` - WhatsApp message sending (ready for API integration)

### 4. API Endpoints
- ✅ `POST /api/phone-verification/send` - Send OTP
- ✅ `POST /api/phone-verification/verify` - Verify OTP
- ✅ `POST /api/phone-verification/resend` - Resend OTP

### 5. Security
- ✅ All endpoints require authentication
- ✅ OTP expiration (5 minutes)
- ✅ Max attempts (3 attempts)
- ✅ Phone number format validation

---

## 📋 API Usage

### 1. Send OTP
```http
POST /api/phone-verification/send?userId={userId}
Authorization: Bearer {token}
Content-Type: application/json

{
  "phoneNumber": "+237677123456"
}
```

**Response:**
```json
{
  "success": true,
  "message": "OTP sent successfully to your WhatsApp number"
}
```

### 2. Verify OTP
```http
POST /api/phone-verification/verify?userId={userId}
Authorization: Bearer {token}
Content-Type: application/json

{
  "otpCode": "123456"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Phone number verified successfully"
}
```

### 3. Resend OTP
```http
POST /api/phone-verification/resend?userId={userId}
Authorization: Bearer {token}
```

**Response:**
```json
{
  "success": true,
  "message": "OTP resent successfully to your WhatsApp number"
}
```

---

## 🔄 User Flow

### Registration with Phone Verification

1. **User Registers**
   - User provides email, phone, password
   - Account created with `phoneVerified = false`

2. **Send OTP** (Optional but recommended)
   - User calls `/api/phone-verification/send`
   - System generates 6-digit OTP
   - OTP sent via WhatsApp
   - OTP expires in 5 minutes

3. **Verify OTP**
   - User receives OTP on WhatsApp
   - User calls `/api/phone-verification/verify` with OTP
   - System validates OTP
   - User's `phoneVerified` set to `true`

4. **Resend if Needed**
   - If OTP expires or max attempts reached
   - User calls `/api/phone-verification/resend`
   - New OTP sent

---

## 🔧 WhatsApp Business API Setup

### Option 1: Meta WhatsApp Business API (Recommended)

1. **Create Facebook App**
   - Go to https://developers.facebook.com/apps/
   - Create new app
   - Add WhatsApp product

2. **Get Credentials**
   - Phone Number ID
   - Access Token
   - Verify Token (for webhooks)

3. **Configure in `application.properties`**
```properties
whatsapp.api.enabled=true
whatsapp.api.url=https://graph.facebook.com/v18.0
whatsapp.api.token=YOUR_ACCESS_TOKEN
whatsapp.api.phone-number-id=YOUR_PHONE_NUMBER_ID
```

4. **Create Message Template**
   - Go to WhatsApp Manager
   - Create template: "otp_verification"
   - Template body: "Your RoomConnect verification code is {{1}}. This code expires in 5 minutes."

5. **Update `WhatsAppService.java`**
   - Uncomment the HTTP client code
   - Add RestTemplate dependency if needed
   - Test with your credentials

### Option 2: Twilio WhatsApp API

1. **Sign up for Twilio**
   - Get Account SID and Auth Token

2. **Update `WhatsAppService.java`**
   - Replace with Twilio SDK
   - Use Twilio's WhatsApp API

### Option 3: Development Mode (Current)

- WhatsApp API is disabled by default
- OTP codes are logged to console
- Perfect for testing without API setup

---

## 🛡️ Security Features

### OTP Security
- ✅ 6-digit random OTP
- ✅ 5-minute expiration
- ✅ Maximum 3 attempts
- ✅ One-time use (marked as verified after use)
- ✅ Phone number format validation

### Validation Rules
- ✅ Phone must match registered phone number
- ✅ Phone must be in format: +237XXXXXXXXX
- ✅ Cannot verify already verified phone
- ✅ OTP must be valid and not expired

---

## 📊 Database Schema

### phone_verification_otps Table
```sql
CREATE TABLE phone_verification_otps (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    phone_number VARCHAR(20) NOT NULL,
    otp_code VARCHAR(6) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    verified BOOLEAN DEFAULT FALSE,
    verified_at TIMESTAMP,
    attempts INTEGER DEFAULT 0,
    max_attempts INTEGER DEFAULT 3,
    created_at TIMESTAMP NOT NULL
);
```

---

## 🧪 Testing

### Development Mode (WhatsApp API Disabled)
1. Send OTP → Check console logs for OTP code
2. Use logged OTP code to verify
3. Test expiration (wait 5+ minutes)
4. Test max attempts (try wrong OTP 3 times)

### Production Mode (WhatsApp API Enabled)
1. Configure WhatsApp Business API
2. Send OTP → Check WhatsApp for message
3. Verify with received OTP
4. Test all edge cases

---

## 🚀 Integration with Registration Flow

### Recommended Flow:
1. User registers → Account created
2. **Optional but recommended:** Prompt user to verify phone
3. User sends OTP → Receives on WhatsApp
4. User verifies OTP → Phone marked as verified
5. User can now use all features

### Frontend Integration:
```javascript
// After registration
async function verifyPhone(userId, phoneNumber) {
  // 1. Send OTP
  await fetch('/api/phone-verification/send', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({ phoneNumber })
  });
  
  // 2. Show OTP input form
  // 3. Verify OTP
  await fetch('/api/phone-verification/verify', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({ otpCode: userInput })
  });
}
```

---

## 📝 Notes

### Current Status
- ✅ Backend implementation complete
- ✅ Database schema ready
- ✅ API endpoints ready
- ⚠️ WhatsApp API integration pending (structure ready)

### Next Steps
1. Set up WhatsApp Business API account
2. Configure credentials in `application.properties`
3. Enable WhatsApp API (`whatsapp.api.enabled=true`)
4. Test with real WhatsApp numbers
5. Update frontend to include verification flow

### Benefits
- ✅ Prevents fake phone numbers
- ✅ Ensures WhatsApp numbers are reachable
- ✅ Better user experience (verified contacts)
- ✅ Reduces spam/fake accounts
- ✅ FREE (WhatsApp Business API free tier)

---

## 🎯 Success Criteria

- ✅ Users can send OTP to their phone
- ✅ Users can verify OTP code
- ✅ Users can resend OTP if needed
- ✅ Phone verification status tracked
- ✅ Security measures in place
- ✅ Ready for WhatsApp API integration

---

**Status:** ✅ **READY FOR WHATSAPP API INTEGRATION**

The backend is complete. Just need to configure WhatsApp Business API credentials and enable it!

