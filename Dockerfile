FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /app
COPY pom.xml pom.xml
COPY juntos-alpha/pom.xml juntos-alpha/pom.xml
COPY juntos-alpha/src juntos-alpha/src
RUN mvn -pl juntos-alpha -am package -DskipTests

FROM eclipse-temurin:21-jre

WORKDIR /app
COPY --from=build /app/juntos-alpha/target/juntos-alpha-0.1.0.jar app.jar
EXPOSE 8080

CMD ["sh", "-c", "java -Dserver.port=${PORT:-8080} -Dserver.address=0.0.0.0 -jar app.jar"]
