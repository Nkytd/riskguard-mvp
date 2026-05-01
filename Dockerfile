FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /workspace/target/riskguard-mvp-0.0.1-SNAPSHOT.jar /app/riskguard-mvp.jar
COPY application-docker.yml /app/config/application-docker.yml
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/riskguard-mvp.jar", "--spring.config.additional-location=/app/config/application-docker.yml"]
