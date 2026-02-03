# ============================================================================
# Multi-stage Dockerfile for Enterprise CRUD Application
# ============================================================================

# ============================================================================
# Stage 1: Build Stage
# ============================================================================
FROM maven:3.9-eclipse-temurin-17 AS builder

# Set working directory
WORKDIR /app

# Copy pom.xml and download dependencies (for better caching)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build application (skip tests in Docker build - run in CI/CD)
RUN mvn clean package -DskipTests -B

# Extract layers for better caching
RUN mkdir -p target/extracted && \
    java -Djarmode=layertools -jar target/*.jar extract --destination target/extracted

# ============================================================================
# Stage 2: Runtime Stage
# ============================================================================
FROM eclipse-temurin:17-jre-alpine

# Install required packages
RUN apk add --no-cache \
    curl \
    ca-certificates \
    tzdata

# Create non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Set working directory
WORKDIR /app

# Copy layers from builder (in order of least to most frequently changing)
COPY --from=builder /app/target/extracted/dependencies/ ./
COPY --from=builder /app/target/extracted/spring-boot-loader/ ./
COPY --from=builder /app/target/extracted/snapshot-dependencies/ ./
COPY --from=builder /app/target/extracted/application/ ./

# Change ownership to non-root user
RUN chown -R appuser:appgroup /app

# Switch to non-root user
USER appuser

# Expose application port
EXPOSE 8080

# Expose actuator port (optional, if different)
EXPOSE 8081

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Environment variables (override these in docker-compose or k8s)
ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

# Run application
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} org.springframework.boot.loader.launch.JarLauncher"]

# Labels for metadata
LABEL maintainer="enterprise-team@example.com" \
      version="2.0.0" \
      description="Enterprise CRUD Application with MFA, Rate Limiting, and Audit Logging" \
      org.opencontainers.image.source="https://github.com/your-org/crud-test-app"
