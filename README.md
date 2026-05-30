# FinanceFlow

Ledger-first wallet platform — Spring Boot, PostgreSQL, Redis.

**Requires:** Java 21, Maven 3.9, Docker

```bash
cp .env.example .env

# Full stack
docker compose up -d --build

# Local dev (infra only)
docker compose up -d postgres redis && mvn spring-boot:run
```

- Health: http://localhost:8080/api/v1/health
- Swagger: http://localhost:8080/swagger-ui.html
- Tests: `mvn test` (needs Docker)

**Roadmap:** wallets → ledger → idempotency → audit

Auth: `POST /api/v1/auth/login` → use `Authorization: Bearer <token>` on wallet endpoints.
