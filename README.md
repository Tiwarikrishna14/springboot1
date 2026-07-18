# order-approval-backend

Production-ready Spring Boot starter backend for a multi-tenant B2B order and inventory management application.

## Prerequisites

- Java 21
- Maven 3.9+
- Docker and Docker Compose
- PostgreSQL 16+ for local non-test runs

Automated integration tests use Testcontainers PostgreSQL. The project does not use H2.

## Environment Variables

Copy `.env.example` to `.env` for Docker Compose or export the same variables locally.

Required production variables:

```bash
DB_URL=jdbc:postgresql://host:5432/order_approval
DB_USERNAME=order_approval
DB_PASSWORD=change-me
JWT_SECRET=replace-with-random-secret-at-least-32-bytes
JWT_ACCESS_EXPIRATION=900
JWT_REFRESH_EXPIRATION=604800
ALLOWED_ORIGINS=https://app.example.com
INITIAL_ADMIN_EMAIL=superadmin@example.com
INITIAL_ADMIN_PASSWORD=ChangeMe@12345
```

## Local PostgreSQL Setup

Start only PostgreSQL:

```bash
docker compose up -d postgres
```

Then run the app locally:

```bash
./mvnw spring-boot:run
```

On Windows PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

## Docker Startup

Build and run the backend plus PostgreSQL:

```bash
docker compose up --build
```

The API is available at `http://localhost:8080`.

## Database Migration

Flyway runs automatically at startup. Migrations live in `src/main/resources/db/migration`.

The migration set creates organizations, users, roles, permissions, refresh tokens, password reset tokens, audit logs, default permissions, default roles, and an environment-driven system super-admin.

## Default Development Login

The `dev` profile creates:

- Organization: `DEMO`
- Email: `admin@example.com`
- Password: `Password@123`
- Role: `ORGANIZATION_ADMIN`

The system super-admin is created from:

- `INITIAL_ADMIN_EMAIL`
- `INITIAL_ADMIN_PASSWORD`

## Swagger

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

Click **Authorize** in Swagger UI and enter `Bearer <accessToken>`.

## Running Tests

```bash
./mvnw test
```

Tests require Docker because integration tests use Testcontainers PostgreSQL.

## Example API Calls

Login:

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@example.com","password":"Password@123"}'
```

Set tokens:

```bash
export ACCESS_TOKEN="paste-access-token"
export REFRESH_TOKEN="paste-refresh-token"
```

Current user:

```bash
curl http://localhost:8080/api/v1/auth/me \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

Create user:

```bash
curl -X POST http://localhost:8080/api/v1/users \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "firstName":"Aman",
    "lastName":"Manager",
    "email":"aman.manager@example.com",
    "password":"Password@123"
  }'
```

List users:

```bash
curl "http://localhost:8080/api/v1/users?page=0&size=20&sort=createdAt,desc&search=aman" \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

Create role:

```bash
curl -X POST http://localhost:8080/api/v1/roles \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"BUYER","description":"Buyer role","systemRole":false}'
```

Assign role:

```bash
curl -X POST http://localhost:8080/api/v1/users/{userId}/roles \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"roleIds":["role-uuid"]}'
```

Refresh token:

```bash
curl -X POST http://localhost:8080/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d "{\"refreshToken\":\"$REFRESH_TOKEN\"}"
```

Logout:

```bash
curl -X POST http://localhost:8080/api/v1/auth/logout \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"refreshToken\":\"$REFRESH_TOKEN\"}"
```

## Protected APIs

All endpoints except login, registration, refresh, forgot/reset password, Swagger, OpenAPI JSON, and health checks require a JWT bearer token.

The security model includes:

- Role authorities as `ROLE_<ROLE_NAME>`
- Permission authorities as raw permission codes, for example `USER_VIEW`
- Method-level authorization with `@PreAuthorize`
- Organization isolation in service methods

## Refresh Tokens

Refresh tokens are opaque random values. Only SHA-256 hashes are stored in PostgreSQL.

Refresh token use rotates the token:

1. Existing refresh token is validated.
2. Existing refresh token is revoked.
3. New access token is issued.
4. New refresh token is generated and stored as a hash.

## Production Configuration

Use `SPRING_PROFILES_ACTIVE=prod` and provide all required environment variables. Do not use development secrets or default passwords in production.

For Supabase PostgreSQL later, keep the same JDBC properties and replace only `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`.

## Supabase PostgreSQL

Prefer separate username and password variables instead of embedding credentials in the JDBC URL:

```bash
DB_URL=jdbc:postgresql://db.<project-ref>.supabase.co:5432/postgres?sslmode=require
DB_USERNAME=postgres
DB_PASSWORD=<your-supabase-database-password>
```

If the host running the backend cannot reach Supabase over IPv6, use the Supabase session pooler:

```bash
DB_URL=jdbc:postgresql://aws-<region>.pooler.supabase.com:5432/postgres?sslmode=require
DB_USERNAME=postgres.<project-ref>
DB_PASSWORD=<your-supabase-database-password>
```

Run locally against Supabase:

```bash
./mvnw spring-boot:run
```

Run with Docker against Supabase without starting local PostgreSQL:

```bash
docker compose -f docker-compose.supabase.yml up --build
```
