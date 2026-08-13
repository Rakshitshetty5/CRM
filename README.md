# FlowCRM – Event-Driven CRM Platform

> A production-ready CRM backend built using **Spring Boot**, demonstrating modern distributed systems patterns including **Idempotency**, **Transactional Outbox**, **Kafka**, **Redis Caching**, **Distributed Locking**, **Retry & Dead Letter Queue**, and **Rate Limiting**.

---

## 📖 Table of Contents

- Overview
- Features
- Architecture
- Tech Stack
- System Design
- Project Structure
- Database Design
- API Documentation
- Event Flow
- Distributed Systems Concepts
- Running the Project
- Docker
- Future Improvements
- License

---

# Overview

FlowCRM is a backend-focused CRM platform designed as a **Modular Monolith** using Spring Boot.

The project focuses on solving real-world backend engineering challenges instead of implementing simple CRUD operations.

It demonstrates how enterprise systems ensure:

- Reliable event publishing
- Idempotent APIs
- Concurrent request handling
- High availability
- Scalability
- Fault tolerance

---

# Features

## Authentication

- User Registration
- User Login
- JWT Authentication
- Role-based Authorization

---

## Lead Management

- Create Lead
- View Lead
- List Leads
- Assign Lead
- Change Lead Stage

Lead Stages

- NEW
- CONTACTED
- QUALIFIED
- DEMO_SCHEDULED
- PROPOSAL_SENT
- NEGOTIATION
- WON
- LOST

---

## Task Management

- Create Task
- Mark Task Completed

---

## Dashboard

- Total Leads
- Leads by Stage
- Assigned Leads
- Pending Tasks

---

## Notification

- Kafka Event Consumer
- Email Notification
- Retry Mechanism
- Dead Letter Queue

---

# High-Level Architecture

The application follows a **Modular Monolith Architecture**.

Major Components

- Spring Boot Application
- PostgreSQL
- Redis
- Apache Kafka
- External Email Provider

The Notification Module consumes Kafka events and sends email notifications asynchronously.

---

# Tech Stack

## Backend

- Java 21
- Spring Boot 3
- Spring Security
- Spring Validation
- Spring Data JPA

## Database

- PostgreSQL

## Cache

- Redis

## Messaging

- Apache Kafka

## Distributed Locking

- Redisson

## Rate Limiting

- Custom Rate Limiter

## API Documentation

- SpringDoc OpenAPI
- Swagger UI

## Build Tool

- Maven

## Containerization

- Docker
- Docker Compose

---

# System Design

## Modular Monolith

Instead of building multiple microservices, the project is implemented as a Modular Monolith.

Modules

- Authentication
- Lead
- Task
- Dashboard
- Notification

Each module has clear responsibilities while remaining within a single deployable application.

---

# Project Structure

```text
flowcrm/

├── auth/
├── lead/
├── task/
├── dashboard/
├── notification/
├── common/
├── config/
├── security/
├── exception/
├── outbox/
├── infrastructure/

src/
└── main
    ├── java
    └── resources

docker-compose.yml

README.md
```

---

# Database Design

Primary Database

PostgreSQL

Logical Data

- Business Data
- Transactional Outbox

Redis

- Cache
- Idempotency Store
- Distributed Lock
- Rate Limiting

---

# REST APIs

## Authentication

| Method | Endpoint |
|---------|----------|
| POST | /api/v1/auth/register |
| POST | /api/v1/auth/login |

---

## Leads

| Method | Endpoint |
|---------|----------|
| POST | /api/v1/leads |
| GET | /api/v1/leads |
| GET | /api/v1/leads/{id} |
| PATCH | /api/v1/leads/{id}/assign |
| PATCH | /api/v1/leads/{id}/stage |

---

## Tasks

| Method | Endpoint |
|---------|----------|
| POST | /api/v1/tasks |
| PATCH | /api/v1/tasks/{id}/complete |

---

## Dashboard

| Method | Endpoint |
|---------|----------|
| GET | /api/v1/dashboard |

