# Mini Merchant Server

A lightweight payment integration service built with Spring Boot that connects merchants to Vietnamese payment gateways (MoMo, VNPay). Designed to handle high-concurrency payment flows with Redis-backed idempotency and PostgreSQL for transaction persistence!

---

## What This Project Does

- Exposes a REST API for initiating and verifying payments via **MoMo** and **VNPay**
- Handles **payment callbacks/webhooks** from the gateways and updates order status atomically
- Uses **Redis** to deduplicate concurrent payment requests (idempotency keys) and cache session/token state
- Uses **PostgreSQL** to persist orders, transactions, and audit logs
- Built for **high-concurrency** scenarios: Redis locks prevent double-processing of the same payment event; the stateless REST layer scales horizontally behind a load balancer

---

## Tech Stack

| Layer        | Technology                    |
|--------------|-------------------------------|
| Runtime      | Java 21                       |
| Framework    | Spring Boot 3.5               |
| Persistence  | PostgreSQL 16 + Spring Data JPA |
| Cache / Lock | Redis 7 + Spring Data Redis   |
| Build        | Maven (mvnw wrapper)          |
| Containers   | Docker Compose                |

---

## Project Structure

```
src/main/java/com/mini_merchant/pay/
│
├── PayApplication.java          # Spring Boot entry point
│
└── <domain>/                    # One package per business domain
    ├── controller/              # REST endpoints (@RestController)
    ├── service/                 # Business logic (@Service)
    └── dto/                     # Request / response objects (Java records)
```

Planned domains:

```
ping/        # Health check (already exists)
order/       # Create and query merchant orders
payment/     # Initiate payment session with MoMo / VNPay
callback/    # Handle gateway webhook callbacks
```

---

## Getting Started

### Prerequisites

- Java 21
- Docker + Docker Compose
- Maven (or use the included `./mvnw` wrapper)

### 1. Start Infrastructure

```bash
docker compose up -d
```

This starts:
- **PostgreSQL 16** on `localhost:5432` (db: `mini_merchant_db`, user: `admin`, password: `abc123`)
- **Redis 7** on `localhost:6379`

### 2. Run the Application

```bash
./mvnw spring-boot:run
```

The server starts on `http://localhost:8080`.

### 3. Verify

```bash
curl http://localhost:8080/ping
# {"message":"pong"}
```

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
      ddl-auto: update   # schema auto-migrated from JPA entities

  data:
    redis:
      host: localhost
      port: 6379
```

For MoMo / VNPay credentials, add the gateway-specific keys here (or via environment variables) once the payment modules are implemented.

---

## How High-Concurrency Is Handled

```
Client (many parallel requests)
        │
        ▼
  Load Balancer
        │
   ┌────┴────┐
   │  App 1  │  App 2  │  App N  │   ← stateless; scale horizontally
   └────┬────┘
        │
        ▼
  Redis (idempotency key + distributed lock)
    → reject duplicate payment requests
    → serialize concurrent callbacks for same order
        │
        ▼
  PostgreSQL (single source of truth for order/transaction state)
```

Key patterns:
- **Idempotency keys** — each payment request carries a unique key stored in Redis; retries return the cached result without hitting the gateway again
- **Distributed locks** — Redis `SET NX PX` lock per `orderId` ensures only one callback writer at a time updates a transaction row
- **Optimistic locking** — JPA `@Version` on transaction entities prevents lost updates if the Redis lock is bypassed

---

## Build & Test

```bash
# Build jar (skip tests)
./mvnw package -DskipTests

# Run all tests
./mvnw test

# Run a single test class
./mvnw test -Dtest=PayApplicationTests
```

---

## Payment Gateway Flow (planned)

```
Client → POST /api/payments          (create payment session)
       ← {paymentUrl, orderId}

Client → redirect user to paymentUrl (MoMo / VNPay hosted page)

Gateway → POST /api/callbacks/momo   (gateway notifies result)
        → POST /api/callbacks/vnpay

Server → verify signature
       → update order status in PostgreSQL
       → notify merchant via webhook (optional)
```
