# Resource Booking System

A secure RESTful backend built with Spring Boot. Authenticated users can browse
bookable resources and manage their own reservations; administrators have full
control over resources and reservations.

## Features

- JWT-based, stateless authentication
- Role-based access control (`ADMIN`, `USER`)
- BCrypt password hashing
- Resource CRUD (admin-only writes)
- Reservation creation, viewing, updating, deletion
- Reservation ownership enforced server-side from the JWT — never from the request body
- Overlap prevention: a resource cannot be double-booked for the same window
- Filtering (status, min/max price), pagination, and sorting on reservations
- Centralized validation and a global exception handler with consistent error JSON
- Swagger / OpenAPI docs
- Seed users and sample resources for local testing
- Unit and integration tests (JUnit 5, MockMvc, H2 in-memory DB)

## Tech Stack

Java 17+, Spring Boot 3, Spring Security, JJWT, Spring Data JPA / Hibernate,
MySQL or PostgreSQL, Jakarta Validation, springdoc-openapi, JUnit 5, Mockito, Maven.

## Prerequisites

- Java 17+
- Maven 3.8+
- MySQL or PostgreSQL (or just use H2 for a zero-setup trial run — see below)
- Git

Verify:

```bash
java -version
mvn -version
```

## 1. Clone & Configure

```bash
git clone <your-repo-url>
cd resource-booking-system
cp .env.example .env
```

Edit `.env` with your database credentials and a strong `JWT_SECRET`
(at least 32 bytes / characters — used to sign HS256 tokens).

The app reads `.env` automatically in local development via the
`spring-dotenv` dependency, so you don't need to `export` the variables
yourself. In production, set real environment variables instead of shipping
a `.env` file.

## 2. Create the Database

MySQL:

```sql
CREATE DATABASE booking_system;
```

PostgreSQL:

```sql
CREATE DATABASE booking_system;
```

Then run with the `postgres` profile active:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=postgres
```

(Omit the profile flag to use MySQL, the default.)

Hibernate (`ddl-auto=update`) creates/updates the required tables automatically
on startup — no manual schema scripts needed.

### Quick trial without installing a database

The test suite runs against an in-memory H2 database automatically, so you can
verify everything works end-to-end with just:

```bash
mvn test
```

## 3. Build

```bash
mvn clean install
```

## 4. Run

```bash
mvn spring-boot:run
```

or:

```bash
java -jar target/booking-system.jar
```

The app starts on `http://localhost:8080`.

## 5. Explore the API

Swagger UI: `http://localhost:8080/swagger-ui/index.html`

Click **Authorize**, log in via `POST /api/auth/login` to get a token, then
paste the raw token (no `Bearer ` prefix needed in the Swagger dialog) to
authorize subsequent requests.

A Postman collection is also included at `postman/booking-system.postman_collection.json`.

## Seed Users

Created automatically on first startup (BCrypt-hashed, for local testing only):

| Username            | Password    | Role  |
| -------------------- | ----------- | ----- |
| admin@example.com    | Admin@123   | ADMIN |
| user@example.com     | User@123    | USER  |

Five sample resources (rooms, a vehicle, equipment) are seeded alongside them.

## API Summary

| Method | Endpoint                    | Access       | Purpose             |
| ------ | ---------------------------- | ------------ | -------------------- |
| POST   | `/api/auth/login`            | Public       | Login, returns JWT   |
| GET    | `/api/resources`             | USER/ADMIN   | List resources       |
| GET    | `/api/resources/{id}`        | USER/ADMIN   | Get resource         |
| POST   | `/api/resources`             | ADMIN        | Create resource       |
| PUT    | `/api/resources/{id}`        | ADMIN        | Update resource       |
| DELETE | `/api/resources/{id}`        | ADMIN        | Delete resource       |
| POST   | `/api/reservations`          | USER/ADMIN   | Create reservation    |
| GET    | `/api/reservations`          | USER/ADMIN   | List (own for USER)   |
| GET    | `/api/reservations/{id}`     | USER/ADMIN   | Get (own for USER)    |
| PUT    | `/api/reservations/{id}`     | ADMIN        | Update reservation    |
| DELETE | `/api/reservations/{id}`     | ADMIN        | Delete reservation    |

### Reservation query parameters

`GET /api/reservations?status=CONFIRMED&minPrice=500&maxPrice=5000&page=0&size=10&sort=price,desc`

All of `status`, `minPrice`, `maxPrice`, `page`, `size`, and `sort` are optional
and combinable.

## Key Security Design Notes

- **Ownership is never trusted from the client.** `ReservationRequest` has no
  `userId` field. The authenticated user is resolved from the JWT via
  `@AuthenticationPrincipal` and passed explicitly into the service layer on
  create; on read/update/delete, ownership is checked against the JWT-derived
  user, so changing a reservation ID in the URL can't leak another user's data
  (see `ReservationService.enforceOwnership`).
- **Stateless auth.** No HTTP session is created; every request is
  authenticated independently via the `Authorization: Bearer <token>` header,
  validated in `JwtAuthenticationFilter`.
- **Passwords** are BCrypt-hashed (`BCryptPasswordEncoder`), never stored or
  logged in plain text.
- **Reservation overlap** is checked at the database query level
  (`ReservationRepository.findOverlapping`) inside the same transaction as the
  write, using the classic `start1 < end2 AND start2 < end1` interval-overlap
  test; cancelled reservations don't block new bookings.

## Testing

```bash
mvn test
```

Tests run against an in-memory H2 database (`src/test/resources/application.properties`)
so no external database is required to run the suite. Coverage includes:

- **Auth**: successful login, bad username, bad password, missing credentials
- **Authorization**: USER vs ADMIN access on resources and reservations
- **Ownership**: a USER cannot read, update, or delete another user's reservation
- **Conflict detection**: overlapping vs back-to-back reservations
- **Validation**: negative price, missing fields, end time before start time,
  invalid status value, unknown resource ID
- **Filtering / pagination / sorting** on the reservations list endpoint
- **JWT**: token generation, validation against the right/wrong user, expiry

## Project Structure

```text
src/main/java/com/example/booking/
├── config/          # OpenAPI/Swagger config, startup data seeder
├── controller/       # REST controllers
├── dto/               # Request/response DTOs
├── entity/            # JPA entities
├── repository/        # Spring Data repositories
├── service/            # Business logic
├── security/            # JWT service, filter, Spring Security config
└── exception/            # Custom exceptions + global handler
```

## Environment Variables

See `.env.example`. Never commit a real `.env` file, database passwords, or
JWT secrets — `.gitignore` already excludes `.env` and `target/`.

## Future Improvements (out of scope for this submission)

Email notifications, self-service reservation cancellation, payment
integration, Redis caching, refresh tokens, audit logging, an admin
dashboard, an availability calendar, Docker/CI-CD, cloud deployment, and
more advanced conflict handling.
