# Runtime stage - Alpha (Postgres Backend)
FROM eclipse-temurin:25-jre-alpine AS alpha
WORKDIR /app

# Copy the pre-built JAR from the local host
COPY falcon-alpha/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "--enable-preview", "-jar", "app.jar"]
