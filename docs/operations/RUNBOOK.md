# TripLedger Runbook

## Local Startup

```bash
cp .env.example .env
docker compose up --build
```

Health checks:

```bash
curl -i http://localhost:18080/api/v1/health
curl -i http://localhost:18080/actuator/health
```

Metrics:

```bash
curl -i http://localhost:18080/actuator/metrics
```

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
