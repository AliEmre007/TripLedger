# TripLedger

TripLedger is a provider-neutral financial control layer for multi-supplier tourism operators. It imports booking, payment, settlement, refund, and supplier records, calculates booking-level financial expectations, matches deterministic records, and exposes discrepancies with audit evidence.

The current repository is in **Stage 6 - Engineering Foundation**. It contains the planning/design documents plus a minimal Java/Spring Boot foundation that can be built, tested, run, and extended consistently.

## Prerequisites

- Docker Desktop or Docker Engine with Compose v2.
- Java 21 if you want to run the application outside Docker.
- Git.

Maven does not need to be installed locally when using the Docker commands below.

## Quick Start

```bash
cp .env.example .env
docker compose run --rm test mvn -B test
docker compose up --build
```

Open:

- application health: http://localhost:18080/api/v1/health
- actuator health: http://localhost:18080/actuator/health
- actuator metrics: http://localhost:18080/actuator/metrics

Stop services:

```bash
docker compose down
```

Remove local database volume:

```bash
docker compose down -v
```

## Common Commands

```bash
make copy-env   # create .env from template
make test       # run unit tests in Maven container
make verify     # run tests and checkstyle in Maven container
make run        # build and run app + PostgreSQL
make stop       # stop services
make logs       # follow app logs
make clean      # stop services and delete volumes
```

## Project Structure

```text
src/main/java/com/tripledger      Spring Boot application code
src/main/resources/db/migration   Flyway migrations
src/test/java/com/tripledger      Automated tests
config/checkstyle                 Java style checks
.github/workflows                 CI workflow
ADRs                              Architecture decision records
docs                              Supporting engineering docs
```

## Configuration

Local configuration starts from `.env.example`. Copy it to `.env` and change local-only values as needed.

Secrets must not be committed. Production secrets must come from the deployment environment or secret manager, not from `.env` files or source code.

Main variables:

| Variable | Purpose |
|---|---|
| `APP_PORT` | Local HTTP port |
| `POSTGRES_DB` | Local database name |
| `POSTGRES_USER` | Local database user |
| `POSTGRES_PASSWORD` | Local database password |
| `TRIPLEDGER_ALLOWED_ORIGINS` | Local CORS origin list for future UI work |
| `TRIPLEDGER_LOG_LEVEL` | Application log level |

## Engineering Foundation

Included now:

- Spring Boot 3 / Java 21 application skeleton.
- PostgreSQL local runtime through Docker Compose.
- Flyway migration setup.
- Maven build and test setup.
- Checkstyle linting.
- GitHub Actions CI.
- Standard JSON error response model.
- Correlation id response header and log MDC.
- Health endpoint at `/api/v1/health`.
- Actuator health and metrics endpoints.
- Initial documentation for contributors, agents, security, tests, and operations.

## Documentation Map

| Document | Purpose |
|---|---|
| `PROJECT_CONTEXT.md` | Problem, users, scope, rules, constraints, terminology |
| `ARCHITECTURE.md` | Modules, boundaries, data flow, trade-offs |
| `DOMAIN_MODEL.md` | Domain entities and ownership |
| `DATABASE_DESIGN.md` | ERD, constraints, migration policy |
| `API_DESIGN.md` | API conventions, auth, errors, contract references |
| `SECURITY.md` | Authentication, authorization, secrets, data classification |
| `TEST_STRATEGY.md` | Test layers, commands, fixture approach |
| `RUNBOOK.md` | Local run, health checks, backup/restore, incident steps |
| `AGENTS.md` | Rules for AI/coding agents working in this repo |
| `ADRs/` | Architecture decision records |

## Contribution Workflow

1. Create a short-lived branch from `main`.
2. Keep changes small and mapped to `VALIDATION_RELEASE_BACKLOG.md`.
3. Run `make verify` before opening a pull request.
4. Update docs when behavior, API, schema, or operations change.
5. Do not merge changes that weaken tenant isolation, auditability, exact money, idempotency, or deterministic matching.

See `docs/CONTRIBUTING.md` for the full branch, pull request, and review workflow.
