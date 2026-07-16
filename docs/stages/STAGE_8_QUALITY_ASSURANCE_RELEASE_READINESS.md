# Stage 8 - Quality Assurance and Release Readiness

## Purpose

Prove the validation release candidate is safe enough for its intended environment. A release is treated as an operational event, not only a code merge.

## Candidate

- Candidate commit: `a585b9d7683d97b26e8c6846d369ff7b945b1ed6`
- Validation scope: validation release backlog items `VR-001` through `VR-026`
- Evidence date: 2026-07-16
- Deployment target used for this stage: isolated local Docker Compose validation stack
- Isolated stack name: `tripledger_stage8`
- Application port: `18082`
- Database: PostgreSQL 16 container managed by Docker Compose

This evidence is sufficient for a validation-release candidate and staging review. It is not approval for real-data pilot or production operation until the residual risks in this document are closed or explicitly accepted.

## Release Checklist

| Check | Result | Evidence |
| --- | --- | --- |
| Candidate source pinned | Passed | Candidate commit recorded as `a585b9d7683d97b26e8c6846d369ff7b945b1ed6`. |
| Automated unit, integration, controller, and migration tests | Passed | `make verify` completed successfully on 2026-07-16T10:59:14Z with 155 tests, 0 failures, 0 errors, 0 skipped, and 0 Checkstyle violations; rerun after Stage 8 documentation on 2026-07-16T11:14:37Z with the same result. |
| Validation demo dataset | Passed | `make demo-validate` completed successfully on 2026-07-16T10:57:37Z with 6 tests, 0 failures, 0 errors, 0 skipped. |
| Runtime image build | Passed | `docker compose up -d --build` built the application image from the candidate source. The image build ran Maven package successfully with 155 tests, 0 failures, 0 errors, 0 skipped, and finished at 2026-07-16T11:03:19Z. |
| Runtime smoke checks | Passed | `APP_PORT=18082 make smoke` passed against the isolated Stage 8 stack. |
| Migration verification | Passed | Runtime startup applied/validated Flyway schema version `17`; backup manifest and restore evidence both recorded `schemaVersion` `17`. |
| Health checks | Passed | Smoke checks confirmed `/api/v1/health` and `/actuator/health` on port `18082`. |
| Backup creation | Passed | Backup created at `/tmp/tripledger-stage8-backups/stage8-release-readiness`; manifest created at `manifest.json`. |
| Restore rehearsal | Passed | Restore reset the isolated database, restored the backup, and wrote `restore-evidence.json` with status `RESTORED`. |
| Post-restore smoke checks | Passed | `APP_PORT=18082 make smoke` passed after restore. |
| Security boundary coverage | Passed for validation scope | Automated tests cover role checks, MFA-required paths, organisation-scoped lookup behavior, stable error responses, and unauthorised financial actions. |
| Observability | Passed for validation scope | Correlation id behavior, request logging, liveness/readiness, and Actuator health endpoints are present and covered by automated or smoke checks. |
| Rollback plan | Documented | See rollback plan below. |
| Release notes | Documented | See release notes below. |
| Known residual risk | Documented | See known-risk statement below. |

## Staging Validation Result

Stage 8 used an isolated local Docker Compose validation target because a shared staging environment is not defined in this repository yet.

Validation result:

- Stack built from the candidate source.
- PostgreSQL became healthy.
- Application started successfully on port `18082`.
- Smoke checks passed before backup and restore.
- Backup was created from the running stack.
- Restore rehearsal reset and restored the isolated database.
- Smoke checks passed again after restore.

Before a real-data pilot, repeat this same checklist in a pilot-like staging environment with production-equivalent secrets handling, network boundaries, backup storage, log retention, and alert routing.

## Migration Verification

No new schema migration was introduced by Stage 8 itself.

The candidate includes Flyway migrations through schema version `17`. Runtime startup and backup/restore evidence both confirmed schema version `17`.

Backup manifest:

- Backup id: `stage8-release-readiness`
- Created at: 2026-07-16T11:06:39Z
- Commit SHA: `a585b9d7683d97b26e8c6846d369ff7b945b1ed6`
- Schema version: `17`
- Dump file: `tripledger.sql`
- Dump SHA-256: `a8a31532ffd5fa9b03709532484c64e9f3401e268651d4b79bf6075d236cd443`

Restore evidence:

- Restored at: 2026-07-16T11:11:44Z
- Backup id: `stage8-release-readiness`
- Status: `RESTORED`
- Schema version: `17`

Critical table counts were unchanged across backup and restore for:

- `booking`: 0
- `financial_event`: 0
- `booking_match`: 0
- `discrepancy`: 0
- `audit_event`: 0

The zero counts are expected for this isolated runtime stack because the validation demo dataset is file-based test evidence, not seeded runtime data.

## Rollback Plan

Use this rollback path if deployment validation fails, smoke checks fail, migration effects are incorrect, or operational monitoring shows unsafe behavior.

1. Stop traffic to the candidate deployment.
2. Preserve application logs, database logs, correlation ids, backup manifests, and restore evidence.
3. Redeploy the last known-good application image or commit.
4. If the database state is unsafe or migration/data effects must be reversed, restore from the last verified backup using the restore rehearsal process.
5. Run smoke checks against `/api/v1/health` and `/actuator/health`.
6. Compare critical table counts and schema version against the backup manifest.
7. Record the rollback decision, cause, operator, restored backup id, and validation result.
8. Communicate impact and next action to product, operations, and pilot stakeholders.

Rollback is recoverable for local validation because backup and restore were rehearsed successfully. For real-data pilot readiness, the same procedure must be proven with encrypted backup storage, access controls, retention policy, and operational ownership.

## Release Notes

The validation release candidate provides the end-to-end backend foundation for TripLedger financial-control validation:

- Health, readiness, request correlation, and stable API error behavior.
- Organisation-scoped core records and actor context.
- Source system, import batch, source record, booking, booking item, supplier, supplier obligation, financial event, exchange rate, matching, reconciliation, discrepancy, audit timeline, and background job storage.
- Exact-money handling and explicit unknown/not-ready financial states.
- Deterministic matching and reconciliation behavior with explainable calculation snapshots.
- Accepted financial-event immutability with reversal support.
- Security-sensitive action checks for role and MFA-required paths.
- Backup and restore rehearsal scripts with manifest and restore evidence.
- Deployment smoke checks and validation demo dataset.

Not included in this candidate:

- Production OTA, bank, payment, or accounting connectors.
- Production OIDC provider configuration.
- UI workflow for manual match creation.
- Real-data pilot approval.
- Production alert routing, backup encryption, and backup access audit integration.

## Known-Risk Statement

Residual risks for release readiness:

- Authentication is still a local/header-backed adapter for validation. A production OIDC/MFA configuration must be implemented and tested before real-data pilot.
- The Stage 8 validation target was local Docker Compose, not a shared production-like staging environment.
- Health checks, request logs, and metrics exist, but production alert routing and dashboard ownership are not wired in this repository.
- Backup and restore are rehearsed locally, but pilot operation requires encrypted backup storage, retention policy, access logging, and restore ownership.
- The validation demo uses file-based fixtures and seeded manual-match evidence. A real operator workflow for manual matching is outside this candidate.
- External integrations remain intentionally provider-neutral and are not production connectors.
- Capacity and performance testing beyond the current automated suite has not been completed.

## Exit Gate Assessment

The candidate is test-proven, observable at the validation level, recoverable through rehearsed backup/restore, and safe to advance to staging review with the residual risks above documented.

It is not yet safe for real-data pilot or production release until staging validation, production authentication, alert ownership, and backup controls are completed or formally accepted.