---

# Distributed Systems Concepts

---

## Idempotency

### Why?

Network failures may cause clients to retry requests.

Without idempotency, duplicate leads could be created.

### Solution

Every Create Lead request requires an

```
Idempotency-Key
```

The key is stored in Redis.

If the same request is received again, the previously stored response is returned instead of creating another Lead.

---

## Transactional Outbox Pattern

### Problem

Updating the database and publishing a Kafka event separately can lead to inconsistencies.

Example

Lead saved

BUT

Kafka publish fails

The notification would never be sent.

### Solution

Within a single transaction

- Save Lead
- Save Outbox Event

The Outbox Publisher later publishes unpublished events to Kafka.

This guarantees reliable event delivery.

---

## Apache Kafka

Kafka is used for asynchronous communication.

Current Events

- Lead Created
- Lead Stage Changed

Consumers

- Notification Module

---

## Redis Cache

Frequently accessed data is cached.

Examples

- Lead Details
- Dashboard Statistics

Benefits

- Reduced database load
- Faster response times

---

## Distributed Lock

Lead assignment uses Redis distributed locking.

Problem

Two users attempt to assign the same lead simultaneously.

Solution

Acquire a Redis lock before updating the Lead.

Only one request succeeds.

---

## Rate Limiting

Public APIs are protected using Custom Rate Limiter backed by Redis.

Applied to

- Register
- Login
- Create Lead

This prevents abuse and brute-force attacks.

---

## Retry & Dead Letter Queue

Notification delivery may fail due to temporary external service issues.

Flow

Notification

↓

Retry

↓

Retry

↓

Retry

↓

Dead Letter Queue

Messages exceeding the retry limit are sent to the DLQ for later inspection.

---

# Lead Creation Flow

1. Client sends Create Lead request with Idempotency-Key.
2. Spring Boot checks Redis for an existing key.
3. If the key exists, the stored response is returned.
4. Otherwise, a database transaction begins.
5. Lead data is saved.
6. A Transactional Outbox event is saved in the same transaction.
7. Transaction commits successfully.
8. The Idempotency result is stored in Redis.
9. HTTP 201 Created is returned to the client.
10. Background Outbox Publisher reads unpublished events.
11. Event is published to Kafka.
12. Notification Module consumes the event.
13. Email notification is sent.
14. Failed notifications are retried.
15. After exhausting retries, the message is moved to the Dead Letter Queue.

---

# Security

- JWT Authentication
- Password Encryption (BCrypt)
- Stateless Authentication
- Role-based Authorization

---

# Swagger

Swagger UI

```
http://localhost:8080/swagger-ui.html
```

OpenAPI

```
http://localhost:8080/v3/api-docs
```

---

# Running the Project

## Clone Repository

```bash
git clone https://github.com/<username>/flowcrm.git
```

---

## Start Infrastructure

```bash
docker-compose up -d
```

---

## Run Application

```bash
./mvnw spring-boot:run
```

---

# Docker Services

- PostgreSQL
- Redis
- Apache Kafka

---

# Future Improvements

- React Frontend
- Elasticsearch
- Audit Dashboard
- Multi-tenancy
- WebSocket Notifications
- OpenTelemetry Distributed Tracing
- Prometheus & Grafana Monitoring
- CI/CD Pipeline
- Kubernetes Deployment

---

# Design Decisions

| Decision | Reason |
|----------|--------|
| Modular Monolith | Faster development while maintaining modular boundaries |
| PostgreSQL | ACID transactions and reliable persistence |
| Redis | Low-latency cache, distributed locking, idempotency, and rate limiting |
| Apache Kafka | Asynchronous event-driven communication |
| Transactional Outbox | Reliable event publishing without dual-write problems |
| JWT | Stateless authentication |
| Custom Rate Limiter | Simple and effective API rate limiting |
| Redisson | Reliable Redis-based distributed locking |

---

# License

This project is developed as part of a backend engineering hackathon and is intended for educational and demonstration purposes.