# Build stage
FROM maven:3.9-eclipse-temurin-21-alpine AS build

WORKDIR /app

# Copy the parent pom
COPY pom.xml .

# Copy the module
COPY juntos-alpha/ juntos-alpha/

# Build the module
RUN mvn clean package -DskipTests -f juntos-alpha/pom.xml

# Final stage
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=build /app/juntos-alpha/target/*.jar app.jar

ENTRYPOINT ["java", "--enable-preview", "-jar", "app.jar"]