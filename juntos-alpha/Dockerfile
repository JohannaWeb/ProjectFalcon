# Build stage
FROM eclipse-temurin:25-jdk-alpine AS build

# Install Maven (Alpine)
RUN apk add --no-cache maven

WORKDIR /app

# Copy the parent pom
COPY pom.xml .

# Copy the module
COPY juntos-alpha/ juntos-alpha/

# Build the module
RUN mvn clean package -DskipTests -f juntos-alpha/pom.xml

# Final stage
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY --from=build /app/juntos-alpha/target/*.jar app.jar
ENTRYPOINT ["java", "--enable-preview", "-jar", "app.jar"]
