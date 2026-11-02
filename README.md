# Library Management API

Simple RESTful API for managing borrowers and book copies.

## Tech Stack
- Java 17, Spring Boot 3.x
- Maven (package manager)
- Spring Data JPA + Hibernate
- PostgreSQL (prod) / H2 (test)
- Validation: jakarta.validation

## Database Choice - Justification
I chose **PostgreSQL/MySQL with JPA** for persistence because requirement #8 requires transactional safety: "no more than one member borrowing same book_id at a time". A relational DB with ACID transactions and unique constraints guarantees this. I use JPA/Hibernate for mapping.

For unit tests I use H2 in-memory DB for speed. The datasource can be swapped via env variable without code change (12-factor):
`spring.datasource.url=${DATABASE_URL:jdbc:h2:mem:library}`

Alternative like in-memory HashMap would lose data on restart and cannot enforce concurrency safely.

## How to Run

1. Start database (if using MySQL/Postgres) or skip if using H2
2. Configure `src/main/resources/application.properties`:
```properties
server.port=8080
spring.datasource.url=jdbc:mysql://localhost:3306/library_db
spring.datasource.username=root
spring.datasource.password=password
server.error.include-stacktrace=never
```
3. Run:
```bash
mvn spring-boot:run
# or in IntelliJ: Run LibraryManagementApiApplication.java
```
API at `http://localhost:8080`

## API Endpoints

### 1. Register Borrower
POST /api/v1/borrowers
```json
{
  "name": "Naufal",
  "email": "naufal@test.com"
}
```
- 201 Created, 409 if email exists, 400 if validation fails

### 2. Register Book Copy
POST /api/v1/books
```json
{
  "isbn": "9780747532699",
  "title": "Harry Potter",
  "author": "J.K. Rowling"
}
```
- If ISBN exists with SAME title/author: creates new copy with new id (requirement #7)
- If ISBN exists with DIFFERENT title/author: 409 Conflict

### 3. Get All Books
GET /api/v1/books
Returns list with `available: true/false`

### 4. Borrow Book
POST /api/v1/books/{bookId}/borrow
```json
{ "borrowerId": 1 }
```
- 200 OK if available, 409 if already borrowed, 404 if book/borrower not found

### 5. Return Book
POST /api/v1/books/{bookId}/return
```json
{ "borrowerId": 1 }
```
- 200 OK, 400 if not currently borrowed

## Full Test Flow (11 steps)
See ASSUMPTIONS.md for assumptions. Full curl flow tested:
1. POST /borrowers -> id 1
2. POST /books (Harry Potter) -> id 1
3. POST /books same ISBN -> id 2 (new copy)
4. POST /books same ISBN diff title -> 409 (correct)
5. GET /books -> 2 available
6. POST /books/1/borrow -> BORROWED
7. GET /books -> 1 false, 1 true
8. POST /books/1/borrow again -> 409
9. POST /books/1/return -> RETURNED
10. POST /books/1/return again -> 400
11. GET /books -> both true

## Error Handling
All errors return:
```json
{
  "timestamp": "...",
  "status": 409,
  "error": "Conflict",
  "message": "...",
  "path": "/api/v1/books"
}
```
Validation errors include field details.
