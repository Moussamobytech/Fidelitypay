FROM maven:3.9.11-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn
COPY src src

RUN chmod +x mvnw && ./mvnw -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /app/target/Fidelitypay-0.0.1-SNAPSHOT.jar app.jar

ENV SERVER_PORT=8060

EXPOSE 8060

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
