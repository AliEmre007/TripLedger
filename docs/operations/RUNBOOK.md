# TripLedger Runbook

## Local Startup

```bash
cp .env.example .env
docker compose up --build
```

Required local tools:

- Docker Engine or Docker Desktop with Compose v2.
- Git.
- `curl` for smoke checks.

Local environment variables:

| Variable | Required | Default or example | Notes |
|---|---|---|---|
| `APP_PORT` | yes | `18080` | Host port for the application container. |
| `POSTGRES_DB` | yes | `tripledger` | Local database name. |
| `POSTGRES_USER` | yes | `tripledger` | Local database user. |
| `POSTGRES_PASSWORD` | yes | `replace-with-local-dev-password` | Local-only password. Do not reuse in shared environments. |
| `TRIPLEDGER_ALLOWED_ORIGINS` | yes | `http://localhost:3000,http://localhost:8080,http://localhost:18080` | Local CORS origins. |
| `TRIPLEDGER_LOG_LEVEL` | yes | `INFO` | Raise only for short investigations. |

Do not commit `.env`. `.env.example` must contain placeholders only.

Health checks:

```bash
curl -i http://localhost:18080/api/v1/health
curl -i http://localhost:18080/api/v1/health/live
curl -i http://localhost:18080/api/v1/health/ready
curl -i http://localhost:18080/actuator/health
curl -i http://localhost:18080/actuator/health/liveness
curl -i http://localhost:18080/actuator/health/readiness
```

Metrics:

```bash
curl -i http://localhost:18080/actuator/metrics
curl -i http://localhost:18080/actuator/metrics/tripledger.http.requests
curl -i http://localhost:18080/actuator/metrics/tripledger.http.errors
curl -i http://localhost:18080/actuator/metrics/tripledger.job.retries
```

Deployment smoke check:

```bash
make smoke
```

Expected result:

```text
Smoke checks passed on port 18080
```

The smoke check verifies:

- `GET /api/v1/health/live`;
- `GET /api/v1/health/ready`;
- `GET /actuator/health/liveness`;
- `GET /actuator/health/readiness`.

Logs:

```bash
docker compose logs -f app
```

Stop:

```bash
docker compose down
```

Reset local database:

```bash
docker compose down -v
```

## Deployment Procedure

Validation-release local deployment:

1. Confirm the worktree is clean: `git status --short`.
2. Run `make verify`.
3. Copy safe environment defaults: `cp .env.example .env`.
4. Replace local-only placeholders in `.env`.
5. Start the stack: `docker compose up --build`.
6. In another terminal, run `make smoke`.
7. Record commit sha, smoke result, timestamp, and any known limitations.

Production-like pilot deployment must additionally provide:

- external secret management for database credentials and OIDC configuration;
- HTTPS termination;
- managed PostgreSQL or an equivalent backed-up database;
- log and metrics collection;
- backup and restore evidence before real pilot data.

## Rollback

Validation-release local rollback:

1. Stop the stack: `docker compose down`.
2. Check out the previous known-good commit.
3. Rebuild and start: `docker compose up --build`.
4. Run `make smoke`.

Database rollback rule:

- Do not manually edit Flyway history.
- Do not reset or restore real data without an approved backup/restore plan.
- If a migration has reached a shared or pilot environment, fix forward with a new migration unless an approved restore procedure is being executed.

## Common Failure Checks

### App cannot connect to database

1. Run `docker compose ps`.
2. Check `db` health.
3. Check `.env` values.
4. Run `docker compose logs db`.

### Port already in use

Change `APP_PORT` in `.env`. Add a local Compose override if direct host access to PostgreSQL is required.

### Migration failure

1. Check app logs.
2. Confirm migration files under `src/main/resources/db/migration`.
3. For local-only data, reset with `docker compose down -v`.
4. Never reset production data without an approved recovery plan.

### Smoke check fails

1. Run `docker compose ps`.
2. Run `docker compose logs app`.
3. Run `curl -i http://localhost:${APP_PORT:-18080}/api/v1/health/ready`.
4. Investigate the failed readiness check before retrying deployment.
5. If the new build cannot become ready, roll back to the previous known-good commit.

### Readiness is down

1. Run `curl -i http://localhost:18080/api/v1/health/ready`.
2. Check the failed `checks[].name`.
3. For `database`, inspect `docker compose ps` and `docker compose logs db`.
4. For `migrations`, inspect application startup logs and migration files.
5. For `background_jobs`, confirm the database is reachable and the `background_job` table exists.

### Failed job investigation

1. Capture the job id and `X-Correlation-Id`.
2. Run `curl -i http://localhost:18080/api/v1/jobs/{jobId}` with the normal local actor headers.
3. Check `status`, `attemptCount`, `diagnosticCategory`, and `correlationId`.
4. Search application logs for the correlation id.
5. Do not modify job or financial records directly in the database.

### Error investigation

1. Capture the response `error.code` and `error.correlationId`.
2. Search application logs for the correlation id.
3. Check `tripledger.http.errors` metrics by code and route.
4. Confirm the API response and logs do not include source payloads, secrets, or restricted personal data.

## Backup and Restore

Stage 6 has local runtime only. Pilot backup/restore must later satisfy `DEPLOYMENT_DIAGRAM.md` and `DEFINITION_OF_DONE.md`.

Local database dump example:

```bash
docker compose exec db pg_dump -U tripledger tripledger > /tmp/tripledger-local.sql
```

Local restore example:

```bash
docker compose exec -T db psql -U tripledger tripledger < /tmp/tripledger-local.sql
```

## Incident Notes

For any user-visible error, capture:

- endpoint;
- timestamp;
- `X-Correlation-Id`;
- user role;
- organisation id if safe to record;
- observed error code.

Do not paste secrets, tokens, source payloads, or restricted personal data into tickets.
