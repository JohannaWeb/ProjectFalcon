FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /app
COPY pom.xml pom.xml
COPY juntos-alpha/pom.xml juntos-alpha/pom.xml
COPY juntos-alpha/src juntos-alpha/src
RUN mvn -pl juntos-alpha -am package -DskipTests

FROM eclipse-temurin:21-jre

WORKDIR /app
COPY --from=build /app/juntos-alpha/target/quarkus-app/ quarkus-app/
EXPOSE 8080

CMD ["java", "-jar", "quarkus-app/quarkus-run.jar"]
