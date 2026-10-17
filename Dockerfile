# Build stage: compiles the project with Maven
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -q clean package -DskipTests

# Run stage: small Java runtime
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/smart-lost-found-portal-1.0.0.jar app.jar
# Online hosting has no screen, so skip the Swing window; keep the DB console off.
ENV APP_DESKTOP=false H2_CONSOLE=false
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
