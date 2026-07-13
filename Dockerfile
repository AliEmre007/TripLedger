FROM maven:3.9.10-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY src ./src
COPY config ./config
RUN mvn -B package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/target/tripledger-0.1.0-SNAPSHOT.jar /app/tripledger.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/tripledger.jar"]
