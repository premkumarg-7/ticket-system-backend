# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Services Overview

Four Spring Boot microservices, all Java 21:

| Service | Build | Port | Spring Boot |
|---|---|---|---|
| `auth` | Gradle | 8081 | 3.2.5 |
| `gateway` | Gradle | 8001 | 3.5.10 |
| `ticketService` | Maven | 8088 | 4.0.1 |
| `notification-service` | Gradle | 9000 | 4.0.5 |

Infrastructure: single PostgreSQL 16 instance (two schemas: `auth`, `ticket`) + Kafka 3.9 (KRaft mode, no ZooKeeper).

## Build & Run Commands

### Full stack (local JARs, no Docker for services)

```bash
# Start infrastructure first
docker compose up -d postgres kafka

# Build + run all services (skips tests by default)
./build-and-run.sh

# Build only / run only / stop all
./build-and-run.sh build
./build-and-run.sh run
./build-and-run.sh stop
```

Service logs go to `logs/<name>.log`; PIDs are tracked in `.pids/`.

### Build individual services

```bash
# Gradle services (auth, gateway, notification-service)
cd auth && ./gradlew bootJar

# ticketService (Maven)
cd ticketService && ./mvnw clean package
```

### Run tests

```bash
# Gradle
cd auth && ./gradlew test
cd gateway && ./gradlew test
cd notification-service && ./gradlew test

# Maven (ticketService)
cd ticketService && ./mvnw test

# Run a single test class (Gradle)
./gradlew test --tests "e2d.auth.AuthApplicationTests"

# Run a single test class (Maven)
./mvnw test -Dtest=SomeTestClass
```

### Docker Compose (all services containerised)

```bash
docker compose up --build   # build images + start everything
docker compose down -v      # stop and remove volumes
```

### WSL / Windows note

When the project is under `/mnt/` (Windows drive via WSL), Gradle lock files fail with I/O errors. `build-and-run.sh` redirects Gradle's project cache to `~/.cache/ticket-system` automatically. When running Gradle manually from WSL on a mounted drive, add:

```bash
./gradlew bootJar --no-daemon --project-cache-dir="${HOME}/.cache/ticket-system/<service>/.gradle"
```

## Architecture

### JWT Authentication flow

```
Client → Gateway (8001) → validates JWT → injects headers → Downstream service
                                                              trusts headers only
```

The gateway's `JwtAuthFilter` validates the JWT and, on success, mutates the request to add:
- `X-User-Name` — subject (username)
- `X-User-Email` — user's email
- `X-Roles` — comma-separated roles (e.g. `ROLE_USER,ROLE_ADMIN`)

Downstream services (ticketService) do **not** re-validate JWTs. `GatewayHeaderFilter` reads these three headers and builds a Spring Security `Authentication` for method-level `@PreAuthorize` checks. This means downstream services must only be reachable through the gateway in production.

### Gateway routes

| Path prefix | Target | JWT required |
|---|---|---|
| `/auth/**` | auth:8081 | No |
| `/admin/**` | auth:8081 | Yes |
| `/api/ticket/**` | ticketService:8088 | Yes |

### Service-to-service communication

ticketService calls auth's `/internal/**` endpoints directly (bypassing the gateway, no JWT needed) to verify that an assignee email exists before saving. The auth `SecurityConfig` permits `/internal/**` without authentication.

### Kafka event flow

ticketService publishes `TicketEvent` records to the `e2d-notification` topic on two occasions:
- `TICKET_CREATED` — after a ticket is saved (fire-and-forget; save never rolls back due to Kafka failure)
- `TICKET_ASSIGNED` — when the assignee changes

notification-service consumes the `e2d-notification` topic (`notification-group`) and sends emails via SMTP (Gmail by default). `spring.json.use.type.headers: false` is set so deserialization uses the configured default type, not headers.

### Database schemas

Hibernate `ddl-auto: update` manages schema evolution — no migration tool. The `db/init/01-schemas.sql` init script creates the `auth` and `ticket` schemas on first Postgres container start.

### Roles & bootstrap

Three roles are created on auth-service startup: `ROLE_USER`, `ROLE_ADMIN`, `ROLE_AGENT`. An admin user is also seeded from `app.bootstrap.admin.*` config (defaults: `admin` / `admin123`). Role prefix (`ROLE_`) is stored in the database; `GatewayHeaderFilter` normalises incoming role strings to always carry the prefix before constructing authorities.

### Ticket entity key facts

- PK: UUID (auto-generated)
- Enums: `TicketStatus`, `TicketPriority` (stored as strings)
- `assignedTo`: stores the assignee's email address
- Extends `Auditable`: `createdBy`, `modifiedBy`, `createdAt`, `modifiedAt` populated by Spring Data JPA auditing. `AuditorAwareImpl` supplies the current principal from `SecurityContextHolder`.

### Swagger / OpenAPI

Unified Swagger UI is served by the gateway at `http://localhost:8001/swagger-ui.html`, aggregating docs from all three backend services via `/v3/api-docs/{auth|ticket|notification}` routes.

## Environment Configuration

Copy `.env.example` to `.env` in the root. Key variables:

```
DB_USER / DB_PASSWORD / DB_NAME    # shared across services
JWT_SECRET                          # must match between auth and gateway
ADMIN_USER / ADMIN_PASSWORD         # auth bootstrap
MAIL_USERNAME / MAIL_PASSWORD       # notification-service (Gmail app password)
SPRING_PROFILES_ACTIVE              # dev (default) or prod
```

Spring profiles: `dev` uses sensible localhost defaults; `prod` requires all env vars set explicitly (no fallbacks).
