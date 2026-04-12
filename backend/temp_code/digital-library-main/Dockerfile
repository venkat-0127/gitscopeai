# 1️⃣ Use Maven to build the JAR
FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /app

# Copy pom.xml and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests

# 2️⃣ Use a lightweight image for running the app
FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app

# Copy the JAR from the builder stage
COPY --from=builder /app/target/*.jar app.jar

# Expose the port your Spring Boot app runs on
EXPOSE 8083

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
