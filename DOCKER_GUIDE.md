# 🐳 RoomBuddy Docker Guide

## Quick Start

### Prerequisites
- [Docker Desktop](https://www.docker.com/products/docker-desktop/) installed
- At least 4GB RAM allocated to Docker

### Start Everything (Production-like)
```bash
# Build and start all services
docker-compose up -d

# View logs
docker-compose logs -f

# Stop everything
docker-compose down
```

### Access Points
| Service | URL | Description |
|---------|-----|-------------|
| Frontend | http://localhost:3000 | React application |
| Backend API | http://localhost:8080 | Spring Boot API |
| Swagger UI | http://localhost:8080/swagger-ui.html | API documentation |
| Mailhog | http://localhost:8025 | Email testing UI |
| Adminer | http://localhost:8081 | Database management |

---

## Development Mode

For active development with hot-reloading, it's better to run frontend and backend outside Docker:

### Option 1: Database Only (Recommended for Development)
```bash
# Start only database and mail server
docker-compose up -d postgres mailhog adminer

# Run backend locally
cd backend
mvn spring-boot:run

# Run frontend locally (in another terminal)
cd frontend/room8
npm run dev
```

### Option 2: Full Docker Development
```bash
docker-compose -f docker-compose.yml -f docker-compose.dev.yml up
```

---

## Services Overview

### PostgreSQL Database
- **Port**: 5432
- **Database**: roombuddy
- **Username**: roombuddy
- **Password**: roombuddy_secret

Connect with any SQL client:
```
jdbc:postgresql://localhost:5432/roombuddy
```

### Mailhog (Email Testing)
All emails sent by the application are caught by Mailhog.
- **SMTP**: localhost:1025
- **Web UI**: http://localhost:8025

No emails are actually sent to real addresses!

### Adminer (Database UI)
Access at http://localhost:8081
- System: PostgreSQL
- Server: postgres
- Username: roombuddy
- Password: roombuddy_secret
- Database: roombuddy

---

## Common Commands

```bash
# Build images without cache
docker-compose build --no-cache

# Restart a specific service
docker-compose restart backend

# View logs for specific service
docker-compose logs -f backend

# Execute command in running container
docker-compose exec backend sh
docker-compose exec postgres psql -U roombuddy -d roombuddy

# Remove all data (including database)
docker-compose down -v

# Check container health
docker-compose ps
```

---

## Environment Variables

Create a `.env` file in the project root for sensitive values:

```env
# Cloudinary (for image uploads)
CLOUDINARY_CLOUD_NAME=your-cloud-name
CLOUDINARY_API_KEY=your-api-key
CLOUDINARY_API_SECRET=your-api-secret

# JWT Secret (use strong secret in production!)
JWT_SECRET=your-super-secret-jwt-key-min-32-characters
```

---

## Troubleshooting

### Port Already in Use
```bash
# Find what's using the port
netstat -ano | findstr :8080

# Or change ports in docker-compose.yml
ports:
  - "8081:8080"  # Use 8081 instead
```

### Database Connection Issues
```bash
# Check if postgres is healthy
docker-compose ps

# View postgres logs
docker-compose logs postgres

# Reset database
docker-compose down -v
docker-compose up -d
```

### Out of Memory
Increase Docker memory in Docker Desktop settings (Settings → Resources → Memory)

---

## Production Deployment

For production, use environment-specific compose files:

```bash
# Production build
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d
```

Key differences for production:
- Use managed database (AWS RDS, etc.)
- Use real SMTP server
- Enable HTTPS
- Use secrets management
- Set proper resource limits
