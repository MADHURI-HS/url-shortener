# URL Shortener

A URL shortening service built with **Java 21, Spring Boot 3, MySQL and Redis**. Users register, log in with JWT authentication, create short links, list their own links and delete them. Redirects are served from a Redis cache, and link creation is rate limited per IP.

**Live demo:** https://url-shortener-app-n9px.onrender.com  
**API docs (Swagger UI):** https://url-shortener-app-n9px.onrender.com/swagger-ui.html

> ### ⏳ About the live demo (please read)
> The demo runs entirely on **free-tier hosting** (Render web service, free managed Redis and a free hosted MySQL). Free services are put to sleep after about 15 minutes without traffic.
>
> - **The first request after a pause can take 30–90 seconds** while the service wakes up, the JVM starts and the database and Redis connections are re-established. The page may look blank or stuck during this time. Please wait and refresh once.
> - After the service is awake, requests are fast again until it goes idle.
> - Free Redis and MySQL tiers also have limited memory, connections and storage, so heavy use may be slow or refused.
> - This is a limitation of the free plans, not of the application. Running the project locally with Docker (see below) starts in a few seconds.

---

## Table of Contents
- [Features](#features)
- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [API Reference](#api-reference)
- [Running Locally](#running-locally)
- [Configuration](#configuration)
- [Testing](#testing)
- [Deployment](#deployment)
- [Design Decisions](#design-decisions)
- [Known Limitations](#known-limitations)
- [Future Enhancements](#future-enhancements)
- [Project Structure](#project-structure)

---

## Features

- **JWT authentication.** Register and log in; tokens expire after 15 minutes and are validated on every request by a custom Spring Security filter. Passwords are hashed with BCrypt.
- **Per-user URL management.** Users can only list and delete their own links.
- **Base62 short codes.** Each code is generated from the row's database id, so codes are unique across restarts and across multiple instances.
- **Redis caching.** Redirect lookups are cached for 24 hours. A deleted link is evicted from the cache immediately, and "not found" results are never cached.
- **Rate limiting.** Bucket4j allows 5 shorten requests per minute per client IP and responds with HTTP 429 beyond that.
- **Input validation.** URLs must start with `http://` or `https://` and be at most 2048 characters. Passwords must be 8–72 characters. Validation errors return field-level messages.
- **Consistent API responses.** Every endpoint returns `{ "success", "message", "data" }`, with meaningful status codes (400, 401, 404, 409, 429).
- **Swagger / OpenAPI** documentation with a Bearer-token "Authorize" button.
- **Simple web UI.** Register, log in, shorten URLs and manage them from a dashboard. User-provided text is HTML-escaped to prevent XSS.
- **Containerized.** Multi-stage Dockerfile and a Docker Compose file for MySQL, Redis and the app.

## Tech Stack

| Layer | Technology |
|---|---|
| Language / runtime | Java 21 |
| Framework | Spring Boot 3.5 (Web, Validation, Data JPA, Cache, Security) |
| Database | MySQL 8 with Spring Data JPA / Hibernate |
| Cache | Redis (Spring Cache + Spring Data Redis, Lettuce) |
| Authentication | Spring Security, JWT (jjwt 0.12), BCrypt |
| Rate limiting | Bucket4j 7.6 |
| API docs | springdoc-openapi (Swagger UI) |
| Testing | JUnit 5, Mockito, Spring Boot Test, MockMvc, H2 (tests only) |
| Build / packaging | Maven (wrapper included), Docker, Docker Compose |
| Hosting (demo) | Render (free tier), hosted MySQL, hosted Redis |

## Architecture

```
Client
  │
  ▼
Spring Security filter chain ── JwtAuthenticationFilter (validates Bearer token)
  │
  ▼
Controllers ──► Rate limiter (Bucket4j, POST /api/shorten only)
  │
  ▼
UrlService
  ├── redirect:  Redis cache (key "urls::<code>", TTL 24h)
  │                 │ miss
  │                 ▼
  │              MySQL (Spring Data JPA)
  └── create / delete: MySQL, and the Redis entry is evicted on delete
```

**How a short code is created**
1. The URL is inserted with a temporary unique placeholder to obtain the auto-increment id.
2. The final code is `Base62(100000 + id)` (for example id 3 becomes `q0X`), saved in the same transaction.
3. If the same user submits the same URL again, the existing code is returned.

**Why the MySQL columns are case-sensitive.** MySQL's default collation is case-insensitive, which would make `q1a` and `q1A` collide on the unique index and would treat `https://x.com/Doc` and `https://x.com/doc` as the same URL. `src/main/resources/schema-mysql.sql` switches `short_code` and `long_url` to binary collations after Hibernate creates the tables. It runs automatically with the `local`, `docker` and `prod` profiles.

## API Reference

Base URL locally: `http://localhost:8080`. Interactive docs: `/swagger-ui.html`.

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/auth/register` | public | Create an account (`name`, `email`, `password` of 8–72 characters) |
| POST | `/auth/login` | public | Returns a JWT in `data.token` |
| POST | `/api/shorten` | Bearer token | Create a short URL (5 requests/min per IP) |
| GET | `/api/urls` | Bearer token | List the caller's URLs |
| DELETE | `/api/urls/{shortCode}` | Bearer token | Delete one of the caller's URLs |
| GET | `/{code}` | public | `302` redirect to the original URL, or `404` |

**Example**

```bash
# register and log in
curl -X POST localhost:8080/auth/register -H "Content-Type: application/json" \
  -d '{"name":"Jane","email":"jane@example.com","password":"secret123"}'

TOKEN=$(curl -s -X POST localhost:8080/auth/login -H "Content-Type: application/json" \
  -d '{"email":"jane@example.com","password":"secret123"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['token'])")

# shorten a URL
curl -X POST localhost:8080/api/shorten -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" -d '{"longUrl":"https://example.com"}'
# -> {"success":true,"message":"Short URL created successfully","data":{"shortUrl":"http://localhost:8080/q0U","longUrl":"https://example.com"}}

# follow the short link
curl -i localhost:8080/q0U          # HTTP/1.1 302, Location: https://example.com
```

**Status codes:** `200` success · `400` validation error · `401` missing, invalid or expired token, or wrong credentials · `404` unknown code or URL not owned by the caller · `409` email already registered · `429` rate limit exceeded.

## Running Locally

### Prerequisites
- Java 21 or newer
- Docker (for MySQL and Redis)

### Steps

```bash
git clone https://github.com/MADHURI-HS/url-shortener.git
cd url-shortener

# 1. start MySQL and Redis (the "urlshortener" database is created automatically)
docker compose up -d mysql redis

# 2. run the app (uses the "local" profile by default)
./mvnw spring-boot:run
```

Open http://localhost:8080 for the web UI or http://localhost:8080/swagger-ui.html for the API docs.

The `local` profile has development defaults (MySQL user `root`, password `root`, and a development-only JWT secret), so no environment variables are required. To override them, set `LOCAL_DB_USERNAME`, `LOCAL_DB_PASSWORD` or `JWT_SECRET`.

### Everything in Docker

```bash
docker compose up --build
```

Starts MySQL, Redis and the app together using the `docker` profile.

## Configuration

| Variable | Used by | Description |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | all | `local` (default), `docker` or `prod` |
| `JWT_SECRET` | docker, prod | Signing secret, **at least 32 characters** |
| `SPRING_DATASOURCE_URL` | docker, prod | e.g. `jdbc:mysql://HOST:3306/DB?useSSL=true&serverTimezone=UTC` |
| `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD` | docker, prod | Database credentials |
| `REDIS_URL` | prod | e.g. `rediss://default:PASSWORD@HOST:6379` |
| `REDIS_HOST` / `REDIS_PORT` | docker | Redis location in Compose |
| `APP_BASE_URL` | docker, prod | Public URL used to build short links |
| `PORT` | optional | HTTP port, default 8080 |

No secrets are committed. Production values come from environment variables only.

## Testing

```bash
./mvnw test
```

**60 automated tests**, no MySQL or Redis required:

- **Unit tests (JUnit 5 + Mockito):** `UrlService`, `AuthService`, `JwtService`, rate limiter, Base62 encoder and request validation.
- **Controller tests (MockMvc + mocked services):** auth, URL management and redirect endpoints, including 400, 401, 404, 409 and 429 responses.
- **Cache test:** verifies that repeated lookups hit the database once, that deleting a link evicts it, and that misses are not cached.
- **End-to-end flow test:** register, login, shorten, redirect, list, delete, and the redirect afterwards returning 404, through the real Spring context and security filter chain on in-memory H2.

> Tests run on H2, not MySQL, so MySQL-specific behavior (such as the collation described above) is verified manually. Testcontainers would be the next step.

## Deployment

The demo is deployed on **Render** using the multi-stage `Dockerfile`:

1. Create a MySQL 8 database on any provider that offers one (Render itself only offers PostgreSQL) and a Redis instance (for example Upstash).
2. Create a Render web service from this repository (Docker runtime).
3. Set the environment variables from the [Configuration](#configuration) table, with `SPRING_PROFILES_ACTIVE=prod`.
4. Deploy. Hibernate creates the tables on first start and `schema-mysql.sql` then applies the case-sensitive collations.

**About free-tier hosting.** Render's free web services sleep after about 15 minutes of inactivity and take 30–90 seconds to wake. Options if this matters:
- accept it for a portfolio demo (it is noted at the top of this README),
- keep the service warm with an external pinger such as UptimeRobot or cron-job.org (free, but not an officially supported fix), or
- move to a paid instance, which stays running.

## Design Decisions

- **Short codes derived from the database id** instead of random strings or an in-memory counter, so there are no collisions and no coordination between instances.
- **302 (not 301) redirects** so every click reaches the server, which keeps deletion effective and leaves room for click analytics.
- **Cache-aside with explicit eviction** on delete and no caching of misses, so a link stops working immediately and a newly created code is never hidden.
- **Stateless JWT authentication** with a short expiry. Rate limiting is applied only to link creation, which is the expensive write path.
- **Binary collation on `short_code` and `long_url`** because Base62 codes and URL paths are case-sensitive.
- **Client IP for rate limiting** is read from `CF-Connecting-IP` (set by Cloudflare, which sits in front of Render), then `X-Forwarded-For`, then the remote address.

## Known Limitations

- Rate-limit counters are in memory per instance and never evicted; with several instances they would not be shared.
- The JWT is stored in `localStorage` (readable by any script on the page) and cannot be revoked before it expires. Refresh tokens and `HttpOnly` cookies would be stronger.
- There is no email verification or password reset.
- `spring.jpa.hibernate.ddl-auto=update` is convenient; a migration tool such as Flyway is the production-grade choice.
- Duplicate detection (`findByLongUrlAndUser`) scans the `long_url` column without an index, which is fine for a small dataset but would need a hash column at scale.
- Free-tier hosting causes the cold starts described above.

## Future Enhancements

- Flyway migrations and Testcontainers-based tests against real MySQL and Redis
- Click analytics (counts, referrers, timestamps)
- Custom aliases and link expiry
- Prometheus and Grafana metrics for cache hit rate and latency
- Distributed rate limiting with Redis (Bucket4j Redis integration)
- CI pipeline (GitHub Actions) to run the tests on every push

## Project Structure

```
src/main/java/com/urlshortener
├── config/        Swagger / OpenAPI configuration
├── controller/    Auth, URL management, redirect and home controllers
├── dto/           Request and response objects with validation
├── exception/     Custom exceptions and the global exception handler
├── model/         JPA entities (User, Url)
├── repository/    Spring Data repositories
├── security/      JWT service, authentication filter, security configuration
├── service/       Auth, URL and rate-limiter services
└── util/          Base62 encoder
src/main/resources
├── application*.properties   common + local / docker / prod profiles
├── schema-mysql.sql          case-sensitive columns for MySQL
└── static/                   web UI (HTML, CSS, JavaScript)
src/test                      unit, controller, cache and end-to-end tests
```

## License

This project is open source and available for learning and reference purposes.

## Author

**Madhuri H S** · [LinkedIn](https://www.linkedin.com/in/madhuri-h-s/) · [GitHub](https://github.com/MADHURI-HS)
