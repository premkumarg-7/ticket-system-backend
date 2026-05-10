# E2D Helpdesk — Microservices-Based Ticket Management System

<p align="center">
  <img src="https://img.shields.io/badge/Java-17-orange?style=for-the-badge&logo=java&logoColor=white"/>
  <img src="https://img.shields.io/badge/Spring Boot-3.x-6DB33F?style=for-the-badge&logo=springboot&logoColor=white"/>
  <img src="https://img.shields.io/badge/Apache Kafka-231F20?style=for-the-badge&logo=apachekafka&logoColor=white"/>
  <img src="https://img.shields.io/badge/PostgreSQL-316192?style=for-the-badge&logo=postgresql&logoColor=white"/>
  <img src="https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white"/>
  <img src="https://img.shields.io/badge/JWT-black?style=for-the-badge&logo=JSON%20web%20tokens"/>
  <img src="https://img.shields.io/badge/Spring Security-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white"/>
</p>

<p align="center">
  A production-grade, microservices-based helpdesk platform for employee issue tracking, ticket workflow management, and asynchronous notification processing — powered by Spring Boot, Apache Kafka, Spring Cloud Gateway, and Docker.
</p>

-----

## 📌 Table of Contents

- [Overview](#-overview)
- [Architecture](#-architecture)
- [Microservices](#-microservices)
- [Tech Stack](#-tech-stack)
- [Features](#-features)
- [Kafka Event Flow](#-kafka-event-flow)
- [Getting Started](#-getting-started)
- [API Reference](#-api-reference)
- [Database Schema](#-database-schema)
- [Docker Setup](#-docker-setup)
- [Security](#-security)
- [Project Structure](#-project-structure)
- [Author](#-author)

-----

## 🧩 Overview

**E2D Helpdesk** (Employee-to-Department Helpdesk) is a microservices-based internal ticketing system designed to streamline employee issue tracking and resolution workflows within an organization.

Employees raise support tickets, track status in real time, and receive asynchronous Kafka-driven notifications on ticket creation and assignment — while agents and admins manage workflows through role-protected APIs.

-----

## 🏗 Architecture

```
                        ┌──────────────────────────────┐
                        │    Spring Cloud API Gateway   │
                        │         (Port: 8080)          │
                        └──────────────┬───────────────┘
                                       │  JWT Validation & Routing
              ┌────────────────────────┼────────────────────────┐
              │                        │                        │
   ┌──────────▼──────┐     ┌──────────▼──────┐     ┌──────────▼──────┐
   │   Auth Service  │     │  Ticket Service  │     │  Notification   │
   │   (Port: 8081)  │◄────│  (Port: 8082)   │────►│    Service      │
   └──────────┬──────┘     └──────────┬──────┘     └──────────┬──────┘
              │          Feign Client  │  Kafka publish         │ Kafka consume
              │        (email check)   │  → e2d-notification    │
   ┌──────────▼────────────────────────────────────────────────▼──────┐
   │                        PostgreSQL Database                         │
   └────────────────────────────────────────────────────────────────────┘
                                       │
                        ┌──────────────▼──────────────┐
                        │         Apache Kafka          │
                        │    Topic: e2d-notification    │
                        └──────────────────────────────┘
```

**Key design decisions:**

- Kafka event is published **only after a successful DB commit** — ensures data consistency
- Kafka failures are caught and logged **without rolling back** the ticket save (resilient fire-and-forget)
- Assignee email is validated via **synchronous Feign REST call** to Auth Service before assignment

-----

## 🔧 Microservices

|Service                 |Port|Responsibility                                               |
|------------------------|----|-------------------------------------------------------------|
|**API Gateway**         |8080|Centralized routing, JWT validation, load balancing          |
|**Auth Service**        |8081|User registration, login, JWT issuance, email existence check|
|**Ticket Service**      |8082|Ticket CRUD, assignment workflow, Kafka event publishing     |
|**Notification Service**|—   |Kafka consumer — processes `e2d-notification` topic events   |

-----

## 🛠 Tech Stack

|Layer           |Technology                                         |
|----------------|---------------------------------------------------|
|Language        |Java 17                                            |
|Framework       |Spring Boot 3.x                                    |
|API Gateway     |Spring Cloud Gateway                               |
|Security        |Spring Security + JWT                              |
|Async Messaging |Apache Kafka (`KafkaTemplate<String, TicketEvent>`)|
|Persistence     |Spring Data JPA + JPQL                             |
|Database        |PostgreSQL                                         |
|HTTP Client     |Feign Client (`AuthServiceClient`)                 |
|Containerization|Docker + Docker Compose                            |
|Build Tool      |Maven                                              |
|Utilities       |Lombok, Slf4j                                      |

-----

## ✨ Features

### 🎫 Ticket Management

- Create, update, assign, and delete support tickets
- Multi-status workflow: `OPEN → IN_PROGRESS → RESOLVED → CLOSED`
- Paginated ticket listing via Spring Data `Pageable`
- Filter tickets by status

### 👥 User & Role Management

- Role-based access control: `EMPLOYEE`, `AGENT`, `ADMIN`
- Secure login with JWT token issuance via Auth Service
- Assignee email validated against Auth Service via Feign before assignment

### ⚡ Async Kafka Notifications

- Events published to Kafka topic **`e2d-notification`** on:
  - Ticket created → `TICKET_CREATED` event
  - Ticket assigned to agent → `TICKET_ASSIGNED` event
- Notification Service consumes and processes events asynchronously
- Resilient design: Kafka failure never blocks ticket persistence

### 🔐 Security

- Centralized JWT authentication at the Gateway level
- Role-based route authorization per microservice
- Stateless session management

-----

## 📨 Kafka Event Flow

### Topic: `e2d-notification`

### Payload: `KafkaTemplate<String, TicketEvent>`

-----

#### Event 1 — `TICKET_CREATED`

Published after a new ticket is successfully saved to PostgreSQL.

```java
TicketEvent event = new TicketEvent(
    savedTicket.getId(),          // UUID
    savedTicket.getTitle(),       // Ticket title
    creatorEmail,                 // Requester's email (from JWT)
    savedTicket.getAssignedTo(),  // Assignee (nullable at creation)
    null,                         // assignedToEmail is null for CREATED events
    TicketEventType.TICKET_CREATED
);
kafkaTemplate.send("e2d-notification", event);
```

-----

#### Event 2 — `TICKET_ASSIGNED`

Published when a ticket is assigned or reassigned to an agent.

```java
// Assignee validated first via Feign:
// authServiceClient.checkUserEmailExist(assignedToEmail)

TicketEvent event = new TicketEvent(
    updatedTicket.getId(),
    updatedTicket.getTitle(),
    creatorEmail,            // Original ticket creator
    updatedTicket.getAssignedTo(),
    assignedToEmail,         // New assignee's email
    TicketEventType.TICKET_ASSIGNED
);
kafkaTemplate.send("e2d-notification", event);
```

-----

#### Resilience Pattern

```
1. Save ticket to PostgreSQL  (@Transactional)
        ↓ success
2. Build TicketEvent object
        ↓
3. kafkaTemplate.send("e2d-notification", event)
        ↓ failure (network/broker issue)
4. log.error(...)  →  DO NOT throw  →  ticket remains saved
```

> This ensures ticket operations are never blocked by notification infrastructure issues.

-----

## 🚀 Getting Started

### Prerequisites

- Java 17+
- Maven 3.8+
- Docker & Docker Compose

### Clone the Repository

```bash
git clone https://github.com/premkumarg-7/ticket-system-backend.git
cd ticket-system-backend
```

### Run with Docker Compose

```bash
docker-compose up --build
```

Spins up: PostgreSQL, Kafka, Zookeeper, Auth Service, Ticket Service, API Gateway.

### Run Locally (without Docker)

1. Start Kafka locally on port `9092`
1. Update `application.yml` in each service with your DB + Kafka config
1. Run services:

```bash
mvn clean install

cd auth        && mvn spring-boot:run   # Auth Service   :8081
cd ticketService && mvn spring-boot:run # Ticket Service  :8082
cd gateway     && mvn spring-boot:run   # API Gateway     :8080
```

-----

## 📡 API Reference

All requests route through **API Gateway** → `http://localhost:8080`

### Auth Endpoints

|Method|Endpoint            |Description        |Auth|
|------|--------------------|-------------------|----|
|POST  |`/api/auth/register`|Register new user  |❌   |
|POST  |`/api/auth/login`   |Login & receive JWT|❌   |

### Ticket Endpoints

|Method|Endpoint                      |Description                |Auth   |Kafka Event      |
|------|------------------------------|---------------------------|-------|-----------------|
|POST  |`/api/tickets`                |Create a ticket            |✅      |`TICKET_CREATED` |
|GET   |`/api/tickets`                |Get all tickets (paginated)|✅      |—                |
|GET   |`/api/tickets/{id}`           |Get ticket by ID           |✅      |—                |
|PUT   |`/api/tickets/{id}`           |Update ticket details      |✅      |—                |
|PATCH |`/api/tickets/{id}/assign`    |Assign to agent            |✅ ADMIN|`TICKET_ASSIGNED`|
|DELETE|`/api/tickets/{id}`           |Delete ticket              |✅ ADMIN|—                |
|GET   |`/api/tickets/status/{status}`|Filter by status           |✅      |—                |


> `Authorization: Bearer <token>` required for all protected routes.

-----

## 🗄 Database Schema

```
tickets
├── id            UUID (PK)
├── title         VARCHAR
├── description   TEXT
├── status        ENUM (OPEN, IN_PROGRESS, RESOLVED, CLOSED)
├── assigned_to   VARCHAR (assignee email, nullable)
├── created_by    VARCHAR (creator email)
├── created_at    TIMESTAMP
└── updated_at    TIMESTAMP
```

-----

## 🐳 Docker Setup

```yaml
services:
  zookeeper:
    image: confluentinc/cp-zookeeper:latest
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181

  kafka:
    image: confluentinc/cp-kafka:latest
    depends_on: [zookeeper]
    ports:
      - "9092:9092"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_AUTO_CREATE_TOPICS_ENABLE: 'true'

  postgres:
    image: postgres:14
    environment:
      POSTGRES_DB: e2d_helpdesk
      POSTGRES_USER: admin
      POSTGRES_PASSWORD: secret

  api-gateway:
    build: ./gateway
    ports: ["8080:8080"]

  auth-service:
    build: ./auth
    ports: ["8081:8081"]

  ticket-service:
    build: ./ticketService
    ports: ["8082:8082"]
```

-----

## 🔐 Security

- **JWT** — validated at Gateway; downstream services trust routed requests
- **Role-Based Access** — `EMPLOYEE`, `AGENT`, `ADMIN` with route-level protection
- **Feign Validation** — assignee email verified against Auth Service before DB write
- **Stateless** — no server-side sessions

-----

## 📁 Project Structure

```
ticket-system-backend/
├── auth/
│   └── src/main/java/
│       ├── Controller/
│       ├── Service/
│       ├── Repository/
│       └── Security/
├── gateway/
│   └── src/main/resources/application.yml
├── ticketService/
│   └── src/main/java/e2d/ticketService/
│       ├── Controller/
│       ├── Service/
│       │   └── TicketService.java        ← Kafka publish logic
│       ├── Repository/
│       ├── Entity/
│       │   └── Enum/
│       │       └── TicketEventType.java  ← TICKET_CREATED, TICKET_ASSIGNED
│       ├── DTO/
│       │   ├── TicketDTO.java
│       │   └── TicketEvent.java          ← Kafka message payload
│       ├── Mapper/
│       ├── Client/
│       │   └── AuthServiceClient.java    ← Feign client for email check
│       └── Exception/
├── docker-compose.yml
└── README.md
```

-----

## 👨‍💻 Author

**Premkumar G** — Backend Engineer (Java & Spring Boot)

[![LinkedIn](https://img.shields.io/badge/LinkedIn-0077B5?style=for-the-badge&logo=linkedin&logoColor=white)](https://linkedin.com/in/premkumarg7)
[![Portfolio](https://img.shields.io/badge/Portfolio-000000?style=for-the-badge&logo=netlify&logoColor=white)](https://premkumarg-portfolio.netlify.app)
[![GitHub](https://img.shields.io/badge/GitHub-100000?style=for-the-badge&logo=github&logoColor=white)](https://github.com/premkumarg-7)

-----

> ⭐ If you find this project useful, consider giving it a star!
