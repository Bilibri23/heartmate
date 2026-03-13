# RoomBuddy Backend Dockerfile
# Multi-stage build for optimized production image
#
# NOTE: This Dockerfile should be built from the PROJECT ROOT, not from backend/
# Example: docker build -f backend/Dockerfile -t roombuddy-backend .

# ==================== BUILD STAGE ====================
FROM maven:3.9-eclipse-temurin-17-alpine AS builder

WORKDIR /app

# Copy pom.xml first for dependency caching
COPY pom.xml .

# Download dependencies (cached if pom.xml unchanged)
# Use -DskipTests and increase timeout for slow networks
RUN mvn dependency:resolve dependency:resolve-plugins -B -Dmaven.wagon.http.retryHandler.count=3

# Copy source code (from backend/src since pom.xml is in root)
COPY backend/src ./src

# Build the application (skip tests for faster builds, run tests in CI)
# Add -X for debug output if build fails
RUN mvn clean package -DskipTests -B -Dmaven.wagon.http.pool=false

# ==================== RUNTIME STAGE ====================
FROM eclipse-temurin:17-jre-alpine

# Add labels for container metadata
LABEL maintainer="RoomBuddy Team"
LABEL version="1.0.0"
LABEL description="RoomBuddy Backend API - Student Housing Platform"

# Create non-root user for security
RUN addgroup -g 1001 -S roombuddy && \
    adduser -u 1001 -S roombuddy -G roombuddy

WORKDIR /app

# Copy the built JAR from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Change ownership to non-root user
RUN chown -R roombuddy:roombuddy /app

# Switch to non-root user
USER roombuddy

# Expose the application port
EXPOSE 8082

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# JVM optimization for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=50.0"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
