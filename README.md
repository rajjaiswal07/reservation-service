# Reservation Service

A production-oriented ticket reservation service built with **Java 21, Spring Boot, PostgreSQL, Flyway, Docker, and Render**.

The service demonstrates reliable seat reservation under concurrent requests, idempotent APIs, transaction-based consistency, per-user reservation limits, observability, and automated deployment.

---

## 1. Live Application

The application is deployed on Render.

| Resource              | URL                                                                     |
| --------------------- | ----------------------------------------------------------------------- |
| Application           | https://reservation-service-xdd7.onrender.com                           |
| Swagger UI            | https://reservation-service-xdd7.onrender.com/swagger-ui.html           |
| OpenAPI Specification | https://reservation-service-xdd7.onrender.com/v3/api-docs               |
| Health                | https://reservation-service-xdd7.onrender.com/actuator/health           |
| Liveness              | https://reservation-service-xdd7.onrender.com/actuator/health/liveness  |
| Readiness             | https://reservation-service-xdd7.onrender.com/actuator/health/readiness |
| Prometheus Metrics    | https://reservation-service-xdd7.onrender.com/actuator/prometheus       |

---

## 2. Technology Stack

* Java 21
* Spring Boot 4
* Spring JDBC / JdbcTemplate
* PostgreSQL
* Flyway
* Spring Boot Actuator
* Micrometer / Prometheus
* Springdoc OpenAPI / Swagger
* Docker
* Render
* Maven

---

## 3. Architecture

The service follows a simple layered architecture:

```text
Client
  |
  v
REST Controllers
  |
  v
Service Layer
  |
  v
JdbcTemplate
  |
  v
PostgreSQL
```

Supporting components:

```text
                    +------------------+
                    |   REST Client    |
                    +--------+---------+
                             |
                             v
                    +------------------+
                    |   Controllers    |
                    +--------+---------+
                             |
                             v
                    +------------------+
                    |    Services      |
                    +--------+---------+
                             |
             +---------------+---------------+
             |                               |
             v                               v
       +-----------+                  +-------------+
       | JdbcTemplate|                 |  Metrics    |
       +-----+-----+                  +-------------+
             |
             v
       +-------------+
       | PostgreSQL  |
       +-------------+
```

---

## 4. Core APIs

### Create Event

```http
POST /api/events
```

Request:

```json
{
  "name": "Test Concert"
}
```

Example response:

```json
{
  "eventId": 2
}
```

---

### Create Seats

```http
POST /api/events/{eventId}/seats
```

Request:

```json
{
  "seatNumbers": [
    "A01",
    "A02",
    "A03",
    "A04"
  ]
}
```

---

### Get Event Seats

```http
GET /api/events/{eventId}/seats
```

To return only available seats:

```http
GET /api/events/{eventId}/seats?availableOnly=true
```

---

### Create Reservation

```http
POST /api/reservations
```

Required header:

```text
Idempotency-Key: unique-request-key
```

Optional correlation header:

```text
X-Correlation-Id: request-123
```

Request:

```json
{
  "eventId": 2,
  "userId": "user-001",
  "seatIds": [6]
}
```

Successful response:

```json
{
  "reservationId": "181bb9da-015b-43e9-9305-98b6a8b5f306",
  "userId": "user-001",
  "status": "CONFIRMED",
  "seatIds": [6]
}
```

---

### Get Reservation

```http
GET /api/reservations/{reservationId}
```

---

## 5. Swagger / OpenAPI

The complete API can be explored and executed through Swagger UI:

https://reservation-service-xdd7.onrender.com/swagger-ui.html

Swagger allows the APIs to be tested directly against the deployed application without running the service locally.

---

## 6. Reservation Consistency

Reservation creation is executed inside a database transaction.

The requested seats are locked using PostgreSQL row-level locking:

```sql
SELECT id, event_id, status
FROM seats
WHERE id IN (...)
ORDER BY id
FOR UPDATE;
```

Availability is checked only after acquiring the locks.

The following operations occur within the same transaction:

1. Lock requested seats
2. Validate seat availability
3. Create reservation
4. Create reservation-seat relationships
5. Mark seats as `RESERVED`

