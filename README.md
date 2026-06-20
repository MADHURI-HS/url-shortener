# URL Shortener

A production-style URL shortening service built with Spring Boot, featuring Base62 encoding, Redis caching, and rate limiting — deployed live on Render.

**🔗 Live demo:** https://url-shortener-9iuz.onrender.com
**📦 Try it:** `POST /shorten` with `{ "longUrl": "https://example.com" }`

> Free-tier hosting note: the server spins down after periods of inactivity. The first request after idle time may take 30–50 seconds to respond while it wakes up.

---

## Features

- **URL shortening** — converts long URLs into short, unique codes using Base62 encoding
- **Fast redirects** — Redis-backed caching sits in front of PostgreSQL, so repeated lookups skip the database entirely
- **Rate limiting** — Bucket4j enforces per-client request limits using the token bucket algorithm
- **Persistent storage** — PostgreSQL with Spring Data JPA for durable URL mappings
- **Environment-based configuration** — Spring Profiles cleanly separate local development from production settings
- **Containerized deployment** — Dockerized and deployed on Render, with Redis hosted on Upstash

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java, Spring Boot, Spring Data JPA |
| Database | PostgreSQL |
| Caching | Redis (Upstash) |
| Rate Limiting | Bucket4j |
| Deployment | Docker, Render |

## API Reference

### Shorten a URL
```http
POST /shorten
Content-Type: application/json

{
  "longUrl": "https://example.com/some/very/long/path"
}
```

**Response:**
```json
{
  "shortUrl": "https://url-shortener-9iuz.onrender.com/q0U",
  "longUrl": "https://example.com/some/very/long/path"
}
```

### Redirect
```http
GET /{code}
```
Redirects to the original long URL. Cached in Redis for fast repeated access.

### Health Check
```http
GET /ping
```
Returns `pong`.

## Architecture

```
Client → Spring Boot Controller → Rate Limiter (Bucket4j)
                                        ↓
                              Redis Cache (check first)
                                        ↓ (cache miss)
                              PostgreSQL (persistent store)
```

- New short URLs are generated using an auto-incrementing ID converted to Base62, producing compact, collision-free codes.
- On redirect, the cache is checked before the database — a cache hit skips the DB lookup entirely.
- Rate limiting is applied per request to prevent abuse.

## Running Locally

### Prerequisites
- Java 21+
- Maven 3.9+
- PostgreSQL (running locally)
- Redis (running locally)

### Setup

1. Clone the repo:
   ```bash
   git clone https://github.com/MADHURI-HS/url-shortener.git
   cd url-shortener
   ```

2. Create a local database in PostgreSQL named `urlshortener`.

3. Set the required environment variable for your local DB password:
   ```bash
   export LOCAL_DB_PASSWORD=your_postgres_password
   ```

4. Run the application:
   ```bash
   ./mvnw spring-boot:run
   ```

   This uses the `local` Spring profile by default, connecting to PostgreSQL and Redis on `localhost`.

5. Test it:
   ```bash
   curl http://localhost:8080/ping
   ```

## Deployment

This project is deployed on [Render](https://render.com) using Docker, with:
- **PostgreSQL** — managed instance on Render
- **Redis** — managed instance on [Upstash](https://upstash.com)

Configuration is handled entirely through environment variables (`SPRING_PROFILES_ACTIVE=prod`), keeping all credentials out of source control.

## License

This project is open source and available for learning and reference purposes.
