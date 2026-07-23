# 🔗 URL Shortener

A production-ready URL Shortener application built with **Spring Boot**, **Spring Security**, **JWT Authentication**, **PostgreSQL**, **Redis**, and **Docker**. The application enables users to securely shorten URLs, manage their links, and redirect shortened URLs efficiently using caching and rate limiting.

## 🌐 Live Demo

🔗 https://url-shortener-app-n9px.onrender.com

## 📂 GitHub Repository

🔗 https://github.com/MADHURI-HS/url-shortener

---

## ✨ Features

- 🔐 User Registration & Login
- 🔑 JWT Authentication & Authorization
- 🔒 BCrypt Password Encryption
- 🔗 Shorten Long URLs
- 🚀 Redirect to Original URLs
- 📋 View User-Specific URLs
- 🗑️ Delete Shortened URLs
- ⚡ Redis Caching for Faster Redirects
- 🚦 Bucket4j Rate Limiting
- 🐳 Dockerized Application
- ☁️ Cloud Deployment on Render
- 📖 Swagger API Documentation

---

## 🛠️ Tech Stack

### Backend
- Java 21
- Spring Boot 3
- Spring Security
- Spring Data JPA
- Hibernate

### Database
- PostgreSQL

### Cache
- Redis

### Authentication
- JWT (JSON Web Token)
- BCrypt Password Encoder

### API Documentation
- Swagger / OpenAPI

### Build Tool
- Maven

### Deployment
- Docker
- Render

---

## 🏗️ System Architecture

```
                 +--------------------+
                 |      Client        |
                 |  (Browser / User)  |
                 +---------+----------+
                           |
                           | HTTP Requests
                           |
                 +---------v----------+
                 | Spring Boot REST API|
                 +---------+----------+
                           |
         +-----------------+------------------+
         |                                    |
         |                                    |
+--------v--------+                 +---------v---------+
| PostgreSQL DB   |                 | Redis Cache       |
| URL & User Data |                 | Cached Redirects  |
+-----------------+                 +-------------------+
```

---

## 📂 Project Structure

```
src
├── controller
├── service
├── repository
├── model
├── dto
├── security
├── config
├── exception
└── resources
```

---

## 🔐 Authentication Flow

1. User registers an account.
2. User logs in using email and password.
3. JWT token is generated upon successful login.
4. Client stores the JWT.
5. JWT is sent in the Authorization header for protected APIs.
6. Spring Security validates the token before processing requests.

---

## 📦 REST API Endpoints

### Authentication APIs

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/auth/register` | Register a new user |
| POST | `/auth/login` | Login and receive JWT |

### URL APIs

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/shorten` | Create a shortened URL |
| GET | `/api/urls` | Retrieve all URLs of the logged-in user |
| DELETE | `/api/urls/{shortCode}` | Delete a shortened URL |
| GET | `/{shortCode}` | Redirect to original URL |

---

## ⚡ Redis Caching

Redis is used to cache frequently accessed URLs, significantly reducing database lookups and improving redirect performance.

---

## 🚦 Rate Limiting

Bucket4j is implemented to protect the application from abuse by limiting the number of requests a client can make within a specified time window.

---

## 🐳 Running the Application with Docker

### Clone the Repository

```bash
git clone https://github.com/MADHURI-HS/url-shortener.git

cd url-shortener
```

### Start the Application

```bash
docker compose up --build
```

Application:

```
http://localhost:8080
```

---

## ⚙️ Running Without Docker

### Start PostgreSQL

### Start Redis

### Configure Environment Variables

```properties
SPRING_DATASOURCE_URL=
SPRING_DATASOURCE_USERNAME=
SPRING_DATASOURCE_PASSWORD=

REDIS_HOST=
REDIS_PORT=

JWT_SECRET=
```

Run:

```bash
./mvnw spring-boot:run
```

---

## ☁️ Deployment

The application is deployed on **Render** using:

- Docker
- Render Web Service
- Render PostgreSQL
- Render Key Value (Redis)

Live URL:

https://url-shortener-app-n9px.onrender.com

---

## 📖 API Documentation

Swagger UI

Local

```
http://localhost:8080/swagger-ui/index.html
```

Production

```
https://url-shortener-app-n9px.onrender.com/swagger-ui/index.html
```

---


## 🚀 Future Enhancements

- Custom Short URLs
- QR Code Generation
- URL Click Analytics
- URL Expiration
- Password Protected URLs
- Email Verification
- Admin Dashboard
- User Profile Management

---

## 👨‍💻 Skills Demonstrated

- REST API Development
- Authentication & Authorization
- Spring Security
- JWT
- Spring Data JPA
- PostgreSQL
- Redis Caching
- Rate Limiting
- Docker
- Cloud Deployment
- API Documentation
- Production Configuration

---

## 👩‍💻 Author

**Madhuri H S**

GitHub:
https://github.com/MADHURI-HS

LinkedIn:
https://www.linkedin.com/in/madhuri-h-s/

---

⭐ If you found this project helpful, consider giving it a star!