If any operation fails, the entire transaction is rolled back.

This prevents partially completed reservations.

---

## 7. Concurrent Reservation Handling

The system uses PostgreSQL `SELECT ... FOR UPDATE` to serialize concurrent attempts to reserve the same seat.

For example, if multiple users simultaneously attempt to reserve the same seat:

```text
Request 1 ----\
Request 2 -----\
Request 3 ------> PostgreSQL row lock ---> Seat
Request 4 -----/
Request 5 ----/
```

Only one transaction can successfully observe the seat as `AVAILABLE` and reserve it.

Subsequent requests receive:

```text
409 Conflict
```

with:

```text
One or more seats are no longer available
```

Requested seats are locked in ascending ID order to reduce the possibility of deadlocks when multiple seats are requested.

---

## 8. Idempotency

Reservation creation requires an `Idempotency-Key`.

The key is scoped to the user.

The service stores a SHA-256 request hash together with the idempotency key.

### Same key + same request

The original reservation is returned.

```text
Request
   |
   +-- Same Idempotency-Key
   +-- Same Request Body
           |
           v
     Original Reservation
```

### Same key + different request

The request is rejected with:

```text
409 Conflict
```

Example:

```text
Idempotency key was already used with a different request
```

This prevents accidental duplicate reservations during client retries.

---

## 9. Per-User Reservation Limit

The default maximum number of confirmed reservations per user is:

```text
5
```

The limit is configurable through:

```text
MAX_RESERVATIONS_PER_USER
```

The reservation flow uses a PostgreSQL transaction-level advisory lock per user so concurrent requests from the same user cannot bypass the reservation limit.

Example:

```text
Reservation 1 -> CONFIRMED
Reservation 2 -> CONFIRMED
Reservation 3 -> CONFIRMED
Reservation 4 -> CONFIRMED
Reservation 5 -> CONFIRMED
Reservation 6 -> 409 Conflict
```

---

## 10. Database

PostgreSQL is used as the primary persistence layer.

Flyway manages schema migrations.

Main tables:

```text
events
seats
reservations
reservation_seats
```

Important constraints include:

* Unique event/seat combination
* Unique user/idempotency-key combination
* Foreign-key relationships
* Seat status validation
* Reservation-seat relationships

The schema is version controlled through Flyway migrations under:

```text
src/main/resources/db/migration/
```

---

## 11. Observability

### Health

```http
GET /actuator/health
```

### Liveness

```http
GET /actuator/health/liveness
```

### Readiness

```http
GET /actuator/health/readiness
```

Readiness verifies that the application can communicate with its database.

### Prometheus

```http
GET /actuator/prometheus
```

Custom metrics include:

```text
reservation_confirmed
reservation_declined{reason="seat-taken"}
reservation_declined{reason="per-user-limit"}
reservation_declined{reason="idempotent-replay"}
reservation_seats_available
```

---

## 12. Correlation IDs

Every request supports:

```text
X-Correlation-Id
```

If a client does not provide one, the service generates a UUID.

The correlation ID is returned in the response header and is also included in application logs.

Example:

```text
X-Correlation-Id: testing-123
```

This allows a request to be traced through the application logs.

---

## 13. Error Handling

The service provides consistent API error responses.

Example:

```json
{
  "timestamp": "2026-10-04T11:46:32.232761321Z",
  "status": 409,
  "error": "Conflict",
  "message": "One or more seats are no longer available"
}
```

Common responses include:

| Status | Meaning                                     |
| ------ | ------------------------------------------- |
| 201    | Resource created                            |
| 400    | Invalid request                             |
| 404    | Resource not found                          |
| 409    | Reservation conflict / idempotency conflict |
| 500    | Unexpected server error                     |

---

## 14. Testing

The deployed application was tested against the Render environment.

Tests include:

* Event creation
* Seat creation
* Seat retrieval
* Available-seat filtering
* Successful reservation
* Reservation retrieval
* Multi-seat reservation
* Already-reserved seat conflict
* Idempotent retry
* Idempotency key/request mismatch
* Per-user reservation limit
* Concurrent reservation attempts
* Correlation IDs
* Health checks
* Prometheus metrics

