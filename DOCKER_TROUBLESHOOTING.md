# 🔧 Docker Build Troubleshooting Guide

## Problem: Backend Build Takes 400+ Seconds

### Why This Happens
- Maven is downloading dependencies inside Docker (slow on first build)
- Network issues or firewall blocking Maven Central
- Windows Docker Desktop performance issues

### Solution 1: Use Pre-Built JAR (Fastest)

Instead of building inside Docker, build locally first:

```bash
# Step 1: Build the JAR locally (faster, uses your Maven cache)
mvn clean package -DskipTests

# Step 2: Use the simpler docker-compose
docker-compose -f docker-compose.local.yml up -d
```

This uses `Dockerfile.simple` which just copies your pre-built JAR.

---

### Solution 2: Run Backend Outside Docker (Recommended for Development)

Only run database and mail server in Docker:

```bash
# Start only infrastructure services
docker-compose up -d postgres mailhog adminer

# Run backend locally (in another terminal)
mvn spring-boot:run

# Run frontend locally (in another terminal)
cd frontend/room8
npm run dev
```

**Benefits:**
- ✅ Faster startup
- ✅ Hot reload works
- ✅ Easier debugging
- ✅ No Docker build time

---

### Solution 3: Fix the Multi-Stage Build

If you want the full Docker build to work:

#### Check 1: Verify Maven Can Download Dependencies
```bash
# Test Maven locally first
mvn dependency:resolve

# If this fails, you have network/firewall issues
```

#### Check 2: Increase Docker Memory
1. Open Docker Desktop
2. Settings → Resources → Memory
3. Increase to at least 4GB
4. Click "Apply & Restart"

#### Check 3: Use Docker BuildKit (Faster Builds)
```bash
# Enable BuildKit
$env:DOCKER_BUILDKIT=1  # PowerShell
set DOCKER_BUILDKIT=1   # CMD

# Then rebuild
docker-compose build --no-cache
```

---

## Problem: "Cannot Find JAR" Error

### Cause
The JAR wasn't built successfully.

### Solution
```bash
# Build locally first
mvn clean package -DskipTests

# Check if JAR exists
ls target/*.jar

# If JAR exists, use docker-compose.local.yml
docker-compose -f docker-compose.local.yml up -d
```

---

## Problem: Port Already in Use

### Error Message
```
Error starting userland proxy: listen tcp4 0.0.0.0:8080: bind: address already in use
```

### Solution
```bash
# Option 1: Stop the conflicting service
# Find what's using port 8080
netstat -ano | findstr :8080

# Kill the process (replace PID with actual number)
taskkill /PID <PID> /F

# Option 2: Change the port in docker-compose.yml
# Change "8080:8080" to "8081:8080"
```

---

## Problem: Database Connection Failed

### Error Message
```
org.postgresql.util.PSQLException: Connection refused
```

### Solution
```bash
# Check if postgres is running
docker-compose ps

# If not healthy, check logs
docker-compose logs postgres

# Restart postgres
docker-compose restart postgres

# If still failing, remove and recreate
docker-compose down -v
docker-compose up -d
```

---

## Problem: Frontend Shows "Cannot Connect to API"

### Cause
Backend isn't running or CORS issue.

### Solution
```bash
# Check backend is running
docker-compose logs backend

# Check backend health
curl http://localhost:8080/actuator/health

# If backend is down, check environment variables
docker-compose config
```

---

## Recommended Development Setup

For the best development experience:

```bash
# Terminal 1: Infrastructure only
docker-compose up -d postgres mailhog adminer

# Terminal 2: Backend
mvn spring-boot:run

# Terminal 3: Frontend
cd frontend/room8
npm run dev
```

**Access:**
- Frontend: http://localhost:5173
- Backend: http://localhost:8080
- Swagger: http://localhost:8080/swagger-ui.html
- Mailhog: http://localhost:8025
- Adminer: http://localhost:8081

---

## Quick Commands Reference

```bash
# View logs
docker-compose logs -f backend
docker-compose logs -f postgres

# Restart a service
docker-compose restart backend

# Rebuild a service
docker-compose build --no-cache backend

# Stop everything
docker-compose down

# Stop and remove all data
docker-compose down -v

# Check service status
docker-compose ps

# Execute command in container
docker-compose exec backend sh
docker-compose exec postgres psql -U roombuddy -d roombuddy
```

---

## Still Having Issues?

1. **Check Docker Desktop is running**
2. **Verify you have enough disk space** (at least 10GB free)
3. **Try restarting Docker Desktop**
4. **Check Windows Firewall isn't blocking Docker**
5. **Use the local build approach** (Solution 1 above)
