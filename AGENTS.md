# AGENTS.md

Repository instructions for AI agents and coding assistants.

## Mission

TripLedger is a financial-control application. Correctness, auditability, tenant isolation, deterministic behavior, and explainability are more important than feature volume.

## Required Reading Before Feature Work

Read these first:

1. [Project context](docs/project/PROJECT_CONTEXT.md)
2. [Architecture](docs/architecture/ARCHITECTURE.md)
3. [Domain model](docs/architecture/DOMAIN_MODEL.md)
4. [Database design](docs/architecture/DATABASE_DESIGN.md)
5. [API design](docs/architecture/API_DESIGN.md)
6. [Security](docs/operations/SECURITY.md)
7. [Test strategy](docs/operations/TEST_STRATEGY.md)
8. [Validation release backlog](docs/delivery/VALIDATION_RELEASE_BACKLOG.md)

For architectural changes, also read the relevant ADRs in [docs/architecture/ADRs](docs/architecture/ADRs/).

## Validation Commands

Preferred local validation:

```bash
make verify
```

Equivalent command:

```bash
docker compose run --rm test mvn -B verify
```

Run the app:

```bash
cp .env.example .env
docker compose up --build
```

## Architecture Constraints

- Keep the validation release as a modular monolith.
- Use relational constraints and migrations for persistent data.
- Every business record must be organisation-scoped.
- Financial values require exact decimal amount plus currency.
- Accepted financial events are immutable.
- Corrections use reversal/replacement or controlled adjustment.
- Automatic matching must be deterministic and unique.
- Missing required financial data is `UNKNOWN` or `NOT_READY`, not zero.
- Audit material financial and security actions.

## Prohibited Changes Without ADR

- Introducing microservices, Kafka, Kubernetes, or event sourcing as a foundation.
- Adding production OTA, bank, payment, or accounting connectors.
- Storing raw card data, passport scans, medical records, or unrestricted bank credentials.
- Replacing deterministic matching with probabilistic or AI-controlled matching.
- Removing organisation scoping, audit evidence, exact money handling, or idempotency.
- Adding secrets to source control.

## Implementation Rules

- Link work to [validation release backlog](docs/delivery/VALIDATION_RELEASE_BACKLOG.md).
- Keep changes vertical and small.
- Update tests with behavior changes.
- Update documentation in the same change when contracts, schema, operations, or security behavior change.
- Do not create broad placeholder packages that are not tied to a planned slice.
- Do not bypass application services for domain writes.

## Done Standard

A change is not done until [Definition of Done](docs/delivery/DEFINITION_OF_DONE.md) is satisfied for the affected slice.
