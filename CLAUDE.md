# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Spring Boot 3.5 / Java 21 payment/merchant backend (`artifactId: pay`, `groupId: com.mini-merchant`). PostgreSQL is the source of truth; Redis is used for the merchant-lookup cache, per-merchant payment rate limiting, and transaction idempotency. Working today: JWT auth (register/login/refresh/logout), merchant CRUD (ADMIN-gated writes), an API-key + HMAC-signed `payment` domain (`/ping` and `POST /transaction` with double-entry ledger writes), and a `/ping` health check.

## Commands

```bash
# Start infrastructure (Postgres + Redis + Adminer)
docker compose up -d

# Run the application (http://localhost:8080)
./mvnw spring-boot:run

# Build (skip tests)
./mvnw package -DskipTests

# Run all tests (the context test needs Postgres + Redis up)
./mvnw test

# Run a single test class (infra-free unit test)
./mvnw test -Dtest=TransactionServiceTest

# Compile only (fast feedback)
./mvnw compile
```

## Architecture

**Package convention:** `com.mini_merchant.pay.<domain>.<layer>` for business slices under `domain/`
(`ping`, `auth`, `merchants`, `payment`), each with `controller/`, `service/`, `dto/`. Cross-domain code
lives in `common/` (`config`, `constant`, `dto`, `security`, `ratelimit`, `exception`), with shared JPA
entities in `entity/` and persistence in `repository/<domain>/`.

**Program to interfaces.** The chain `controller → service → repository` is wired through interfaces
(`I{X}Service`, `I{X}Repository`); only the `{X}Repository` impl knows the Spring Data `I{X}JpaRepository`.
Inject the interface with an `i`-prefixed field name (e.g. `iMerchantRepository`).

**Schema is Flyway-only.** Migrations live in `src/main/resources/db/migration/` and are the ONLY thing
that creates or alters schema. Hibernate is `ddl-auto: validate` — an entity that doesn't match the
migrated schema fails startup. Never rely on Hibernate to change schema.

**Responses & errors.** Every controller response is wrapped in `common/dto/ApiResponse<T>`
(`{ status, message, data }`). Exceptions map to status codes in `common/exception/GlobalExceptionHandler`
(`@RestControllerAdvice`); the payment filter writes its 401/429 directly since it runs outside MVC.

**Two auth schemes.** JWT (`JwtAuthenticationFilter`) for `auth`/`merchants`; API-key + HMAC
(`PaymentApiKeyAuthFilter`) for `/api/v1/payments/**`, which also applies the per-merchant rate limit.

**Dependencies:** Lombok (`@RequiredArgsConstructor`, `@Getter`, `@Builder`, …) throughout; Spring
Actuator for health/metrics. DTOs are Lombok classes, not Java records.

> Full architecture and API docs: `README.md`. Rules for adding a new entity/domain: `conventions.md`.

## Local Infrastructure

```
PostgreSQL 16  localhost:5432  db=mini_merchant_db  user=admin  password=abc123
Redis 7        localhost:6379
Adminer (DB UI) localhost:8081
```

Start all with `docker compose up -d` before running the app.
