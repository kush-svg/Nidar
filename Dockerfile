# Stage 1: Build the application
FROM gradle:8.6-jdk17-alpine AS builder
WORKDIR /app

# Copy gradle wrapper and build scripts first to leverage Docker cache
COPY build.gradle.kts settings.gradle.kts ./
COPY gradle ./gradle
COPY gradlew ./

# Make the wrapper executable
RUN chmod +x gradlew

# Copy the source code
COPY src ./src

# Build the Spring Boot application (skipping tests for speed)
RUN ./gradlew bootJar --no-daemon -x test

# Stage 2: Run the application
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copy the built jar from the builder stage
COPY --from=builder /app/build/libs/*-SNAPSHOT.jar app.jar

ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-jar", "app.jar"]

EXPOSE 8081