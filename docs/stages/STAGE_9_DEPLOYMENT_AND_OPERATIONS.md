# Stage 9 - Deployment and Operations

## Purpose

Operate TripLedger as a service with repeatable deployment, observable runtime behavior, recoverable data, and documented incident handling.

For the current project state, Stage 9 applies to the validation/demo release only. Real customer financial data remains blocked until the production-readiness gaps in Stage 8 and this document are closed or formally accepted.

## Release Boundary

- Release name: `v0.1.0-validation`
- Release type: validation/demo operations release
- Allowed data: synthetic or anonymised demo data only
- Real customer data: not approved
- Deployment mode: Docker Compose validation deployment
- Database: PostgreSQL
- Operational proof target: deploy, smoke test, observe, back up, restore, and record incidents consistently

## Operational Readiness Checklist

| Area | Validation-release status | Evidence |
| --- | --- | --- |
| Release candidate | Ready | Stage 8 evidence records a tested candidate and known risks. |
| Deployment procedure | Ready for validation | `docs/operations/RUNBOOK.md` documents local deployment, smoke checks, rollback, backup, and restore. |
| Deployment record | Ready | Use `docs/operations/DEPLOYMENT_RECORD.md` for every validation deployment. |
| Health checks | Ready | `make smoke` checks app and Actuator liveness/readiness endpoints. |
| Logs | Ready for validation | Request logs include method, route, status, outcome, stable error code, duration, and correlation id. |
| Metrics | Ready for validation | Actuator metrics are exposed; expected dashboard and alert signals are documented in `docs/operations/OBSERVABILITY_OPERATIONS.md`. |
| Traces | Deferred | Distributed tracing is not required for the modular monolith validation release. Add when external integrations or multi-service calls exist. |
| Backups | Ready for local validation | `make backup-local` creates a PostgreSQL dump and manifest. |
| Restore rehearsal | Ready for local validation | `make restore-local` validates checksum, restores, compares critical counts, and writes restore evidence. |
| Incident handling | Ready for validation | Use `docs/operations/INCIDENT_RECORD_TEMPLATE.md`. |
| Access management | Limited | Local/header-backed actor adapter is acceptable only for validation/demo data. Production OIDC is required before real data. |
| Certificates and HTTPS | Environment responsibility | Required before any production-like internet exposure. |
| Secret management | Limited | Local `.env` is acceptable only for local/demo. Production-like deployments require external secret management. |
| Dependency and image scanning | Gap | Required before real pilot or public production release. |

## Deployment Procedure

For a validation deployment:

1. Confirm the release tag or commit.
2. Confirm no real customer data is present.
3. Configure environment values from `.env.example` without committing secrets.
4. Run `make verify`.
5. Start the stack with `docker compose up --build`.
6. Run `make smoke`.
7. Create a backup with `make backup-local`.
8. Rehearse restore in a safe environment before treating the backup as proven.
9. Record the deployment using `docs/operations/DEPLOYMENT_RECORD.md`.
10. Monitor logs, metrics, health, and incident records during the validation window.

## Operational Signals

Logs answer: what happened?

- request route, method, status, outcome, duration;
- stable error code;
- correlation id;
- safe actor and organisation context where applicable;
- no secrets, raw source rows, tokens, or restricted data.

Metrics answer: how often or how much?

- request count;
- error count by route and stable code;
- request latency;
- readiness state;
- job retry counts;
- database availability through readiness.

Traces answer: where did time or failure occur?

- Deferred for the validation release because TripLedger is one modular monolith and does not yet call production external systems.
- Revisit tracing before production connectors, asynchronous external workflows, or multi-service deployment.

## Backup And Restore Rule

A backup is not proven until it has been restored in a safe environment.

For validation release, backup and restore are proven locally through:

- manifest checksum validation;
- destructive restore only against safe local/synthetic data;
- critical table count comparison;
- post-restore smoke checks.

For real pilot, the same path must additionally prove:

- encrypted backup storage;
- restricted access and access logging;
- retention policy;
- restore timing against declared RTO;
- backup age against declared RPO;
- named operational owner.

## Exit Gate Assessment

Stage 9 is complete for a validation/demo release when:

- a version tag exists;
- deployment records can be filled for each environment;
- smoke checks are repeatable;
- logs and metrics have documented operational meaning;
- backup and restore evidence is captured;
- incident records have a standard format;
- known production gaps are documented.

Stage 9 is not complete for real customer-data production until:

- production OIDC and MFA are integrated;
- HTTPS and certificates are configured;
- secrets come from an external manager;
- encrypted backups and restore rehearsal are proven in a production-like environment;
- alert routing and dashboard ownership are active;
- dependency, secret, and image scans pass release policy;
- independent security review is complete.

## Current Recommendation

Release `v0.1.0-validation` as a validation/demo operations release only.

Do not accept real tourism operator financial data until the production-readiness gaps are closed or formally accepted by an accountable owner.
