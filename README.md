# Mini Merchant Server

A Spring Boot payment/merchant backend (`groupId: com.mini-merchant`, `artifactId: pay`). It exposes a REST API for managing merchants and — as a roadmap — connecting them to Vietnamese payment gateways (MoMo, VNPay). PostgreSQL is the single source of truth for persistence; Redis is available for caching and, later, idempotency/locking.

> **Status:** Early stage. A full merchant CRUD domain and a `/ping` health check exist today, plus scaffolded `transactions` and `ledger_entries` schema/entities and a centralized exception handler. Payment/order/callback domains are planned (see [Roadmap](#roadmap)).

---

## Tech Stack

| Layer         | Technology                                    |
|---------------|-----------------------------------------------|
| Runtime       | Java 21                                        |
| Framework     | Spring Boot 3.5.16                             |
| Persistence   | PostgreSQL 16 + Spring Data JPA / Hibernate    |
| Migrations    | Flyway (SQL, the **only** thing that alters schema) |
| Cache         | Redis 7 + Spring Data Redis                    |
| Validation    | Spring Bean Validation (`jakarta.validation`)  |
| Boilerplate   | Lombok                                         |
| Build         | Maven (`./mvnw` wrapper)                        |
| Infra (local) | Docker Compose                                 |

---

## Project Structure

The codebase splits into **per-domain slices** (under `domain/`) and **shared cross-domain layers** (`entity/`, `repository/`, `common/`).

```
src/main/java/com/mini_merchant/pay/
│
├── PayApplication.java                 # Spring Boot entry point
│
├── common/                             # Shared, cross-domain code
│   ├── constant/
│   │   ├── ApiPath.java                # Centralized URL paths (/api/v1, /api/v1/merchants, /ping)
│   │   ├── Direction.java              # Ledger-entry direction enum (DEBIT / CREDIT)
│   │   └── HttpStatusCode.java         # int HTTP status constants (200/201/400/404/500)
│   ├── dto/
│   │   └── ApiResponse.java            # Uniform response envelope { status, message, data }
│   └── exception/
│       ├── NotFoundException.java      # Missing resource → 404
│       └── GlobalExceptionHandler.java # @RestControllerAdvice: exceptions → ApiResponse
│
├── entity/                             # JPA entities (shared, not per-domain)
│   ├── Merchants.java
│   ├── Transactions.java
│   └── LedgerEntries.java
│
├── repository/                         # Persistence, one sub-package per domain
│   └── merchants/
│       ├── IMerchantRepository.java    # Domain-facing repository interface
│       ├── IMerchantJpaRepository.java # Spring Data JPA interface
│       └── MerchantRepository.java     # Impl adapting JPA → domain interface
│
└── domain/                             # Business slices, one package per domain
    ├── ping/
    │   ├── controller/  service/  dto/
    └── merchants/
        ├── controller/                 # MerchantController  (@RestController)
        ├── service/                    # IMerchantService + MerchantService (@Service)
        └── dto/
            ├── create/                 # CreateMerchantsReqModel / CreateMerchantsResModel
            ├── update/                 # UpdateMerchantReqModel
            └── detail/                 # GetMerchantResModel

src/main/resources/
├── application.yaml                    # Datasource / JPA / Flyway / Redis config
├── application.properties              # spring.application.name=pay
└── db/migration/                       # Flyway migrations
    ├── V1__create_table_merchants.sql
    ├── V2__create_table_transactions.sql
    ├── V3__create_table_ledger_entries.sql
    └── V4__add_index_transactions_merchant_id.sql
```

> Detailed rules for adding a new domain live in [`conventions.md`](conventions.md).

---

## Getting Started

### Prerequisites

- Java 21
- Docker + Docker Compose
- (Maven is provided via the `./mvnw` wrapper)

### 1. Start infrastructure

```bash
docker compose up -d
```

Starts:
- **PostgreSQL 16** — `localhost:5432` (db `mini_merchant_db`, user `admin`, password `abc123`)
- **Redis 7** — `localhost:6379`

### 2. Run the application

```bash
./mvnw spring-boot:run
```

Server starts on `http://localhost:8080`. On boot, Flyway applies any pending migrations and Hibernate **validates** (does not modify) the schema.

### 3. Verify

```bash
curl http://localhost:8080/ping
# {"message":"pong"}
```

---

## API

Base path for domain resources: `/api/v1`. URL paths are centralized in `common/constant/ApiPath.java` — controllers reference the constants (e.g. `@RequestMapping(ApiPath.MERCHANTS)`) rather than hard-coding strings.

### Merchants — `/api/v1/merchants`

| Method | Path                    | Body                      | Success | Returns                        |
|--------|-------------------------|---------------------------|---------|--------------------------------|
| POST   | `/api/v1/merchants`     | `CreateMerchantsReqModel` | 201     | `{ id }`                       |
| GET    | `/api/v1/merchants`     | —                         | 200     | `[ GetMerchantResModel ]`      |
| GET    | `/api/v1/merchants/{id}`| —                         | 200     | `GetMerchantResModel`          |
| PUT    | `/api/v1/merchants/{id}`| `UpdateMerchantReqModel`  | 200     | `GetMerchantResModel`          |
| DELETE | `/api/v1/merchants/{id}`| —                         | 200     | `null` (soft delete)           |

On create/update, `email` is validated as a well-formed address (`@Email`); `name`, `status`, and the audit fields are required (`@NotBlank`). `apiKey` and `secret` are generated server-side and are never accepted from the client.

All responses are wrapped in the `ApiResponse<T>` envelope, whose `status` mirrors the HTTP status:

```json
{ "status": 200, "message": "success", "data": { "...": "..." } }
```

Example — create a merchant:

```bash
curl -X POST http://localhost:8080/api/v1/merchants \
  -H 'Content-Type: application/json' \
  -d '{"name":"Acme","email":"acme@example.com","status":"ACTIVE","createdBy":"admin"}'
```

### Error handling

Exceptions from **any** controller are caught by `GlobalExceptionHandler` (`@RestControllerAdvice`) and returned as an `ApiResponse` with `data: null` and a matching HTTP status:

| Condition                                   | Status | Example message                             |
|---------------------------------------------|--------|---------------------------------------------|
| Bean-validation failure (`@Valid`)          | 400    | `email: Merchant email must be a valid…`    |
| `NotFoundException` (e.g. unknown merchant) | 404    | `Merchant not found: <id>`                  |
| Any other uncaught exception                | 500    | (the exception message)                     |

```json
{ "status": 404, "message": "Merchant not found: 3f2a…", "data": null }
```

Status codes come from `common/constant/HttpStatusCode.java`.

---

## Configuration

All runtime config lives in `src/main/resources/application.yaml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/mini_merchant_db
    username: admin
    password: abc123
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: validate   # CHECK ONLY — never create/alter/drop tables
  flyway:
    enabled: true          # Flyway is the ONLY place schema is created & changed
  data:
    redis:
      host: localhost
      port: 6379
```

> **Schema rule:** every schema change is a new Flyway migration under `db/migration/`. Hibernate is set to `validate`, so an entity that doesn't match the migrated schema fails startup.

---

## Build & Test

```bash
# Compile only (fast feedback)
./mvnw compile

# Build jar (skip tests)
./mvnw package -DskipTests

# Run all tests
./mvnw test

# Run a single test class
./mvnw test -Dtest=MerchantServiceTest
```

Service logic is unit-tested with JUnit 5 + Mockito + AssertJ (see `MerchantServiceTest`).

---

## Roadmap

- `order/` — create and query merchant orders
- `payment/` — initiate payment sessions with MoMo / VNPay
- `callback/` — verify and process gateway webhooks
- Wire the `transactions` / `ledger_entries` schema into a service/API layer
- Redis-backed idempotency keys and distributed locks for concurrent callbacks
