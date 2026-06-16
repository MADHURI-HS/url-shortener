# URL Shortener

A production-grade URL shortener (bit.ly clone) built with Java and Spring Boot.

## Tech Stack
- Java 17 + Spring Boot 3.x
- PostgreSQL (persistence)
- Redis (caching)
- Spring Data JPA (zero SQL written manually)
- Bucket4j (rate limiting)
- Maven

## Features
- 🔗 Shorten any URL to a Base62 short code
- ⚡ Redis cache-aside pattern (80% of redirects skip DB)
- 🔀 Real HTTP 302 redirects
- 🛡️ Input validation
- 🚦 Token bucket rate limiting (5 req/min per IP)
- 🔁 Deduplication — same URL returns same code

## API
| Method | Endpoint | Description |
|---|---|---|
| POST | `/shorten` | Shorten a long URL |
| GET | `/{code}` | Redirect to original URL |
| GET | `/ping` | Health check |

## Setup
1. Clone the repo
2. Create PostgreSQL database: `CREATE DATABASE urlshortener;`
3. Copy `application.properties.example` → `application.properties` and fill credentials
4. Start Redis: `brew services start redis`
5. Run: `mvn spring-boot:run`

## Author
**Madhuri H S** — Java & Spring Boot Engineer | Open to Work  
[LinkedIn](https://linkedin.com/in/madhuri-h-s) · [GitHub](https://github.com/MADHURI-HS)