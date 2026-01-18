# --- Stage 1: Build Stage ---
# We use a Gradle image with a JDK to build the application
FROM gradle:8.5-jdk17 AS builder

# Set the working directory inside the builder container
WORKDIR /app

# 1. Copy only Gradle configuration files first
# This allows Docker to cache dependencies if these files haven't changed
COPY build.gradle.kts settings.gradle.kts gradle.properties ./

# 2. Download dependencies (optional but recommended for caching)
# This step will fail if the build scripts are invalid, but saves time on re-builds
RUN gradle dependencies --no-daemon || true

# 3. Copy the rest of the source code
COPY src ./src

# 4. Build the application (skip tests for faster builds, remove -x test to run them)
RUN gradle clean build -x test --no-daemon

# --- Stage 2: Runtime Stage ---
# We use a smaller, lightweight JRE image for the final container
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# 5. Copy the JAR file from the builder stage
# Note: Ensure your gradle build produces a "fat" or "shadow" jar if you have dependencies
# The path 'build/libs/' depends on your gradle setup, but is standard.
COPY --from=builder /app/build/libs/*.jar app.jar

# 6. Expose the port your app runs on (e.g., Ktor/Spring Boot default is often 8080)
EXPOSE 8080

# 7. Define the command to run the application
ENTRYPOINT ["java", "-jar", "app.jar"]