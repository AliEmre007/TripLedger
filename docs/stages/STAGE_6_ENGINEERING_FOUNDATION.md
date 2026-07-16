# Stage 6 - Engineering Foundation

**Product:** TripLedger  
**Owner:** Ali Emre GÜŞLÜ  
**Date:** 13 July 2026  
**Version:** 0.1  
**Status:** Engineering foundation initialized

## 1. Stage Purpose

Stage 6 creates repeatable ways to build, test, run, deploy, and understand the project before feature volume grows.

## 2. Evidence Created

| Output | File or location |
|---|---|
| Onboarding README | `README.md` |
| Agent instructions | `AGENTS.md` |
| Docker local runtime | `docker-compose.yml`, `Dockerfile` |
| Environment template | `.env.example` |
| Migration setup | `src/main/resources/db/migration/V1__engineering_foundation.sql` |
| Test setup | `pom.xml`, `src/test/java/...` |
| CI workflow | `.github/workflows/ci.yml` |
| Logging and health endpoints | `CorrelationIdFilter`, `HealthController`, Actuator config |
| Secret handling guide | `SECURITY.md` |
| Test strategy | `TEST_STRATEGY.md` |
| Runbook | `RUNBOOK.md` |
| Backup/restore rehearsal procedure | `BACKUP_RESTORE_REHEARSAL.md`, `scripts/backup-local.sh`, `scripts/restore-local.sh` |
| Branch and PR workflow | `docs/CONTRIBUTING.md` |

## 3. Technology Foundation

- Java 21.
- Spring Boot 3.
- PostgreSQL 16 for local runtime.
- Flyway for migrations.
- Maven for build/test.
- Docker Compose for reproducible local environment.
- Spring Actuator for health and metrics.
- Checkstyle for initial linting.
- GitHub Actions for CI.
- Local PostgreSQL backup and restore rehearsal scripts.

## 4. Implemented Endpoints

- `GET /api/v1/health`
- `GET /api/v1/health/live`
- `GET /api/v1/health/ready`
- `GET /actuator/health`
- `GET /actuator/health/liveness`
- `GET /actuator/health/readiness`
- `GET /actuator/metrics`

Every response receives `X-Correlation-Id`. Application logs include the correlation id in MDC.

## 5. Exit Gate Assessment

The foundation is ready when a contributor can:

1. clone the repository;
2. copy `.env.example` to `.env`;
3. run tests with `make test`;
4. run the system with `make run`;
5. run deployment smoke checks with `make smoke`;
6. inspect health and metrics endpoints;
7. understand configuration and secret handling;
8. follow branch, pull request, and review workflow.

This stage does not implement TripLedger feature workflows yet. It establishes the repeatable base for the validation-release backlog.
