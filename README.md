# E2D Helpdesk — Microservices-Based Ticket Management System

<p align="center">
  <img src="https://img.shields.io/badge/Java-21-orange?style=for-the-badge&logo=java&logoColor=white"/>
  <img src="https://img.shields.io/badge/Spring Boot-3.x / 4.x-6DB33F?style=for-the-badge&logo=springboot&logoColor=white"/>
  <img src="https://img.shields.io/badge/Apache Kafka-231F20?style=for-the-badge&logo=apachekafka&logoColor=white"/>
  <img src="https://img.shields.io/badge/PostgreSQL-316192?style=for-the-badge&logo=postgresql&logoColor=white"/>
  <img src="https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white"/>
  <img src="https://img.shields.io/badge/JWT-black?style=for-the-badge&logo=JSON%20web%20tokens"/>
  <img src="https://img.shields.io/badge/Spring Security-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white"/>
</p>

<p align="center">
  A production-grade, microservices-based helpdesk platform for employee issue tracking, ticket workflow management, and asynchronous email notification — powered by Spring Boot, Apache Kafka (KRaft), Spring Cloud Gateway, and Docker.
</p>

---

## 📌 Table of Contents

- [Overview](#-overview)
- [Architecture](#-architecture)
- [Microservices](#-microservices)
- [Tech Stack](#-tech-stack)
- [Features](#-features)
- [Kafka Event Flow](#-kafka-event-flow)
- [Getting Started](#-getting-started)
- [API Reference](#-api-reference)
- [Security](#-security)
- [Author](#-author)

---

## 🧩 Overview

**E2D Helpdesk** (Employee-to-Department Helpdesk) is a microservices-based internal ticketing system designed to streamline employee issue tracking and resolution workflows within an organisation.

Employees raise support tickets, agents manage and resolve them, and all stakeholders receive asynchronous email notifications on ticket creation and assignment — driven by Apache Kafka.

---

## 🏗 Architecture

```
                        ┌───────────────────────────────┐
                        │   Spring Cloud API Gateway     │
                        │   (Port: 8001)                 │
                        │   JWT validation & routing     │
                        └──────────────┬────────────────┘
                                       │
              ┌────────────────────────┼────────────────────────┐
              │                        │                        │
   ┌──────────▼──────┐     ┌──────────▼──────┐     ┌──────────▼──────┐
   │  Auth Service   │     │ Ticket Service   │     │  Notification   │
   │  (Port: 8081)   │◄────│  (Port: 8088)   │────►│    Service      │
   └──────────┬──────┘     └──────────┬──────┘     │  (Port: 9000)   │
              │         Feign Client   │             └─────────────────┘
              │        (email check)   │ Kafka publish → e2d-notification
   ┌──────────▼────────────────────────────────────────────────────────┐
   │            PostgreSQL 16  (schemas: auth | ticket)                │
   └───────────────────────────────────────────────────────────────────┘
                                       │
                        ┌──────────────▼──────────────┐
                        │   Apache Kafka (KRaft)        │
                        │   Topic: e2d-notification     │
                        └──────────────────────────────┘
```

**Key design decisions:**

- JWT is validated **once at the Gateway** — downstream services trust the `X-User-Name`, `X-User-Email`, and `X-Roles` headers the gateway injects
- Kafka events are published **only after a successful DB commit** — ticket saves never roll back due to a Kafka failure
- Assignee email is verified via a **synchronous Feign call** to Auth Service before the assignment is persisted
- Kafka runs in **KRaft mode** (no Zookeeper)

---

## 🔧 Microservices

| Service | Port | Build | Responsibility |
|---|---|---|---|
| **API Gateway** | 8001 | Gradle | JWT validation, CORS, routing to all services |
| **Auth Service** | 8081 | Gradle | User registration, login, JWT issuance, refresh tokens, admin management |
| **Ticket Service** | 8088 | Maven | Ticket CRUD, assignment workflow, Kafka event publishing |
| **Notification Service** | 9000 | Gradle | Kafka consumer — sends emails via SMTP on ticket events |

---

## 🛠 Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.x (auth, gateway) / 4.x (ticket, notification) |
| API Gateway | Spring Cloud Gateway (WebFlux) |
| Security | Spring Security + JWT (JJWT 0.12.6) |
| Async Messaging | Apache Kafka — KRaft mode, no Zookeeper |
| Persistence | Spring Data JPA + Hibernate (DDL auto-update) |
| Database | PostgreSQL 16 — single instance, two schemas |
| HTTP Client | OpenFeign (Spring Cloud 2025.1.x) |
| Email | Spring Mail — Gmail SMTP |
| Containerisation | Docker + Docker Compose |
| Build Tools | Gradle (auth, gateway, notification) · Maven (ticketService) |
| Utilities | Lombok, MapStruct, Springdoc OpenAPI |

---

## ✨ Features

### 🎫 Ticket Management

- Create, read, update, and delete support tickets
- Status workflow: `OPEN → IN_PROGRESS → RESOLVED → CLOSED`
- Priority levels: `LOW`, `MEDIUM`, `HIGH`, `CRITICAL`
- Paginated ticket listing (`/api/ticket/page` with Spring `Pageable`)
- Attach a list of file references per ticket
- JPA auditing — every ticket records `createdBy`, `modifiedBy`, `createdAt`, `modifiedAt` automatically

### 👥 User & Role Management

- Self-registration via `POST /auth/register`
- Admin-managed registration with explicit role assignment via `POST /admin/register`
- Three roles: `USER`, `AGENT`, `ADMIN` — seeded automatically on first startup
- Admin user bootstrapped from environment variables on first startup
- Refresh token support — obtain a new access token without re-logging in
- Logout — invalidates the current refresh token

### ⚡ Async Email Notifications

Events published to Kafka topic **`e2d-notification`**:

| Trigger | Event | Email recipient |
|---|---|---|
| Ticket created | `TICKET_CREATED` | Ticket creator |
| Ticket assigned | `TICKET_ASSIGNED` | Newly assigned agent |

- Notification Service consumes events asynchronously — the ticket operation is never blocked
- Kafka failures are caught and logged without rolling back the ticket save

### 🔐 Security

- JWT validated centrally at the Gateway — downstream services trust injected headers, no JWT re-parsing
- Role-based route protection via Spring Security `@PreAuthorize`
- Stateless sessions (no server-side state)
- CORS configured at the Gateway for configurable allowed origins

### 📖 API Documentation

Aggregated Swagger UI served by the Gateway at:
```
http://localhost:8001/swagger-ui.html
```
Covers auth, ticket, and notification service endpoints in one place.

---

## 📨 Kafka Event Flow

### Topic: `e2d-notification`

**Event 1 — `TICKET_CREATED`** — published after the ticket is saved to PostgreSQL:

```
1. POST /api/ticket  →  TicketService.createTicket()
2. ticket saved to DB  (@Transactional)
3. TicketEvent(TICKET_CREATED) published to Kafka
4. NotificationService consumes → email sent to creator
```

**Event 2 — `TICKET_ASSIGNED`** — published when the assignee changes:

```
1. PATCH /api/ticket/{id}  →  TicketService.updateAssignedTo()
2. Feign: AuthServiceClient.checkUserEmailExist(email)  ← validates assignee
3. assignedTo updated in DB
4. TicketEvent(TICKET_ASSIGNED) published to Kafka
5. NotificationService consumes → email sent to new assignee
```

---

## 🚀 Getting Started

### Prerequisites

- Docker & Docker Compose
- Java 21 (for local JAR builds)

### Environment setup

```bash
cp .env.example .env
# Fill in the required values — see comments in .env.example
```

### Run with Docker Compose

```bash
docker compose up --build
```

Starts PostgreSQL, Kafka (KRaft), Auth, Ticket, Notification, and Gateway.

### Run locally (JARs, without Docker for services)

```bash
# Start only infrastructure
docker compose up -d postgres kafka

# Build and run all services
./build-and-run.sh

# Or individually:
./build-and-run.sh build   # build JARs
./build-and-run.sh run     # start services
./build-and-run.sh stop    # stop services
```

---

## 📡 API Reference

All external requests go through the Gateway at `http://localhost:8001`.

### Auth (`/auth/**` — no JWT required)

| Method | Endpoint | Description |
|---|---|---|
| POST | `/auth/register` | Register a new user |
| POST | `/auth/login` | Login — returns access + refresh token |
| POST | `/auth/refresh` | Exchange a refresh token for a new access token |
| POST | `/auth/logout` | Invalidate the current refresh token |

### Admin (`/admin/**` — JWT + ADMIN role required)

| Method | Endpoint | Description |
|---|---|---|
| POST | `/admin/register` | Create a user with an explicit role |
| GET | `/admin/health` | Admin health check |
| GET | `/admin/support` | Support access check |

### Tickets (`/api/ticket/**` — JWT required)

| Method | Endpoint | Roles | Kafka Event |
|---|---|---|---|
| POST | `/api/ticket` | ADMIN, AGENT | `TICKET_CREATED` |
| GET | `/api/ticket` | USER, AGENT, ADMIN | — |
| GET | `/api/ticket/page` | USER, AGENT, ADMIN | — |
| GET | `/api/ticket/{id}` | USER, AGENT, ADMIN | — |
| PUT | `/api/ticket/{id}` | USER, AGENT, ADMIN | — |
| PATCH | `/api/ticket/{id}` | ADMIN, AGENT | `TICKET_ASSIGNED` |
| DELETE | `/api/ticket/{id}` | ADMIN | — |

> All protected routes require `Authorization: Bearer <token>` header.

---

## 🔐 Security

| Concern | Approach |
|---|---|
| Authentication | JWT — issued by Auth Service, validated by Gateway |
| Authorisation | Spring Security `@PreAuthorize` with role checks |
| Identity propagation | Gateway injects `X-User-Name`, `X-User-Email`, `X-Roles` headers |
| Assignee validation | Feign call to Auth's internal API verifies email before assignment |
| Sessions | Stateless — no server-side session |

---

## 👨‍💻 Author

**Premkumar G** — Backend Engineer (Java & Spring Boot)

[![LinkedIn](https://img.shields.io/badge/LinkedIn-0077B5?style=for-the-badge&logo=linkedin&logoColor=white)](https://linkedin.com/in/premkumarg7)
[![Portfolio](https://img.shields.io/badge/Portfolio-000000?style=for-the-badge&logo=netlify&logoColor=white)](https://premkumarg-portfolio.netlify.app)
[![GitHub](https://img.shields.io/badge/GitHub-100000?style=for-the-badge&logo=github&logoColor=white)](https://github.com/premkumarg-7)

---

> ⭐ If you find this project useful, consider giving it a star!
