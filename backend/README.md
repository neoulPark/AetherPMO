# AetherPMO Backend

Spring Boot backend for the AetherPMO PMS (vertical slice: Project & Task domains, mock auth, mock Amaranth adapter).

## Stack
- Java 21, Spring Boot 3.2.x, Maven
- Spring Web / Data JPA / Security / Validation
- PostgreSQL + Flyway
- Lombok

## Prerequisites
- JDK 21
- Maven 3.9+
- Docker (for Postgres) or a local PostgreSQL instance

## 1. Start PostgreSQL

```bash
docker run --name aetherpmo-postgres \
  -e POSTGRES_DB=aetherpmo \
  -e POSTGRES_USER=aetherpmo \
  -e POSTGRES_PASSWORD='aetherpmo1234!' \
  -p 5432:5432 \
  -d postgres:16
```

## 2. Run the app

```bash
cd backend
mvn spring-boot:run
```

Flyway runs `V1__core_tables.sql` and `V2__seed_data.sql` on startup. The server listens on `http://localhost:8080`.

## 3. Try it

```bash
# Login (any seeded username + password "password")
curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"ahnyk","password":"password"}'
# -> { "success": true, "data": { "token": "mock-token-1", "user": {...} } }

# Use the token
TOKEN="mock-token-1"
curl -s http://localhost:8080/api/v1/auth/me -H "Authorization: Bearer $TOKEN"
curl -s "http://localhost:8080/api/v1/projects?stage=EXECUTION" -H "Authorization: Bearer $TOKEN"
curl -s http://localhost:8080/api/v1/projects/1/tasks -H "Authorization: Bearer $TOKEN"
```

## Seeded users
All passwords are `password`.

| username | name   | role   |
|----------|--------|--------|
| ahnyk    | 안유경 | PM     |
| leeyh    | 이영희 | PM     |
| kimcs    | 김철수 | PM     |
| kimjh    | 김준현 | MEMBER |
| parkjm   | 박지민 | MEMBER |
| kangdw   | 강동우 | MEMBER |

## API (all under `/api/v1`, responses wrapped in `ApiResponse`)
- `POST /auth/login` -> `{ token, user }`
- `GET  /auth/me`
- `GET  /projects?stage=EXECUTION` / `POST /projects`
- `GET/PUT /projects/{id}` / `GET /projects/{id}/summary`
- `GET/POST /projects/{projectId}/tasks` (GET returns nested tree)
- `GET/PUT/DELETE /tasks/{id}`
- `PUT  /tasks/{id}/progress` (leaf only; recalculates parents)
- `POST /tasks/{id}/assign` (writes assignment history)
- `GET  /tasks/{id}/assignment-history`

## Auth model (mock)
- Login validates a known username with password `password` and returns `mock-token-{userId}`.
- `MockAuthFilter` reads `Authorization: Bearer mock-token-{id}`, loads the user, and sets the SecurityContext.
- All endpoints except `POST /api/v1/auth/login` require a valid token.

## Progress auto-calculation
Parent task progress = weighted average of children (weighted by `planned_effort` when every child has a positive effort, otherwise equal weight). Updating a leaf's progress recalculates all ancestors. Direct progress input on a parent task is rejected.

## Notes
- `ddl-auto=validate` — the schema is owned by Flyway, not Hibernate.
- The Amaranth adapter (`UserPort`, `FilePort`, `TokenValidator`) is implemented by in-memory/DB-backed mocks under `adapter/amaranth/mock`, ready to be swapped for the real ACL later.
```
