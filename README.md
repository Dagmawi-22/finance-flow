# FinanceFlow

Ledger-first wallet and transaction platform built with Spring Boot, PostgreSQL, and Redis.

## Prerequisites

- Java 21+
- Maven 3.9+
- Docker & Docker Compose

## Quick start

Start infrastructure:

```bash
docker compose up -d
```

Run the application:

```bash
mvn spring-boot:run
```

Verify:

```bash
curl http://localhost:8080/api/v1/health
curl http://localhost:8080/actuator/health
```

OpenAPI UI: http://localhost:8080/swagger-ui.html

## Run tests

```bash
mvn test
```

Tests use Testcontainers — Docker must be running.

## Project structure

```
src/main/java/com/financeflow/
├── FinanceFlowApplication.java   # Entry point
└── api/                          # REST controllers

src/main/resources/
├── application.yml
└── db/migration/                 # Flyway SQL migrations
```

## Build roadmap

| Brick | Scope |
|-------|-------|
| **1** | Project scaffold, Docker, health checks |
| 2 | Wallet domain model & CRUD |
| 3 | Double-entry ledger & transactions |
| 4 | Idempotency (Redis) |
| 5 | Auth & role-based access |
| 6 | Audit logging & reconciliation |
