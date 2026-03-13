# RoomBuddy — Production Deployment Guide

## Overview
- **Backend** → Render (free tier, Spring Boot JAR)
- **Frontend** → Vercel (free tier, Next.js)
- **Database** → Render PostgreSQL (free tier)
- **Images** → Cloudinary (free tier, 25GB)
- **Email** → Gmail SMTP with App Password (free)

---

## STEP 1 — Get Your Credentials (Do This First)

### 1a. Cloudinary (Image Uploads)
1. Go to https://cloudinary.com/users/register/free
2. Sign up with Google or email
3. After login, go to **Dashboard**
4. Copy: **Cloud Name**, **API Key**, **API Secret**

### 1b. Gmail App Password (Email Sending)
1. Go to your Google Account → Security
2. Enable **2-Step Verification** if not already on
3. Go to **App Passwords** (search for it in Google Account)
4. Create a new app password → select "Mail" + "Other (custom name)" → type "RoomBuddy"
5. Copy the 16-character password shown

### 1c. Generate a Strong JWT Secret
Run this in PowerShell to generate a secure 64-char secret:
```powershell
-join ((65..90) + (97..122) + (48..57) | Get-Random -Count 64 | % {[char]$_})
```
Or use: https://generate-secret.vercel.app/64

---

## STEP 2 — Update Your Local .env

Edit `c:\Users\noble\Downloads\Roombuddy\Roombuddy-develop\.env`:

```env
# DATABASE (local dev)
DATABASE_URL=jdbc:postgresql://localhost:5432/roomconnect_db
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=postgres

# JWT — use the 64-char secret you generated
JWT_SECRET=PASTE_YOUR_64_CHAR_SECRET_HERE

# CLOUDINARY — paste from your Cloudinary dashboard
CLOUDINARY_CLOUD_NAME=your_actual_cloud_name
CLOUDINARY_API_KEY=your_actual_api_key
CLOUDINARY_API_SECRET=your_actual_api_secret

# EMAIL — Gmail SMTP
MAIL_USERNAME=your.gmail@gmail.com
MAIL_PASSWORD=your_16_char_app_password

# APP
APP_BASE_URL=http://localhost:3000
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:5173
SPRING_PROFILES_ACTIVE=default
NEXT_PUBLIC_BACKEND_URL=http://localhost:8082/api
NEXT_PUBLIC_API_URL=/api
```

---

## STEP 3 — Deploy Backend to Render

### 3a. Create Render Account
1. Go to https://render.com → Sign up with GitHub

### 3b. Create PostgreSQL Database
1. Render Dashboard → **New** → **PostgreSQL**
2. Name: `roombuddy-db`
3. Plan: **Free**
4. Click **Create Database**
5. Copy the **Internal Database URL** (starts with `postgres://`)

### 3c. Deploy Spring Boot Backend
1. Render Dashboard → **New** → **Web Service**
2. Connect your GitHub repo
3. Settings:
   - **Name**: `roombuddy-backend`
   - **Root Directory**: (leave blank — pom.xml is at root)
   - **Runtime**: `Java`
   - **Build Command**: `./mvnw clean package -DskipTests`
   - **Start Command**: `java -jar target/roombuddy-*.jar`
   - **Plan**: Free

4. Add **Environment Variables** (click "Add Environment Variable" for each):

| Key | Value |
|-----|-------|
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `DATABASE_URL` | (paste Internal Database URL from step 3b, change `postgres://` to `jdbc:postgresql://`) |
| `DATABASE_USERNAME` | (from Render DB dashboard) |
| `DATABASE_PASSWORD` | (from Render DB dashboard) |
| `JWT_SECRET` | (your 64-char secret) |
| `CLOUDINARY_CLOUD_NAME` | (from Cloudinary) |
| `CLOUDINARY_API_KEY` | (from Cloudinary) |
| `CLOUDINARY_API_SECRET` | (from Cloudinary) |
| `MAIL_USERNAME` | (your Gmail) |
| `MAIL_PASSWORD` | (your 16-char app password) |
| `APP_BASE_URL` | `https://roombuddy-frontend.vercel.app` (update after frontend deploy) |
| `CORS_ALLOWED_ORIGINS` | `https://roombuddy-frontend.vercel.app` (update after frontend deploy) |
| `PORT` | `8080` |

5. Click **Create Web Service**
6. Wait for build (~5 min). Copy your backend URL: `https://roombuddy-backend.onrender.com`

> ⚠️ **DATABASE_URL format for JDBC**: Render gives `postgres://user:pass@host/db`
> You must change it to: `jdbc:postgresql://host/db` and set user/pass separately.

---

## STEP 4 — Deploy Frontend to Vercel

### 4a. Create Vercel Account
1. Go to https://vercel.com → Sign up with GitHub

### 4b. Deploy
1. Vercel Dashboard → **New Project**
2. Import your GitHub repo
3. Settings:
   - **Framework Preset**: Next.js
   - **Root Directory**: `frontend`
4. Add **Environment Variables**:

| Key | Value |
|-----|-------|
| `NEXT_PUBLIC_BACKEND_URL` | `https://roombuddy-backend.onrender.com/api` |
| `BACKEND_URL` | `https://roombuddy-backend.onrender.com` |

5. Click **Deploy**
6. Copy your frontend URL: `https://roombuddy-frontend.vercel.app`

### 4c. Update Backend CORS
Go back to Render → your backend service → Environment:
- Update `APP_BASE_URL` to your Vercel URL
- Update `CORS_ALLOWED_ORIGINS` to your Vercel URL
- Click **Save Changes** (backend will redeploy)

---

## STEP 5 — Create Admin User

After both services are running, run this SQL on your Render PostgreSQL:

```sql
-- First register via the app, then promote to admin:
UPDATE users SET role = 'ADMIN' WHERE email = 'your-admin@email.com';
```

Or use the Render PostgreSQL console directly.

---

## STEP 6 — Verify Everything Works

Test these in order:
1. ✅ `GET https://roombuddy-backend.onrender.com/actuator/health` → should return `{"status":"UP"}`
2. ✅ Register a new user on the frontend
3. ✅ Login works
4. ✅ Create a listing (landlord)
5. ✅ Photo upload works (Cloudinary)
6. ✅ Admin dashboard loads

---

## V2 Features (After Launch)
- MTN Mobile Money / Orange Money real payment gateway
- WhatsApp Business API notifications
- OCR for student ID verification
- Video tour upload (Cloudinary video support)
- Flyway database migrations
- Subscription plans

---

## Troubleshooting

### Backend won't start on Render
- Check logs in Render dashboard
- Ensure `DATABASE_URL` is in JDBC format
- Ensure `SPRING_PROFILES_ACTIVE=prod`

### Images not loading
- Check Cloudinary credentials are correct
- Verify `CLOUDINARY_CLOUD_NAME` matches your dashboard

### CORS errors in browser
- Ensure `CORS_ALLOWED_ORIGINS` exactly matches your Vercel URL (no trailing slash)
- Redeploy backend after updating env vars

### Email not sending
- Verify Gmail 2FA is enabled
- Verify App Password is 16 chars (no spaces)
- Check `MAIL_USERNAME` is the full Gmail address
