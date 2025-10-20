# MinURL

Java 21 URL shortener backed by Postgres with deterministic SHA-256 based short codes and Liquibase migrations.

## Prerequisites

- Java 21 and Maven 3.6.3+
- Docker Desktop (for the bundled Postgres service)
- `pg_dump` / `psql` (bundled with Postgres client tools) for operational scripts

## Quick start

1. Start the database: `docker compose up -d postgres`
2. Build the application: `mvn clean package`
3. Run locally (Linux/macOS): `APP_ENV=local mvn exec:java`  
   Run locally (PowerShell): `$Env:APP_ENV = 'local'; mvn exec:java`
4. Shorten a URL: `curl -X POST http://localhost:7000/api/shorten -H "Content-Type: application/json" -d '{"url":"https://example.com"}'`

Liquibase migrations run automatically on startup. When `APP_ENV=local` the development seed (`https://example.com/`) is inserted.

## Configuration

All configuration is supplied via environment variables or an optional `.env` file.

| Variable      | Default                                   | Description                                                          |
| ------------- | ----------------------------------------- | -------------------------------------------------------------------- |
| `APP_ENV`     | `local`                                   | `local` enables seed data; set to `prod` for production deployments. |
| `BASE_URL`    | `http://localhost:7000`                   | External origin used when constructing short URLs.                   |
| `PORT`        | `7000`                                    | HTTP listen port.                                                    |
| `DB_URL`      | `jdbc:postgresql://localhost:5432/minurl` | JDBC URL for Postgres.                                               |
| `DB_USER`     | `minurl`                                  | Database username.                                                   |
| `DB_PASSWORD` | `minurl`                                  | Database password.                                                   |
| `LOG_DIR`     | `logs`                                    | Directory for request logs (see below).                              |

Set `LIQUIBASE_CONTEXTS=local` when running Liquibase commands manually to include the local seed.

## Database management

- **Apply migrations manually:** `mvn liquibase:update`
- **Rollback last change set:** `mvn liquibase:rollback -Dliquibase.rollbackCount=1`
- **Generate SQL without executing:** `mvn liquibase:updateSQL`

All commands honour the `DB_URL`, `DB_USER`, `DB_PASSWORD`, and optional `LIQUIBASE_CONTEXTS` environment variables.

## Request logging

Request/response metadata is written to `logs/requests.log` with daily rotation and a seven day retention window. Override the folder via `LOG_DIR`. Ensure the directory exists or allow Logback to create it on first write.

## Database dumps

Daily logical dumps with seven day retention can be scheduled using the helper scripts:

- Bash: `scripts/dump-db.sh [output_dir]`
- PowerShell: `.\scripts\dump-db.ps1 -OutputDir backups`

Both scripts honour standard `PG*` environment variables and default to the local Docker credentials. Add the script to a cron job or Windows Task Scheduler to meet the retention objective.

## API surface

- `POST /api/shorten` → `{"shortUrl": "<BASE_URL>/<code>"}`  
  Accepts JSON payload `{"url":"..."}`. URLs are normalised (trim, lower-case host, remove tracking params, drop default ports, enforce HTTPS) before hashing. Codes are the Base62 representation of the SHA-256 digest with a minimum length of five characters and deterministic collision extension.
- `GET /{code}` → 302 redirect to the stored canonical URL.
- `GET /healthz` → Liveness probe.

## Operational notes

- Concurrency target is 1–5 requests; connections are opened on demand without an application-level pool per the requirement.
- Request logs and the provided dump scripts meet the 7-day retention objectives; monitor disk usage if traffic or dump sizes increase.
- For production, inject strong credentials and back up the `postgres-data` volume in addition to the logical dumps.

## TODO

- Add auth
- Add frontend
