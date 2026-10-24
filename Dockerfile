FROM maven:3.9.12-eclipse-temurin-25 AS build
WORKDIR /build
COPY .mvn .mvn
COPY mvnw pom.xml ./
COPY src src
RUN mvn --batch-mode --no-transfer-progress package -DskipTests
FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /build/target/agrosphere-estate-service-0.2.1.jar app.jar
USER 10001:10001
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
