# TripLedger Test Strategy

## Purpose

Tests must prove financial correctness, tenant isolation, authorization, idempotency, auditability, and operational behavior. Happy-path tests are not enough.

## Current Test Command

```bash
make test
make verify
```

Equivalent:

```bash
docker compose run --rm test mvn -B test
docker compose run --rm test mvn -B verify
```

## Test Layers

| Layer | Purpose | Examples |
|---|---|---|
| Unit | Pure business rules and value objects | money precision, status transitions, match candidate rules |
| Web/API | Contract and error behavior | health endpoint, correlation id, validation errors |
| Persistence | Migrations, constraints, repositories | source uniqueness, organisation-scoped references |
| Integration | Module workflows | import batch to booking, financial event to reconciliation |
| Acceptance | User-visible workflows | scenarios from `ACCEPTANCE_CRITERIA.md` |
| Operational | Run, health, metrics, backup/restore | `RUNBOOK.md` checks |

## Required Future Coverage

- Cross-organisation access returns no protected data.
- Operations cannot perform finance-only actions.
- Re-import creates no duplicate financial effect.
- Invalid currency precision is rejected.
- Accepted financial events cannot be edited.
- Missing required data produces `UNKNOWN` or `NOT_READY`.
- Deterministic matching refuses ambiguity.
- Reconciliation rerun is repeatable.
- Material discrepancy is created and deduplicated.
- Audit event exists for material financial actions.

## Test Data

- Use synthetic fixtures by default.
- Do not commit real customer, passport, card, bank, medical, or confidential business data.
- Acceptance fixture names should describe the rule being tested.

## Done Rule

A feature is not done until its relevant tests pass and the behavior maps to `DEFINITION_OF_DONE.md`.
