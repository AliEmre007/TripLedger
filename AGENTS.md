# AGENTS.md

Repository instructions for AI agents and coding assistants.

## Mission

TripLedger is a financial-control application for tourism booking reconciliation. Correctness, auditability, tenant isolation, deterministic behavior, and explainability are more important than feature volume.

## Required Reading

Before feature work, read:

1. [Project context](docs/project/PROJECT_CONTEXT.md)
2. [Architecture](docs/architecture/ARCHITECTURE.md)
3. [Domain model](docs/architecture/DOMAIN_MODEL.md)
4. [Database design](docs/architecture/DATABASE_DESIGN.md)
5. [API design](docs/architecture/API_DESIGN.md)
6. [Security](docs/operations/SECURITY.md)
7. [Test strategy](docs/operations/TEST_STRATEGY.md)
8. [Validation release backlog](docs/delivery/VALIDATION_RELEASE_BACKLOG.md)
9. [Definition of Done](docs/delivery/DEFINITION_OF_DONE.md)

For architectural changes, also read the relevant ADRs in [docs/architecture/ADRs](docs/architecture/ADRs/).

## Repository Structure

- `src/main/java/com/tripledger` contains application code.
- `src/main/resources/application.yml` contains Spring configuration defaults.
- `src/main/resources/db/migration` contains Flyway migrations.
- `src/test/java/com/tripledger` contains automated tests.
- `config/checkstyle` contains style rules.
- `docs/project` contains problem, scope, users, requirements, and business rules.
- `docs/architecture` contains system design, API/database design, security design, deployment design, and ADRs.
- `docs/delivery` contains roadmap, backlog, milestone plan, and ready/done standards.
- `docs/risk` contains feasibility, risks, constraints, threats, and trade-offs.
- `docs/operations` contains runbook, security guide, and test strategy.
- `docs/stages` contains stage evidence documents.

## Local Commands

Preferred verification:

```bash
make verify
```

Equivalent verification:

```bash
docker compose run --rm test mvn -B verify
```

Run the application:

```bash
cp .env.example .env
docker compose up --build
```

Health checks:

```bash
curl -i http://localhost:18080/api/v1/health
curl -i http://localhost:18080/actuator/health
```

Stop the local stack:

```bash
docker compose down
```

## Architecture Constraints

- Keep the validation release as a modular monolith.
- Use PostgreSQL as the system of record.
- Use Flyway for every schema change.
- Use relational constraints and migrations for persistent invariants.
- Every business record must be organisation-scoped.
- Financial values require exact decimal amount plus currency.
- Accepted financial events are immutable.
- Corrections use reversal/replacement or controlled adjustment.
- Automatic matching must be deterministic and unique.
- Missing required financial data is `UNKNOWN` or `NOT_READY`, not zero.
- Audit material financial and security actions.
- Keep external integrations provider-neutral until real pilot evidence justifies a specific connector.

## ADR Rules

Create or update an ADR before changing decisions about:

- architecture style;
- database technology;
- authentication or authorization model;
- tenancy model;
- money representation;
- reconciliation or matching strategy;
- audit strategy;
- deployment model;
- external integration approach.

Do not introduce major technology because it is fashionable. Document the trade-off, rejected alternatives, and consequences.

## Prohibited Changes Without ADR

- Introducing microservices, Kafka, Kubernetes, or event sourcing as a foundation.
- Adding production OTA, bank, payment, or accounting connectors.
- Storing raw card data, passport scans, medical records, or unrestricted bank credentials.
- Replacing deterministic matching with probabilistic or AI-controlled matching.
- Removing organisation scoping, audit evidence, exact money handling, idempotency, or immutability.
- Adding secrets to source control.

## Implementation Rules

- Link work to [validation release backlog](docs/delivery/VALIDATION_RELEASE_BACKLOG.md).
- Keep changes vertical and small.
- Prefer existing Spring Boot, Maven, Flyway, and Docker Compose patterns already in this repo.
- Do not create broad placeholder packages that are not tied to a planned slice.
- Do not bypass application services for domain writes.
- Keep controllers thin; place business behavior behind application/domain services.
- Keep domain rules explicit and testable.
- Return stable API errors using the standard error response shape.
- Preserve `X-Correlation-Id` behavior for request tracing.
- Update documentation in the same change when contracts, schema, operations, or security behavior change.

## Database Rules

- Add schema changes as new Flyway migration files. Do not edit already-applied migrations.
- Use database constraints for ownership, uniqueness, required fields, and financial invariants where practical.
- Include organisation ownership in business tables.
- Avoid nullable fields unless the unknown/not-ready state is intentional and documented.
- Never store raw card data, unrestricted banking credentials, passport scans, or medical data.

## API Rules

- Keep API routes under `/api/v1`.
- Use JSON request and response bodies.
- Use stable error codes for business-rule failures.
- Do not leak stack traces, SQL details, secrets, or sensitive data in API responses.
- Document new or changed contracts in [API design](docs/architecture/API_DESIGN.md) or [API specification](docs/architecture/API_SPECIFICATION.md).

## Testing Rules

- Add tests with behavior changes.
- For controllers, test status codes, response body, error shape, and correlation id behavior.
- For domain rules, test accepted cases, rejected cases, and boundary cases.
- For database changes, test migration validity and important constraints.
- For security-sensitive changes, test unauthorized, wrong-organisation, and permitted access paths.
- Before completion, run `make verify` or explain why it could not be run.

## Git and Review Rules

- Work on short-lived branches after the foundation commit.
- Keep commits focused and reviewable.
- Do not commit generated build output, local `.env`, logs, IDE files, or zip input artifacts.
- Do not rewrite published history unless explicitly instructed.
- Pull request descriptions should mention the backlog item, tests run, schema changes, API changes, and documentation changes.

## Done Standard

A change is not done until [Definition of Done](docs/delivery/DEFINITION_OF_DONE.md) is satisfied for the affected slice.

Before final response or handoff, confirm:

- code compiles;
- relevant tests pass;
- migrations are present for schema changes;
- docs are updated for changed behavior;
- no secrets or generated artifacts are staged;
- trade-offs are documented when design decisions changed.
