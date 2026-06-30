# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Spring Boot 3.5 / Java 21 payment/merchant backend (`artifactId: pay`, `groupId: com.mini-merchant`). Uses PostgreSQL for persistence and Redis for caching. Currently in early scaffolding — only a `/ping` health-check endpoint exists.

## Commands

```bash
# Start infrastructure (Postgres + Redis)
docker compose up -d

# Run the application
./mvnw spring-boot:run

# Build (skip tests)
./mvnw package -DskipTests

# Run all tests
./mvnw test

# Run a single test class
./mvnw test -Dtest=PayApplicationTests

# Compile only (fast feedback)
./mvnw compile
```

## Architecture

**Package convention:** `com.mini_merchant.pay.<domain>.<layer>`

Each domain (e.g. `ping`) is self-contained with three sub-packages:
- `controller` — REST layer (`@RestController`)
- `service` — business logic (`@Service`)
- `dto` — request/response records

**Infrastructure:** `application.yaml` holds all datasource/redis config. `application.properties` sets only `spring.application.name=pay`. Hibernate DDL is `update` — schema evolves automatically from JPA entities.

**Dependencies:** Lombok (`@RequiredArgsConstructor`, `@Data`, etc.) is used throughout; Spring Actuator is included for health/metrics endpoints.

## Local Infrastructure

```
PostgreSQL 16  localhost:5432  db=mini_merchant_db  user=admin  password=abc123
Redis 7        localhost:6379
```

Start both with `docker compose up -d` before running the app.
