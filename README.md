# Library Management API

A RESTful API for a simple library system. It lets you:

- register **borrowers**
- register **books** (each registration creates a new physical copy with its own id)
- list all book copies with their availability
- **borrow** and **return** a book copy

Only one borrower can hold a book copy at a time. The database enforces this, so it holds even when requests run concurrently.

---

## Table of Contents

1. [Tech Stack](#tech-stack)
2. [Project Structure](#project-structure)
3. [Domain Model](#domain-model)
4. [Getting Started](#getting-started)
   - [Prerequisites](#prerequisites)
   - [Option A: Docker Compose (app + database)](#option-a-docker-compose-app--database)
   - [Option B: Run locally with Maven](#option-b-run-locally-with-maven)
   - [Option C: Run with a throwaway Testcontainers database](#option-c-run-with-a-throwaway-testcontainers-database)
5. [Configuration](#configuration)
6. [API Reference](#api-reference)
7. [Error Handling](#error-handling)
8. [Testing](#testing)
9. [CI/CD](#cicd)
10. [Design Decisions](#design-decisions)
11. [Known Limitations & Future Work](#known-limitations--future-work)

---

## Tech Stack

| Concern            | Choice                                                    |
|--------------------|-----------------------------------------------------------|
| Language / Runtime | Java 17                                                   |
| Framework          | Spring Boot 4.1 (Web MVC, Data JPA, Validation, Actuator) |
| Build tool         | Maven (wrapper included: `./mvnw`)                        |
| Database           | PostgreSQL 16                                             |
| Schema migrations  | Flyway                                                    |
| ORM                | Hibernate (via Spring Data JPA)                           |
| API docs           | springdoc-openapi (Swagger UI)                            |
| Boilerplate        | Lombok                                                    |
| Testing            | JUnit 5, Mockito, AssertJ, Testcontainers                 |
| Containerization   | Docker (multi-stage build), Docker Compose                |
| CI/CD              | GitHub Actions → GitHub Container Registry (GHCR)         |

---

## Project Structure

```
library-management-api/
├── src/main/java/com/librarymanagement/library_management_api/
│   ├── LibraryManagementApiApplication.java   # Spring Boot entry point
│   ├── config/          # OpenAPI / Swagger configuration
│   ├── controller/      # REST endpoints (HTTP layer only, no business logic)
│   ├── service/         # Business rules and transactions
│   ├── repository/      # Spring Data JPA repositories
│   ├── entity/          # JPA entities (BookTitle, BookCopy, Borrower, Borrowing)
│   ├── dto/
│   │   ├── request/     # Incoming payloads + bean validation rules
│   │   └── response/    # Outgoing payloads (entities are never exposed directly)
│   └── exception/       # Custom exceptions + global exception handler
├── src/main/resources/
│   ├── application.yaml # Configuration (overridable via environment variables)
│   └── db/migration/    # Flyway SQL migrations (V1..V4)
├── src/test/java/...    # Unit tests (service layer) + Spring context test
├── Dockerfile           # Multi-stage image build
├── docker-compose.yml   # App + PostgreSQL stack
├── .env.example         # Template for docker compose variables
└── .github/workflows/ci-cd.yml
```

Each request goes through the layers in order: `Controller → Service → Repository → Database`.

---

## Domain Model

```
┌──────────────┐ 1      * ┌──────────────┐ 1      * ┌──────────────┐ *      1 ┌──────────────┐
│  book_title  │──────────│  book_copy   │──────────│  borrowing   │──────────│   borrower   │
├──────────────┤          ├──────────────┤          ├──────────────┤          ├──────────────┤
│ isbn (PK)    │          │ id (PK)      │          │ id (PK)      │          │ id (PK)      │
│ title        │          │ isbn (FK)    │          │ book_copy_id │          │ name         │
│ author       │          │ version      │          │ borrower_id  │          │ email (UQ)   │
└──────────────┘          └──────────────┘          │ borrowed_at  │          └──────────────┘
                                                    │ returned_at  │
                                                    └──────────────┘
```

- **BookTitle**: the catalogue entry. It is identified by ISBN, and the title and author are fixed for that ISBN.
- **BookCopy**: one physical copy. Its `id` is the "book id" used in the API. Several copies can share one ISBN.
- **Borrower**: a library member. The email is unique.
- **Borrowing**: one borrow event. `returned_at IS NULL` means the copy is still out.
  The full history is kept: rows are never deleted.

A copy is **available** when it has no borrowing with `returned_at IS NULL`.
A **partial unique index** (`uq_active_borrowing`) allows at most one such row per copy.

---

## Getting Started

### Prerequisites

| Tool           | Needed for                                    |
|----------------|-----------------------------------------------|
| Docker         | Options A and C, and for running the full test suite |
| JDK 17         | Options B and C, and running tests            |
| Maven          | Not required. Use the bundled `./mvnw`        |

Clone the repository:

```bash
git clone https://github.com/naufalazim/library-api.git
cd library-api
```

### Option A: Docker Compose (app + database)

This is the quickest way to get everything running. You don't need a local JDK.

```bash
cp .env.example .env        # optional: adjust DB credentials / port
docker compose up -d --build
```

This starts:
- `library-db`: PostgreSQL 16 on port `5432`, with data kept in the `pgdata` volume
- `library-api`: the application on port `8080` (or `APP_PORT`). It starts only after the DB health check passes.

Check that it is running:

```bash
curl http://localhost:8080/actuator/health
# {"status":"UP", ...}
```

Stop it:

```bash
docker compose down        # keep data
docker compose down -v     # also delete the database volume
```

### Option B: Run locally with Maven

1. Start PostgreSQL. The simplest way is to start only the database from Compose:
   ```bash
   docker compose up -d db
   ```
   You can also use any PostgreSQL instance. Create a database called `library` in it.

2. Run the app:
   ```bash
   ./mvnw spring-boot:run
   ```
   Or run `LibraryManagementApiApplication` from your IDE.

3. To point at a different database, set environment variables (see [Configuration](#configuration)):
   ```bash
   DB_URL=jdbc:postgresql://myhost:5432/library \
   DB_USERNAME=me DB_PASSWORD=secret \
   ./mvnw spring-boot:run
   ```

Flyway applies the migrations in `src/main/resources/db/migration` on startup.
Hibernate runs with `ddl-auto: validate`: it checks that the entities match the schema but never changes the schema.

### Option C: Run with a throwaway Testcontainers database

`TestLibraryManagementApiApplication` (in `src/test`) starts the app with a disposable PostgreSQL container. You don't need to set up a database. Docker must be running.

```bash
./mvnw spring-boot:test-run
```

Or run `TestLibraryManagementApiApplication` from your IDE. The data is lost when the app stops.

### Useful URLs (once running)

| URL                                         | Purpose                     |
|---------------------------------------------|-----------------------------|
| http://localhost:8080/swagger-ui.html       | Interactive API docs        |
| http://localhost:8080/v3/api-docs           | OpenAPI JSON spec           |
| http://localhost:8080/actuator/health       | Health check (with liveness/readiness probes) |
| http://localhost:8080/actuator/info         | App info                    |

---

## Configuration

All settings are in `src/main/resources/application.yaml` and can be overridden with environment variables. You don't need to change any code.

| Variable      | Default                                     | Description                         |
|---------------|---------------------------------------------|-------------------------------------|
| `PORT`        | `8080`                                      | HTTP port of the application        |
| `DB_URL`      | `jdbc:postgresql://localhost:5432/library`  | JDBC URL                            |
| `DB_USERNAME` | `postgres`                                  | Database user                       |
| `DB_PASSWORD` | `postgres`                                  | Database password                   |
| `SHOW_SQL`    | `true`                                      | Log the SQL that Hibernate generates |

Docker Compose also reads these from `.env` (see `.env.example`):

| Variable   | Default   | Description                        |
|------------|-----------|------------------------------------|
| `DB_NAME`  | `library` | Database name created in Postgres  |
| `APP_PORT` | `8080`    | Host port mapped to the app        |

> `.env` is git-ignored. Don't commit real credentials.

---

## API Reference

Base path: `/api/v1`. All request and response bodies are JSON.

| # | Method | Path                          | Description              | Success |
|---|--------|-------------------------------|--------------------------|---------|
| 1 | POST   | `/borrowers`                  | Register a borrower      | 201     |
| 2 | POST   | `/books`                      | Register a book copy     | 201     |
| 3 | GET    | `/books`                      | List all book copies     | 200     |
| 4 | POST   | `/books/{bookId}/borrow`      | Borrow a book copy       | 200     |
| 5 | POST   | `/books/{bookId}/return`      | Return a book copy       | 200     |

### 1. Register Borrower

`POST /api/v1/borrowers`

```json
{ "name": "Naufal", "email": "naufal@test.com" }
```

| Field   | Rules                       |
|---------|-----------------------------|
| `name`  | required, not blank         |
| `email` | required, valid email, unique |

**201 Created**
```json
{ "id": 1, "name": "Naufal", "email": "naufal@test.com" }
```

| Status | When                           |
|--------|--------------------------------|
| 400    | Validation failed              |
| 409    | A borrower with that email already exists |

### 2. Register Book Copy

`POST /api/v1/books`

```json
{ "isbn": "9780747532699", "title": "Harry Potter", "author": "J.K. Rowling" }
```

| Field    | Rules                                                        |
|----------|--------------------------------------------------------------|
| `isbn`   | ISBN-10 (`123456789X`) or ISBN-13 (`978…`/`979…`), digits only, no dashes |
| `title`  | required, not blank                                          |
| `author` | required, not blank                                          |

Each call creates a **new copy** with a new `id`:

- **New ISBN**: the title is created, then the first copy.
- **Existing ISBN with the same title and author**: another copy is added.
- **Existing ISBN with a different title or author**: `409 Conflict`.

Title and author must match exactly, including case.

**201 Created**
```json
{ "id": 1, "isbn": "9780747532699", "title": "Harry Potter", "author": "J.K. Rowling", "available": true }
```

### 3. List All Books

`GET /api/v1/books`

**200 OK**
```json
[
  { "id": 1, "isbn": "9780747532699", "title": "Harry Potter", "author": "J.K. Rowling", "available": false },
  { "id": 2, "isbn": "9780747532699", "title": "Harry Potter", "author": "J.K. Rowling", "available": true }
]
```

### 4. Borrow Book

`POST /api/v1/books/{bookId}/borrow`

```json
{ "borrowerId": 1 }
```

**200 OK**
```json
{
  "borrowingId": 1,
  "bookId": 1,
  "borrowerId": 1,
  "borrowedAt": "2026-11-02T10:15:30.123Z",
  "returnedAt": null,
  "status": "BORROWED"
}
```

| Status | When                                   |
|--------|----------------------------------------|
| 400    | `borrowerId` missing                   |
| 404    | Book copy or borrower does not exist   |
| 409    | Copy is already borrowed (by anyone)   |

### 5. Return Book

`POST /api/v1/books/{bookId}/return`

```json
{ "borrowerId": 1 }
```

**200 OK**
```json
{
  "borrowingId": 1,
  "bookId": 1,
  "borrowerId": 1,
  "borrowedAt": "2026-11-02T10:15:30.123Z",
  "returnedAt": "2026-11-02T11:00:00.456Z",
  "status": "RETURNED"
}
```

| Status | When                                              |
|--------|---------------------------------------------------|
| 400    | `borrowerId` missing, or the copy is not currently borrowed |
| 404    | Book copy does not exist                          |
| 409    | The copy was borrowed by a **different** borrower |

### End-to-end walkthrough (curl)

```bash
API=http://localhost:8080/api/v1
JSON='Content-Type: application/json'

# 1. Register a borrower                         -> 201, id 1
curl -s -X POST $API/borrowers -H "$JSON" -d '{"name":"Naufal","email":"naufal@test.com"}'

# 2. Register a book                             -> 201, id 1
curl -s -X POST $API/books -H "$JSON" -d '{"isbn":"9780747532699","title":"Harry Potter","author":"J.K. Rowling"}'

# 3. Same ISBN, same title/author                -> 201, id 2 (new copy)
curl -s -X POST $API/books -H "$JSON" -d '{"isbn":"9780747532699","title":"Harry Potter","author":"J.K. Rowling"}'

# 4. Same ISBN, different title                  -> 409
curl -s -X POST $API/books -H "$JSON" -d '{"isbn":"9780747532699","title":"Other","author":"J.K. Rowling"}'

# 5. List books                                  -> both available
curl -s $API/books

# 6. Borrow copy 1                               -> 200, BORROWED
curl -s -X POST $API/books/1/borrow -H "$JSON" -d '{"borrowerId":1}'

# 7. List books                                  -> copy 1 unavailable, copy 2 available
curl -s $API/books

# 8. Borrow copy 1 again                         -> 409
curl -s -X POST $API/books/1/borrow -H "$JSON" -d '{"borrowerId":1}'

# 9. Return copy 1                               -> 200, RETURNED
curl -s -X POST $API/books/1/return -H "$JSON" -d '{"borrowerId":1}'

# 10. Return copy 1 again                        -> 400
curl -s -X POST $API/books/1/return -H "$JSON" -d '{"borrowerId":1}'

# 11. List books                                 -> both available
curl -s $API/books
```

You can also run all of these from Swagger UI at `/swagger-ui.html`.

---

## Error Handling

Business errors are turned into a consistent JSON body by `GlobalExceptionHandler`:

```json
{
  "timestamp": "2026-11-02T10:15:30.123Z",
  "status": 409,
  "error": "Conflict",
  "message": "Book with id 1 is already borrowed",
  "path": "/api/v1/books/1/borrow",
  "errors": null
}
```

| Exception                   | HTTP status |
|-----------------------------|-------------|
| `BadRequestException`       | 400         |
| `ResourceNotFoundException` | 404         |
| `ConflictException`         | 409         |

> **Note:** Bean-validation failures (`@Valid`), such as a blank name or an invalid ISBN, return **400**. They currently use Spring Boot's default error format, not the body above, so the `errors` field-list is not filled in yet. See [Known Limitations](#known-limitations--future-work).

---

## Testing

### Run all tests

```bash
./mvnw test        # or ./mvnw verify (what CI runs)
```

The full suite includes `LibraryManagementApiApplicationTests`. It starts the whole Spring context against a real PostgreSQL container through **Testcontainers**, so **Docker must be running**.

### Run only the unit tests (no Docker needed)

```bash
./mvnw test -Dtest='BorrowerServiceTest*,BorrowingServiceTest*'
```

> The trailing `*` matters. `BorrowingServiceTest` groups its tests in `@Nested` classes, and without the wildcard Surefire skips them.

### What is covered

| Test class                             | Type        | Covers |
|----------------------------------------|-------------|--------|
| `BorrowerServiceTest`                  | Unit (Mockito) | Successful registration; duplicate email → `ConflictException`, nothing saved |
| `BorrowingServiceTest` → `borrowBook`  | Unit (Mockito) | Success; book not found; borrower not found; already borrowed; DB unique-constraint race → `ConflictException` |
| `BorrowingServiceTest` → `returnBook`  | Unit (Mockito) | Success; book not found; not currently borrowed → `BadRequestException`; different borrower → `ConflictException`; borrower repository never queried |
| `LibraryManagementApiApplicationTests` | Integration | Spring context starts, Flyway migrations apply, and the JPA mappings match the schema |

The unit tests mock the repositories and don't start Spring, so they run in well under a second.

Reports are written to `target/surefire-reports/`.

---

## CI/CD

`.github/workflows/ci-cd.yml` runs on every push and pull request to `main`:

1. **Build & Test**: `./mvnw -B verify` on JDK 17. Testcontainers uses the Docker daemon on the runner. Test reports are uploaded as an artifact.
2. **Docker Compose smoke test**: builds the image, starts the app and database, waits for health checks, then calls the API with `curl` (health, register borrower, register book, list books).
3. **Publish image** (pushes to `main` only): builds the image and pushes it to `ghcr.io/<owner>/<repo>`, tagged `latest` and with the short commit SHA.

Pull the published image:

```bash
docker pull ghcr.io/naufalazim/library-api:latest
```

### Docker image

The `Dockerfile` is a multi-stage build:
- **Build stage** (`maven:3.9-eclipse-temurin-17`): dependencies are cached in their own layer, then the jar is built. Tests are skipped here because CI runs them.
- **Runtime stage** (`eclipse-temurin:17-jre`): runs as a **non-root** user, has a `HEALTHCHECK` against `/actuator/health`, and uses `-XX:MaxRAMPercentage=75` so the JVM respects container memory limits.
- Multi-arch base images, so it builds on both Apple Silicon and x86.

---

## Design Decisions

**Why PostgreSQL (a relational DB)?**
The key rule is "no more than one borrower can hold the same book copy at a time". That needs atomic, consistent writes.
A relational database with ACID transactions and constraints guarantees this even under concurrent requests.
An in-memory store would lose data on restart and would need hand-written locking.

**How the "one borrower per copy" rule is enforced** (defence in depth):
1. **Service check**: inside a transaction, `borrowBook` checks for an active borrowing (`returned_at IS NULL`) and returns 409 if one exists. This gives a clear error in the normal case.
2. **Database guarantee**: a partial unique index makes the DB itself reject a second active borrowing:
   ```sql
   CREATE UNIQUE INDEX uq_active_borrowing ON borrowing (book_copy_id) WHERE returned_at IS NULL;
   ```
   If two requests both pass step 1 at the same moment, one insert fails. The service catches the resulting `DataIntegrityViolationException` and returns 409.

**Separate `BookTitle` and `BookCopy`**
Storing ISBN, title, and author once per ISBN prevents copies with inconsistent data.
It also makes the rule "same ISBN must have the same title and author" a simple lookup.

**Availability is derived, not stored**
There is no `available` column. A copy's status is calculated from the `borrowing` table, so the two can never get out of sync.
The `borrowing` table also keeps the full borrow history.

**Flyway with `ddl-auto: validate`**
The schema is versioned in SQL and applied the same way in every environment. Hibernate only checks the mappings, so it can't silently change a production schema.

**DTOs instead of entities in the API**
Request records carry the validation rules. Response records keep the API stable when the internal model changes, and avoid lazy-loading or serialization problems.

**12-factor configuration**
Every environment-specific value comes from environment variables with sensible defaults for local development.

See [ASSUMPTIONS.md](ASSUMPTIONS.md) for the assumptions made where the requirements were silent.

---

## Known Limitations & Future Work

- **Validation error format**: add a `MethodArgumentNotValidException` handler so `@Valid` failures return the standard `ErrorResponse` with the `errors` field-list filled in.
- **Missing ISBN**: `isbn` only has a `@Pattern` rule, which accepts `null`. Adding `@NotBlank` would reject a missing ISBN with a proper 400.
- **Concurrent duplicate registration**: two simultaneous registrations with the same email or the same new ISBN can both pass the "exists" check. The DB constraint then rejects one of them, but that surfaces as a 500 rather than a 409.
- **`GET /books` performance**: availability is checked with one query per copy (N+1). For large catalogues, use a single join query and add pagination.
- **No authentication or authorization**: anyone can call any endpoint.
- **No update or delete endpoints**: these weren't in the requirements.
- **Integration tests**: add `@WebMvcTest` controller tests and repository tests backed by Testcontainers.
