# syntax=docker/dockerfile:1

# ---------- Build stage ----------
# Compiles and packages the Spring Boot fat jar with JDK 21 + Maven.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Resolve dependencies first so this layer is cached across source-only changes.
COPY pom.xml ./
RUN mvn -B -ntp dependency:go-offline

# Build the application. Tests need live Postgres/Redis, so they run in CI, not here.
COPY src ./src
RUN mvn -B -ntp clean package -DskipTests

# ---------- Runtime stage ----------
# Slim JRE image that only carries what the packaged jar needs to run.
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

# Run as an unprivileged user instead of root.
RUN groupadd --system spring && useradd --system --gid spring spring

# Copy the built jar from the build stage. Name matches artifactId `pay`
# and version `0.0.1-SNAPSHOT` from pom.xml.
COPY --from=build /build/target/pay-0.0.1-SNAPSHOT.jar app.jar

USER spring:spring

# Spring Boot default port (no server.port override in application.yaml).
EXPOSE 8080

# All configuration (DB_URL, REDIS_HOST, KAFKA_BOOTSTRAP_SERVERS, JWT_SECRET, ...)
# is supplied at runtime via environment variables; the image ships config-free.
ENTRYPOINT ["java", "-jar", "app.jar"]
