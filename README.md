# SeatSure — Seat Booking Backend

A backend application for managing events, shows, seat availability, seat reservations, bookings, and payments. Built with Java and Spring Boot, with PostgreSQL for persistent storage and Redis for temporary seat holds.

## Features

* **Authentication and authorization:** User registration, login, password hashing, and JWT-based authentication.
* **Event and venue management:** Manage events, venues, shows, and seats.
* **Seat availability:** Track seat availability and status for individual shows.
* **Temporary seat holds:** Use Redis to manage temporary reservations and prevent competing users from holding the same seat.
* **Booking management:** Create and manage bookings with booking-status and expiry handling.
* **Payment workflow:** Manage payment requests, payment status, and booking-related payment events.
* **Concurrency control:** Use optimistic locking to help prevent conflicting updates to seat records.
* **Exception handling:** Centralized handling of application errors.
* **Automated tests:** Test booking expiry and payment races, seat-hold cleanup retries, and optimistic locking.

## Technology Stack

* Java
* Spring Boot
* Spring Security
* Spring Data JPA and Hibernate
* PostgreSQL
* Redis
* JWT authentication
* Maven
* JUnit and Spring Boot testing

## Architecture

The application follows a layered backend architecture:

* **Controller layer:** Exposes REST API endpoints.
* **Service layer:** Implements business logic for authentication, events, seats, bookings, and payments.
* **Repository layer:** Uses Spring Data JPA to access persistent data.
* **Entity layer:** Models users, venues, events, shows, seats, bookings, booking items, and payments.
* **Security layer:** Handles authentication and authorization using Spring Security and JWT.
* **Redis integration:** Stores temporary seat-hold information with expiration.
* **Event handling:** Processes selected booking and payment-related operations after database transactions commit.

## Prerequisites

Install the following before running the application:

* Java version compatible with the project configuration
* PostgreSQL
* Redis
* Git

## Local Setup

### 1. Clone the repository

```bash
git clone https://github.com/nayan-9860/seatsure-backend.git
cd seatsure-backend
```

### 2. Configure environment variables

Copy `.env.example` to `.env` and replace the placeholder database credentials and JWT secret with your local values.

```bash
cp .env.example .env
```

The application expects these variables:

| Variable      | Purpose                        |
| ------------- | ------------------------------ |
| `DB_URL`      | PostgreSQL JDBC connection URL |
| `DB_USERNAME` | Database username              |
| `DB_PASSWORD` | Database password              |
| `JWT_SECRET`  | Secret used for JWT operations |
| `REDIS_HOST`  | Redis server hostname          |
| `REDIS_PORT`  | Redis server port              |

**Security:** Never commit `.env` or real credentials to GitHub. Use a fresh, securely generated JWT secret for each environment.

### 3. Start PostgreSQL and Redis

Ensure PostgreSQL and Redis are running and accessible. Create the application database and configure the database credentials.

The local Redis defaults are `localhost:6379`.

### 4. Load environment variables

The `.env` file is not automatically loaded by Spring Boot. In a macOS or Linux terminal, run:

```bash
set -a
source .env
set +a
```

You can alternatively configure these variables in your IDE's run configuration.

### 5. Run tests

```bash
./mvnw test
```

### 6. Start the application

After configuring the environment variables and required services:

```bash
./mvnw spring-boot:run
```

The application uses Spring Boot's configured server port unless overridden.

## Testing

Run the automated test suite using:

```bash
./mvnw test
```

The repository includes tests covering selected booking and payment race conditions, seat-hold cleanup retries, and optimistic locking.

## Configuration Notes

* Keep local credentials in `.env`.
* Use environment variables or your hosting platform's secret-management settings in deployment.
* Review database schema management before production deployment.
* Configure production PostgreSQL and Redis connection details separately from local development.

## Deployment

Deployment documentation will be added after the production configuration and hosting setup are finalized.

## Future Improvements

Potential improvements include:

* OpenAPI/Swagger API documentation
* Database schema migrations
* Integration tests using isolated database and Redis instances
* Production health checks and monitoring
* A persistent event/outbox strategy for reliable asynchronous processing

## Author

Nayan Joshi

GitHub: [nayan-9860](https://github.com/nayan-9860)
