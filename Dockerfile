
# Stage 1: Build the Spring Boot application
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

# Copy Maven configuration and wrapper
COPY pom.xml .
COPY .mvn/ .mvn/
COPY mvnw .

RUN chmod +x mvnw

# Copy application source code
COPY src/ src/

# Compile and package the application into a JAR
RUN ./mvnw -B clean package -DskipTests

# Stage 2: Run the application
FROM eclipse-temurin:17-jre

WORKDIR /app

# Copy only the packaged JAR from the build stage
COPY --from=build /app/target/seatsure-backend-0.0.1-SNAPSHOT.jar app.jar

# Document the default application port
EXPOSE 8080

# Start the Spring Boot application
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]