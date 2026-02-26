# Build stage
FROM maven:3.9.9-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Copy the parent pom and all module poms to cache dependencies
COPY pom.xml .
COPY falcon-core/pom.xml falcon-core/
COPY falcon-gateway/pom.xml falcon-gateway/
COPY trust-service/pom.xml trust-service/
COPY siv-service/pom.xml siv-service/

# Cache dependencies
RUN mvn dependency:go-offline -B

# Copy source code
COPY . .

# Build all modules
RUN mvn clean package -DskipTests

# Final stage - Gateway
FROM eclipse-temurin:21-jre-alpine AS gateway
WORKDIR /app
COPY --from=build /app/falcon-gateway/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

# Final stage - Trust Service
FROM eclipse-temurin:21-jre-alpine AS trust-service
WORKDIR /app
COPY --from=build /app/trust-service/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]

# Final stage - SIV Service
FROM eclipse-temurin:21-jre-alpine AS siv-service
WORKDIR /app
COPY --from=build /app/siv-service/target/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
