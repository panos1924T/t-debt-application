# T-Debt Backend

REST API backend for the T-Debt application, built with Spring Boot.  
It provides authentication/authorization, user management, debt tracking, and transaction ledger operations with PostgreSQL persistence and Flyway migrations.

---

## What this backend does

- Registers and authenticates users with JWT
- Applies role/capability-based authorization (ADMIN / USER)
- Manages debts (create, update, list, archive/unarchive, delete)
- Manages debt transactions (create, update via correction record, list)
- Uses soft-delete for users and debts
- Returns structured validation and error responses
- Seeds an admin user from environment variables (optional)

---

## Implemented service layer (brief)

- **AuthenticationService**: validates credentials and issues JWT tokens
- **JwtService**: token generation and validation
- **CustomUserDetailsService**: loads user + role/capabilities for Spring Security
- **UserServiceImpl**: registration, update/delete, paginated retrieval with access control
- **DebtServiceImpl**: debt CRUD, ownership checks, status toggle, filtered pagination
- **TransactionServiceImpl**: transaction creation/update, balance recalculation, correction-chain logic
- **AdminSeeder**: creates initial admin user on startup (when env vars are provided)

---

## Tech stack

- **Language:** Java 21
- **Framework:** Spring Boot (Web MVC, Validation, Security, Data JPA)
- **Auth:** JWT (jjwt)
- **Database:** PostgreSQL
- **Migrations:** Flyway
- **Build tool:** Gradle (Kotlin DSL)
- **Utilities:** Lombok, dotenv-java
- **Testing:** JUnit + Spring test starters + JaCoCo

---

## Prerequisites

Install the following on your machine:

1. **Git**
2. **JDK 21**
3. **PostgreSQL** (for local non-docker run)
4. **Docker + Docker Compose** (for containerized run)
5. (Optional) **Postman/Insomnia** for API testing

> Gradle does **not** need to be installed globally; the project uses the Gradle Wrapper (`gradlew` / `gradlew.bat`).

---

## 1. Clone the project

```bash
git clone https://github.com/panos1924T/t-debt-application.git
cd t-debt-application
```

---

## 2. Configure environment variables

Create a `.env` file in the project root (or copy `.env.example`) and fill:

```env
DB_HOST=localhost
DB_PORT=5433
DB_NAME=your_db_name
DB_USERNAME=your_db_user
DB_PASSWORD=your_db_password

JWT_SECRET_KEY=your_base64_secret_key
JWT_EXPIRATION=86400000

ALLOWED_ORIGINS=http://localhost:3000,http://localhost:4200

ADMIN_EMAIL=admin@example.com
ADMIN_PASSWORD=Admin123!

# used by docker-compose
CURRENT_DB_SCHEMA=tdebtapp
SPRING_PROFILES_ACTIVE=default
```

### Notes

- `JWT_SECRET_KEY` must be a **Base64-encoded** secret.
- `JWT_EXPIRATION` is in **milliseconds** (example: `86400000` = 24h).
- `ADMIN_EMAIL` / `ADMIN_PASSWORD` are optional; if set, admin seeding runs at startup.
- App schema is `tdebtapp`.

---

## 3. Local database setup (non-docker flow)

Create the database first (schema and tables are handled by Flyway):

```sql
CREATE DATABASE your_db_name;
```

Ensure DB user permissions are sufficient for schema/table migration.

---

## 4. Build the backend (local)

### Windows
```bash
.\gradlew.bat clean build
```

### macOS/Linux
```bash
./gradlew clean build
```

This compiles code, runs tests, and creates the jar in `build/libs/`.

---

## 5. Run the backend (local)

### Windows
```bash
.\gradlew.bat bootRun
```

### macOS/Linux
```bash
./gradlew bootRun
```

Default base URL:
- `http://localhost:8080`

---

## Docker build & run

This project includes `Dockerfile` and `docker-compose.yml`.

### A) Build app jar first (required by Dockerfile)

### Windows
```bash
.\gradlew.bat clean build
```

### macOS/Linux
```bash
./gradlew clean build
```

### B) Build Docker image manually

```bash
docker build -t tdebt-backend:latest .
```

### C) Run full stack with Docker Compose (app + postgres)

```bash
docker compose up --build -d
```

Stop:
```bash
docker compose down
```

Stop and remove DB volume too:
```bash
docker compose down -v
```

### Docker ports

- **API:** `8080:8080`
- **PostgreSQL (container):** exposed as `5434` on host

---

## API overview

### Public
- `POST /api/v1/users`
- `POST /api/v1/auth`

### Users
- `PUT /api/v1/users/{uuid}`
- `DELETE /api/v1/users/{uuid}`
- `GET /api/v1/users/{uuid}?includeDeleted=false`
- `GET /api/v1/users?includeDeleted=false&page=0&size=10&sort=email,asc`

### Debts
- `POST /api/v1/debts`
- `PUT /api/v1/debts/{debtUuid}`
- `PATCH /api/v1/debts/{debtUuid}` (toggle OPEN/ARCHIVED)
- `DELETE /api/v1/debts/{debtUuid}`
- `GET /api/v1/debts/{debtUuid}`
- `GET /api/v1/debts?...filters...`

### Transactions
- `POST /api/v1/debts/{debtUuid}/transactions`
- `PUT /api/v1/debts/{debtUuid}/transactions/{transUuid}`
- `GET /api/v1/transactions/{transUuid}`
- `GET /api/v1/debts/{debtUuid}/transactions?...filters...`
- `GET /api/v1/transactions?page=0&size=10&sort=date,desc`

---

## Testing

### Run all tests

### Windows
```bash
.\gradlew.bat test
```

### macOS/Linux
```bash
./gradlew test
```

### Test classes currently implemented

- `TdebtApplicationTests` (context load)
- `DebtServiceImplTest`
- `TransactionServiceImplTest`
- `TransactionMapperTest`

### Generate coverage report (JaCoCo)

```bash
./gradlew test jacocoTestReport
```

Windows:
```bash
.\gradlew.bat test jacocoTestReport
```

Coverage report:
- `build/reports/jacoco/test/html/index.html`

---

## Logging

Runtime logs are written to:
- `logs/all.log`
- `logs/error.log`
- `logs/tomcat.log`
- `logs/hikari.log`
- `logs/sql.log`

---

## Important implementation notes

- Flyway runs automatically at startup.
- User and debt entities are soft-deleted.
- Transaction updates may create correction entries to preserve ledger integrity.
- Access control is enforced both at route level and service level (`@PreAuthorize`).

---

## Troubleshooting (short)

- **App fails on DB connect:** verify `DB_*` values and PostgreSQL availability.
- **Flyway errors on startup:** check DB user permissions and schema state.
- **401 Unauthorized:** token missing/invalid/expired.
- **403 Forbidden:** authenticated user lacks required capability.
- **Docker app fails on startup:** ensure `.env` includes all required values and the jar was built before `docker build`.
