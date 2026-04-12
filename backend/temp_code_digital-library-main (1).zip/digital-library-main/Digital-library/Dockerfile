# Step 1: Build stage
FROM maven:3.9.4-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Step 2: Run stage
FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app

# Copy JAR from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Expose backend port (8083)
EXPOSE 8083

# Run the Spring Boot application
ENTRYPOINT ["java", "-jar", "app.jar"]
