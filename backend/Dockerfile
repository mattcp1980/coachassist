# Stage 1: Build the application using Gradle
FROM gradle:8.5-jdk17-alpine AS build
WORKDIR /home/gradle/src
# Copy only necessary files to leverage Docker layer caching
# Copy dependency-related files first
COPY build.gradle.kts settings.gradle.kts ./
# Copy source code
COPY src ./src
# Use the shadowJar task to build an executable fat JAR
RUN gradle --no-daemon shadowJar

# Stage 2: Create a minimal runtime image
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Create a dedicated user and group to run the application for better security
RUN groupadd --system appgroup && useradd --system --gid appgroup appuser
USER appuser

# Copy the fat JAR from the build stage, renaming it for simplicity
# Use a wildcard to avoid hardcoding the version
COPY --from=build /home/gradle/src/build/libs/coach-assist-service-*-all.jar ./app.jar

# Expose the port the application runs on (from application.conf)
EXPOSE 5001
# Command to run the application when the container starts
CMD ["java", "-jar", "app.jar"]