# Mini Merchant Server

A Spring Boot payment/merchant backend (`groupId: com.mini-merchant`, `artifactId: pay`). It exposes a REST API for managing merchants, authenticating users, and accepting merchant-signed payment calls — and, as a roadmap, connecting to Vietnamese payment gateways (MoMo, VNPay). PostgreSQL is the single source of truth for persistence; Redis caches hot lookups (and, later, idempotency/locking).

> **Status:** Active development. Working today: JWT-based auth (register / login / refresh / logout) on Spring Security, merchant CRUD with role-based authorization, a `payment` domain guarded by API-key + HMAC request signing, a Redis-cached merchant lookup, and a `/ping` health check. `transactions` and `ledger_entries` exist as schema + entities only. Order/callback domains and gateway integration are planned (see [Roadmap](#roadmap)).

---

## Tech Stack

| Layer         | Technology                                    |
|---------------|-----------------------------------------------|
| Runtime       | Java 21                                        |
| Framework     | Spring Boot 3.5.16                             |
| Security      | Spring Security + JWT (jjwt 0.12.6), BCrypt    |
| Persistence   | PostgreSQL 16 + Spring Data JPA / Hibernate    |
| Migrations    | Flyway (SQL, the **only** thing that alters schema) |
| Cache         | Redis 7 + Spring Data Redis (`@Cacheable`)     |
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
│   ├── config/
│   │   ├── SecurityConfig.java         # SecurityFilterChain, method security, BCrypt encoder
│   │   └── CacheConfig.java            # @EnableCaching + Redis CacheManager (JSON values, TTL)
│   ├── constant/
│   │   ├── ApiPath.java                # Centralized URL paths (/api/v1, merchants, auth, payments, /ping)
│   │   ├── Role.java                   # ADMIN / MERCHANT / USER
│   │   ├── Direction.java              # Ledger-entry direction (DEBIT / CREDIT)
│   │   └── HttpStatusCode.java         # int HTTP status constants
│   ├── dto/
│   │   └── ApiResponse.java            # Uniform response envelope { status, message, data }
│   ├── security/
│   │   ├── JwtTokenProvider.java       # Signs/parses access tokens (HMAC), TTL config
│   │   ├── JwtAuthenticationFilter.java# Reads Bearer token → Authentication
│   │   ├── JwtAuthenticationEntryPoint.java # 401 as ApiResponse JSON
│   │   ├── CustomUserDetailsService.java
│   │   └── PaymentApiKeyAuthFilter.java # API-key/HMAC filter for /api/v1/payments/**
│   └── exception/
│       ├── NotFoundException / ConflictException / UnauthorizedException
│       └── GlobalExceptionHandler.java # @RestControllerAdvice: exceptions → ApiResponse
│
├── entity/                             # JPA entities (shared, not per-domain)
│   ├── Merchants.java   Users.java   RefreshTokens.java
│   ├── Transactions.java   LedgerEntries.java
│
├── repository/                         # Persistence, one sub-package per domain
│   └── merchants/  users/  refreshtokens/   # I{X}Repository + I{X}JpaRepository + {X}Repository
│
└── domain/                             # Business slices, one package per domain
    ├── ping/       # health check
    ├── auth/       # register / login / refresh / logout
    ├── merchants/  # merchant CRUD (writes are ADMIN-gated)
    └── payment/    # API-key/HMAC-signed payment calls
        # each: controller/  service/  dto/

src/main/resources/
├── application.yaml                    # Datasource / JPA / Flyway / Redis / JWT / cache config
├── application.properties              # spring.application.name=pay
└── db/migration/                       # Flyway migrations
    ├── V1__create_table_merchants.sql
    ├── V2__create_table_transactions.sql
    ├── V3__create_table_ledger_entries.sql
    ├── V4__add_index_transactions_merchant_id.sql
    ├── V5__create_table_users.sql
    └── V6__create_table_refresh_tokens.sql
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
- **Adminer** (DB UI) — `localhost:8081`

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

## Authentication & Authorization

The app has **two independent auth schemes**:

1. **JWT (users)** — for the `auth` and `merchants` APIs. Register/login issue a short-lived **access token** (Bearer JWT) plus a longer-lived **refresh token** persisted in the DB. `JwtAuthenticationFilter` turns a valid `Authorization: Bearer <token>` into an authenticated principal; failures return 401 via `JwtAuthenticationEntryPoint`. Method-level `@PreAuthorize("hasRole('ADMIN')")` gates merchant writes.
2. **API-key + HMAC (merchants → payments)** — for `/api/v1/payments/**`. These routes are `permitAll` in the JWT chain and instead guarded by `PaymentApiKeyAuthFilter`, which validates three headers on every call. No JWT is used here.

Passwords are hashed with **BCrypt**. Registration enforces a strong password: **≥ 8 chars incl. an uppercase, a lowercase, a digit, and a special character.**

### Auth — `/api/v1/auth` (public)

| Method | Path                     | Body               | Returns                                   |
|--------|--------------------------|--------------------|-------------------------------------------|
| POST   | `/api/v1/auth/register`  | `{ email, password }` | created user `{ id, email, role }` (role `USER`) |
| POST   | `/api/v1/auth/login`     | `{ email, password }` | `{ accessToken, refreshToken, ... }`      |
| POST   | `/api/v1/auth/refresh`   | `{ refreshToken }`    | a new access token                        |
| POST   | `/api/v1/auth/logout`    | `{ refreshToken }`    | revokes (soft-deletes) that refresh token |

```bash
# register → login → call a protected endpoint
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"email":"user@example.com","password":"Passw0rd!"}'

curl -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"user@example.com","password":"Passw0rd!"}'
# → { "data": { "accessToken": "<JWT>", "refreshToken": "<token>", ... } }
```

Token TTLs are configured under `app.jwt.*` (access-token minutes, refresh-token days).

---

## API

Base path for domain resources: `/api/v1`. URL paths are centralized in `common/constant/ApiPath.java` — controllers reference the constants (e.g. `@RequestMapping(ApiPath.MERCHANTS)`) rather than hard-coding strings. All responses use the `ApiResponse<T>` envelope:

```json
{ "status": 200, "message": "success", "data": { "...": "..." } }
```

### Merchants — `/api/v1/merchants` (JWT required)

| Method | Path                    | Body                      | Success | AuthZ           |
|--------|-------------------------|---------------------------|---------|-----------------|
| POST   | `/api/v1/merchants`     | `CreateMerchantsReqModel` | 201     | **ADMIN** role  |
| GET    | `/api/v1/merchants`     | —                         | 200     | any authenticated |
| GET    | `/api/v1/merchants/{id}`| —                         | 200     | any authenticated |
| PUT    | `/api/v1/merchants/{id}`| `UpdateMerchantReqModel`  | 200     | **ADMIN** role  |
| DELETE | `/api/v1/merchants/{id}`| —                         | 200     | **ADMIN** role (soft delete) |

On create/update, `email` is validated (`@Email`); `name`, `status`, and audit fields are required. `apiKey` and `secret` are generated server-side (never accepted from the client) — these are the credentials the merchant later uses to call the payments API. A merchant lookup by `apiKey` is **Redis-cached** (see [Caching](#caching)).

### Payments — `/api/v1/payments` (API-key + HMAC, no JWT)

Every payments request must carry three headers:

| Header        | Meaning                                                            |
|---------------|--------------------------------------------------------------------|
| `X-Api-Key`   | the merchant's `api_key` (resolves the merchant)                   |
| `X-Timestamp` | Unix epoch **seconds**; rejected if more than **±300s** from now   |
| `X-Signature` | lowercase hex `HMAC-SHA256(secret, canonical)`, constant-time compared |

Canonical string (LF-separated): `apiKey \n timestamp \n METHOD \n path`. On success the filter resolves the merchant and the request proceeds; any failure returns **401** as an `ApiResponse`.

| Method | Path                      | Returns                            |
|--------|---------------------------|------------------------------------|
| GET    | `/api/v1/payments/ping`   | the authenticated merchant's `id`  |

```bash
# sign and call (recompute each time — the signature is time-bound)
APIKEY='<merchant api_key>'; SECRET='<merchant secret>'; TS=$(date +%s)
SIG=$(printf '%s\n%s\n%s\n%s' "$APIKEY" "$TS" "GET" "/api/v1/payments/ping" \
      | openssl dgst -sha256 -hmac "$SECRET" | sed 's/^.*= //')
curl -i -H "X-Api-Key: $APIKEY" -H "X-Signature: $SIG" -H "X-Timestamp: $TS" \
  http://localhost:8080/api/v1/payments/ping
# → 200 { "data": "<merchant-uuid>" }
```

### Error handling

Exceptions from **any** controller are caught by `GlobalExceptionHandler` (`@RestControllerAdvice`) and returned as an `ApiResponse` with `data: null` and a matching HTTP status. (Payment-filter failures are written directly by the filter, since it runs outside the MVC advice.)

| Condition                                   | Status | Example message                          |
|---------------------------------------------|--------|------------------------------------------|
| Bean-validation failure (`@Valid`)          | 400    | `password: must be at least 8 characters…`|
| `UnauthorizedException` / bad/absent token  | 401    | `Unauthorized` / `Invalid API credentials`|
| Access denied (`@PreAuthorize` fails)       | 403    | `Access denied`                          |
| `NotFoundException`                         | 404    | `Merchant not found: <id>`               |
| `ConflictException` (e.g. duplicate email)  | 409    | `Email already registered`               |
| Any other uncaught exception                | 500    | (the exception message)                  |

---

## Caching

`MerchantRepository.findByApiKey` — on the payments hot path — is annotated `@Cacheable("merchantByApiKey", key="#apiKey")`, backed by Redis (`CacheConfig`). Values are stored as **JSON** (survives DevTools restarts and entity-shape changes) with a TTL from `app.cache.merchant-ttl-minutes` (default 10). Cache misses are not cached; merchant `update`/`delete` evict the cache to stay coherent.

---

## Configuration

All runtime config lives in `src/main/resources/application.yaml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/mini_merchant_db
    username: admin
    password: abc123
  jpa:
    hibernate:
      ddl-auto: validate   # CHECK ONLY — never create/alter/drop tables
  flyway:
    enabled: true          # Flyway is the ONLY place schema is created & changed
  data:
    redis:
      host: localhost
      port: 6379

app:
  jwt:
    secret: ${JWT_SECRET:...}          # HMAC signing key (≥ 32 bytes); override in prod
    access-token-ttl-minutes: 15
    refresh-token-ttl-days: 7
  cache:
    merchant-ttl-minutes: 10           # optional; defaults to 10 if unset
```

> **Schema rule:** every schema change is a new Flyway migration under `db/migration/`. Hibernate is set to `validate`, so an entity that doesn't match the migrated schema fails startup.
>
> **Prod note:** always override `JWT_SECRET` with a long random secret; never ship the default.

---

## Build & Test

```bash
# Compile only (fast feedback)
./mvnw compile

# Build jar (skip tests)
./mvnw package -DskipTests

# Run all tests (needs Postgres + Redis up for the context test)
./mvnw test

# Run a single test class
./mvnw test -Dtest=MerchantServiceTest
```

Service/logic is unit-tested with JUnit 5 + Mockito + AssertJ. Notable infra-free tests:
`MerchantServiceTest`, `PaymentAuthServiceTest` (HMAC/timestamp/key cases), `MerchantRepositoryCacheTest` (cache hit/miss), `RegisterDtoValidationTest` (password policy), `AuthServiceTest`.

---

## Roadmap

- ✅ User auth (register / login / refresh / logout) on JWT + Spring Security
- ✅ Role-based authorization for merchant writes (`@PreAuthorize`)
- ✅ `payment/` API-key + HMAC request signing (scaffold endpoint)
- ✅ Redis-cached merchant lookup
- `order/` — create and query merchant orders
- `payment/` — real payment operations + MoMo / VNPay gateway integration, body-bound signatures
- `callback/` — verify and process gateway webhooks
- Wire the `transactions` / `ledger_entries` schema into a service/API layer
- Redis-backed idempotency keys and distributed locks for concurrent callbacks
```
