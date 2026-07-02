# Mini Merchant Server

A Spring Boot payment/merchant backend (`groupId: com.mini-merchant`, `artifactId: pay`). It exposes a REST API for managing merchants and — as a roadmap — connecting them to Vietnamese payment gateways (MoMo, VNPay). PostgreSQL is the single source of truth for persistence; Redis is available for caching and, later, idempotency/locking.

> **Status:** Early stage. A full merchant CRUD domain and a `/ping` health check exist today. Payment/order/callback domains are planned (see [Roadmap](#roadmap)).

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
├── common/
│   └── dto/
│       └── ApiResponse.java            # Uniform response envelope { status, message, data }
│
├── entity/                             # JPA entities (shared, not per-domain)
│   └── Merchants.java
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
└── db/migration/
    └── V1__create_table_merchants.sql  # Flyway migrations
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

Base path for domain resources: `/api/v1`.

### Merchants — `/api/v1/merchants`

| Method | Path                    | Body                      | Returns                        |
|--------|-------------------------|---------------------------|--------------------------------|
| POST   | `/api/v1/merchants`     | `CreateMerchantsReqModel` | `{ id }`                       |
| GET    | `/api/v1/merchants`     | —                         | `[ GetMerchantResModel ]`      |
| GET    | `/api/v1/merchants/{id}`| —                         | `GetMerchantResModel`          |
| PUT    | `/api/v1/merchants/{id}`| `UpdateMerchantReqModel`  | `GetMerchantResModel`          |
| DELETE | `/api/v1/merchants/{id}`| —                         | `null` (soft delete)           |

All responses are wrapped in the `ApiResponse<T>` envelope:

```json
{ "status": 200, "message": "success", "data": { "...": "..." } }
```

Example — create a merchant:

```bash
curl -X POST http://localhost:8080/api/v1/merchants \
  -H 'Content-Type: application/json' \
  -d '{"name":"Acme","email":"acme@example.com","status":"ACTIVE","createdBy":"admin"}'
```

`apiKey` and `secret` are generated server-side and are never accepted from the client.

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
- Redis-backed idempotency keys and distributed locks for concurrent callbacks
- Centralized exception handling (`@RestControllerAdvice`) mapping domain errors to `ApiResponse.error(...)`
