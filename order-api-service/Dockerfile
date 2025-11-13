# Multi-stage build for Order Service
FROM maven:3.9.5-eclipse-temurin-21 AS build

# Set working directory
WORKDIR /workspace

# Copy Maven files for dependency caching
COPY pom.xml ./
COPY mvnw ./
COPY mvnw.cmd ./
COPY .mvn .mvn

# Make sure Maven wrapper is executable
RUN chmod +x mvnw

# Download dependencies first for better caching
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application (skip tests for faster builds)
RUN ./mvnw clean package -DskipTests

# Production stage
FROM eclipse-temurin:21-jre-alpine

# Install curl for health checks
RUN apk add --no-cache curl

# Create non-root user for security
RUN addgroup -g 1001 -S orderuser && adduser -u 1001 -S orderuser -G orderuser

# Set working directory
WORKDIR /app

# Copy the JAR file from build stage
COPY --from=build /workspace/target/*.jar app.jar

# Change ownership to non-root user
RUN chown orderuser:orderuser app.jar

# Switch to non-root user
USER orderuser

# Expose application port
EXPOSE 8090

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8090/actuator/health || exit 1
# Run the application with Docker profile
ENTRYPOINT ["java", "-Xmx512m", "-Xms256m", "-Dspring.profiles.active=docker", "-jar", "/app/app.jar"]