---

## 15. Concurrency Test Script

The repository contains:

```text
scripts/hot-seat-test.sh
```

The script sends multiple concurrent reservation requests for the same seat.

Example:

```bash
EVENT_ID=2 SEAT_ID=25 REQUESTS=20 ./scripts/hot-seat-test.sh
```

Expected result:

```text
201 Created : 1
409 Conflict: 19
Other       : 0
PASS: Exactly one request reserved the hot seat.
```

This validates that concurrent requests cannot double-book a seat.

---

## 16. Reconciliation Script

The repository also contains:

```text
scripts/reconcile.sh
```

It checks:

1. Final seat state
2. Reservation metrics

Example:

```bash
EVENT_ID=2 ./scripts/reconcile.sh
```

---

## 17. Docker

The application is containerized using Docker.

The Docker build uses:

```text
Maven + Eclipse Temurin 21
```

The final runtime image uses:

```text
Eclipse Temurin 21 JRE
```

The application listens on the port provided by the `PORT` environment variable.

---

## 18. Deployment

The application is deployed on Render.

Deployment flow:

```text
GitHub
   |
   | push to main
   v
Render
   |
   v
Docker Build
   |
   v
Spring Boot Application
   |
   v
PostgreSQL
```

Render automatically deploys changes pushed to the `main` branch.

---

## 19. Local Development

The application can also be built using Maven and Java 21.

However, the deployed Render environment is the primary demonstration environment for this assignment.

The Swagger UI provides a convenient way to interact with the deployed API without requiring the reviewer to run the application locally.

---

## 20. Design Decisions

### Consistency over availability

The reservation path prioritizes consistency.

A reservation is confirmed only when the database transaction successfully establishes both:

```text
Reservation
+
Seat state
```

Readiness also depends on database connectivity, so the service fails closed when the database is unavailable.

### Database locking

PostgreSQL is used as the source of truth for seat availability.

Application-level synchronization is intentionally avoided because multiple application instances may run concurrently.

### Idempotency

Idempotency is implemented at the database level using a unique constraint on:

```text
(user_id, idempotency_key)
```

This ensures duplicate requests cannot create multiple reservations.

---

## 21. AI Assistance

AI assistance was used during development for:

* API design
* Concurrency design review
* PostgreSQL locking approach
* Idempotency implementation
* Observability design
* Test scripting
* Documentation

All generated code was reviewed, integrated, deployed, and tested against the running application.

---

## 22. Future Improvements

Potential future enhancements include:

* Reservation holds with expiration
* Background cleanup of expired holds
* Authentication and authorization
* Rate limiting
* Distributed tracing
* Automated integration tests using Testcontainers
* CI/CD pipeline
* Structured JSON logging
* Distributed locking if the architecture requires cross-database coordination

---

## 23. Repository Structure

```text
reservation-service/
│
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/pytm/reservationservice/
│   │   │       ├── controller/
│   │   │       ├── dto/
│   │   │       ├── exception/
│   │   │       ├── filter/
│   │   │       ├── metrics/
│   │   │       └── service/
│   │   │
│   │   └── resources/
│   │       ├── db/migration/
│   │       └── application.yaml
│
├── scripts/
│   ├── hot-seat-test.sh
│   └── reconcile.sh
│
├── Dockerfile
├── pom.xml
└── README.md
```

---

## 24. Quick Start for Reviewers

The fastest way to evaluate the deployed service is:

### 1. Open Swagger

https://reservation-service-xdd7.onrender.com/swagger-ui.html

### 2. Create an event

```http
POST /api/events
```

### 3. Add seats

```http
POST /api/events/{eventId}/seats
```

### 4. Reserve seats

```http
POST /api/reservations
```

Include:

```text
Idempotency-Key
```

### 5. Verify the reservation

```http
GET /api/reservations/{reservationId}
```

### 6. Check observability

```text
/actuator/health
/actuator/health/liveness
/actuator/health/readiness
/actuator/prometheus
```

### 7. Review the concurrency test

```text
scripts/hot-seat-test.sh
```

The complete API is available through Swagger and the source code is available in the repository.
