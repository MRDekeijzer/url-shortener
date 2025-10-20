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
4. Shorten a URL: `curl -u admin:password -X POST http://localhost:7000/api/shorten -H "Content-Type: application/json" -d '{"url":"https://example.com"}'`

Liquibase migrations run automatically on startup. When `APP_ENV=local` the development seed (`https://example.com/`) is inserted.

## Configuration

All configuration is supplied via environment variables or an optional `.env` file.

| Variable           | Default                                   | Description                                                          |
| ------------------ | ----------------------------------------- | -------------------------------------------------------------------- |
| `APP_ENV`          | `local`                                   | `local` enables seed data; set to `prod` for production deployments. |
| `BASE_URL`         | `http://localhost:7000`                   | External origin used when constructing short URLs.                   |
| `PORT`             | `7000`                                    | HTTP listen port.                                                    |
| `DB_URL`           | `jdbc:postgresql://localhost:5432/minurl` | JDBC URL for Postgres.                                               |
| `DB_USER`          | `minurl`                                  | Database username.                                                   |
| `DB_PASSWORD`      | `minurl`                                  | Database password.                                                   |
| `LOG_DIR`          | `logs`                                    | Directory for request logs (see below).                              |
| `BASIC_AUTH_USERS` | _(required)_                              | Comma-separated `username:BCryptHash` pairs for HTTP Basic auth.     |

Set `LIQUIBASE_CONTEXTS=local` when running Liquibase commands manually to include the local seed.

## Authentication

`POST /api/shorten` is protected with HTTP Basic authentication. Provide one or more credentials via `BASIC_AUTH_USERS`, for example:

```
BASIC_AUTH_USERS="admin:$2a$10$7EqJtq98hPqEX7fNZaFWoO3Z7N/1ob7Hu0f1D/Gg7OfG4yJ/9F9Wy"
```

The example above corresponds to the password `password` and is offered for local use only. Generate unique hashes for each deployment with a BCrypt-capable tool such as `htpasswd -nBC 12 admin | cut -d: -f2`, or via JShell:

```
mvn -q dependency:build-classpath "-Dmdep.outputFile=target/classpath.txt"
jshell --class-path "$(Get-Content target/classpath.txt -Raw)"
jshell> import org.mindrot.jbcrypt.BCrypt;
jshell> System.out.println(BCrypt.hashpw("choose-a-strong-password", BCrypt.gensalt(12)));
```

Restart the service after updating credentials. The authenticated username is exposed to request handlers through the `authenticatedUser` context attribute.

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

- `POST /api/shorten` (requires Basic Auth) -> `{"shortUrl": "<BASE_URL>/<code>"}`  
  Accepts JSON payload `{"url":"..."}`. URLs are normalised (trim, lower-case host, remove tracking params, drop default ports, enforce HTTPS) before hashing. Codes are the Base62 representation of the SHA-256 digest with a minimum length of five characters and deterministic collision extension.
- `GET /{code}` -> 302 redirect to the stored canonical URL.
- `GET /api/health` -> Liveness probe.

## Operational notes

- Concurrency target is 1–5 requests; connections are opened on demand without an application-level pool per the requirement.
- Request logs and the provided dump scripts meet the 7-day retention objectives; monitor disk usage if traffic or dump sizes increase.
- For production, inject strong credentials and back up the `postgres-data` volume in addition to the logical dumps.

## TODO

- Add frontend
