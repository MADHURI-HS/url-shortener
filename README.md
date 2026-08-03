# URL Shortener

A Spring Boot URL shortening service with JWT-based user authentication, Redis-backed caching, PostgreSQL persistence, and per-client rate limiting. Each user registers/logs in and manages their own shortened URLs through a REST API and a simple web dashboard.

**🔗 Live demo:** https://url-shortener-9iuz.onrender.com

> Free-tier hosting note: the server spins down after periods of inactivity. The first request after idle time may take 30–50 seconds to respond while it wakes up.

---

## Features

- **User authentication** — registration and login secured with Spring Security, BCrypt password hashing, and stateless JWT sessions (`jjwt`, 15-minute token expiry)
- **Per-user URL ownership** — shortened URLs are tied to the authenticated user; users can list and delete only their own URLs
- **URL shortening** — converts long URLs into short, unique codes using Base62 encoding of an auto-incrementing counter
- **Duplicate handling** — re-shortening a URL a user has already shortened returns the existing short code instead of creating a new one
- **Fast redirects** — Redis-backed caching (`@Cacheable`/`@CacheEvict`) sits in front of PostgreSQL for short-code lookups, so repeated redirects skip the database
- **Rate limiting** — Bucket4j token-bucket algorithm limits each client IP to 5 shorten requests per minute (resolved via `X-Forwarded-For`, falling back to remote address)
- **Centralized error handling** — a `@RestControllerAdvice` global exception handler returns consistent JSON responses for validation errors, duplicate registrations, and invalid credentials
- **API documentation** — interactive Swagger UI via springdoc-openapi
- **Web dashboard** — static login, register, and dashboard pages for creating and managing short URLs from the browser
- **Environment-based configuration** — separate Spring Profiles (`local`, `docker`, `prod`) for database, Redis, and base-URL settings
- **Containerized deployment** — multi-stage Dockerfile, Docker Compose for local Postgres/Redis, deployed on Render with Redis hosted on Upstash

## Tech Stack

| Layer            | Technology                                  |
|-------------------|----------------------------------------------|
| Backend           | Java 21, Spring Boot 3.5, Spring Web         |
| Security          | Spring Security, JWT (jjwt 0.12.7), BCrypt   |
| Database          | PostgreSQL, Spring Data JPA                  |
| Caching           | Redis (Spring Cache abstraction)             |
| Rate Limiting     | Bucket4j (token bucket, in-memory per IP)    |
| API Docs          | springdoc-openapi (Swagger UI)               |
| Frontend          | HTML, CSS, vanilla JavaScript                |
| Deployment        | Docker, Docker Compose, Render, Upstash      |
| Boilerplate       | Lombok                                       |

## API Reference

### Auth

**Register**
```
POST /auth/register
Content-Type: application/json

{ "name": "Jane Doe", "email": "jane@example.com", "password": "secret" }
```

**Login** — returns a JWT
```
POST /auth/login
Content-Type: application/json

{ "email": "jane@example.com", "password": "secret" }
```

### URLs (require `Authorization: Bearer <token>`)

**Shorten a URL**
```
POST /api/shorten
Content-Type: application/json

{ "longUrl": "https://example.com/some/very/long/path" }
```
Response:
```
{
  "success": true,
  "message": "Short URL created successfully",
  "data": {
    "shortUrl": "https://url-shortener-9iuz.onrender.com/q0U",
    "longUrl": "https://example.com/some/very/long/path"
  }
}
```
Rate limited to 5 requests/minute per client IP; exceeding the limit returns `429 Too Many Requests`.

**Get my URLs**
```
GET /api/urls
```

**Delete a URL**
```
DELETE /api/urls/{shortCode}
```

### Redirect (public)
```
GET /{code}
```
Redirects (`302 Found`) to the original long URL. Lookup is cached in Redis; a cache miss falls through to PostgreSQL.

### API Docs
```
GET /swagger-ui.html
```

## Architecture

```
Client → Spring Security filter chain (JWT auth filter)
                    ↓
        Spring Boot Controller → Rate Limiter (Bucket4j, /api/shorten only)
                    ↓
          Redis Cache (checked first on redirect)
                    ↓ (cache miss)
          PostgreSQL (persistent store, Spring Data JPA)
```

- Short codes are generated from an in-memory `AtomicLong` counter (initialized from the highest existing URL ID) converted to Base62.
- `JwtAuthenticationFilter` runs once per request, validates the bearer token, and populates the Spring Security context so `@AuthenticationPrincipal` resolves the current user in controllers.
- Public endpoints (`/`, static pages, `/auth/**`, Swagger, and all `GET` requests for redirects) bypass authentication; `/api/**` requires a valid JWT.

## Running Locally

### Prerequisites
- Java 21+
- Maven 3.9+
- PostgreSQL (running locally)
- Redis (running locally)

### Setup

1. Clone the repo:
```
git clone https://github.com/MADHURI-HS/url-shortener.git
cd url-shortener
```

2. Create a local database in PostgreSQL named `urlshortener`.

3. Set required environment variables:
```
export LOCAL_DB_PASSWORD=your_postgres_password
export JWT_SECRET=your_jwt_signing_secret
```

4. Run the application:
```
./mvnw spring-boot:run
```
Uses the `local` Spring profile by default, connecting to PostgreSQL and Redis on `localhost`.

5. Test it:
```
curl http://localhost:8080/ping
```

### Run with Docker Compose

```
docker compose up --build
```
Spins up the app, PostgreSQL, and Redis together using the `docker` Spring profile.

## Deployment

Deployed on [Render](https://render.com) using the multi-stage Dockerfile, with:
- **PostgreSQL** — managed instance on Render
- **Redis** — managed instance on [Upstash](https://upstash.com)

Configuration is handled entirely through environment variables (`SPRING_PROFILES_ACTIVE=prod`), keeping credentials out of source control.

## License

This project is open source and available for learning and reference purposes.
