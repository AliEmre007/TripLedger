# TripLedger v0.1.0-validation

## Release Summary

`v0.1.0-validation` is the first validation/demo operations release for TripLedger.

This release is intended to prove the backend financial-control workflow with synthetic data, repeatable deployment, smoke checks, and backup/restore evidence.

It is not approved for real customer financial data.

## Included

- Organisation-scoped backend records.
- Header-backed local actor adapter for validation and tests.
- Role and MFA policy boundaries for protected financial actions.
- Source-system registry.
- Import batch lifecycle and row results.
- Booking CSV import and canonical booking detail.
- Supplier obligation import.
- Financial event CSV import.
- Financial event reversal path.
- Exact money and supported currency validation.
- Exchange-rate evidence.
- Booking economics and calculation explanation.
- Deterministic one-to-one matching.
- Reconciliation state engine.
- Basic discrepancy generation, list, and detail.
- Booking timeline and audit projection.
- Background job retry state and diagnostics.
- Health, readiness, metrics, request logs, and correlation ids.
- Deployment smoke checks.
- Local backup and restore rehearsal tooling.
- End-to-end synthetic validation demo dataset.
- Stage 8 release-readiness evidence.
- Stage 9 deployment and operations evidence.

## Not Included

- Production OIDC provider configuration.
- Production HTTPS/certificate configuration.
- External secret manager integration.
- Production alert routing or concrete dashboards.
- Encrypted backup storage and access logging.
- Real customer-data pilot approval.
- Production OTA, bank, payment, or accounting connectors.
- UI workflow for manual match creation.
- Full performance and capacity baseline.
- Independent security review.

## Release Evidence

| Evidence | Result |
| --- | --- |
| `make verify` | Passed on 2026-07-16T11:30:50Z with 155 tests, 0 failures, 0 errors, 0 skipped, and 0 Checkstyle violations. |
| `make demo-validate` | Passed on 2026-07-16T10:57:37Z with 6 tests, 0 failures, 0 errors, 0 skipped. |
| Runtime image build | Passed during Stage 8 local validation. |
| Smoke checks | Passed before and after restore in Stage 8 isolated validation stack. |
| Backup manifest | Created at `/tmp/tripledger-stage8-backups/stage8-release-readiness/manifest.json` during Stage 8 validation. |
| Restore evidence | Created at `/tmp/tripledger-stage8-backups/stage8-release-readiness/restore-evidence.json` with status `RESTORED`. |
| Schema version | `17`. |

## Operating Instructions

Run a validation deployment with:

```bash
cp .env.example .env
docker compose up --build
make smoke
```

Create local backup evidence with:

```bash
make backup-local
```

Restore only in a safe local or rehearsal environment:

```bash
BACKUP_DIR=/tmp/tripledger-backups/<backup-id> RESTORE_CONFIRM=restore-local make restore-local
make smoke
```

Record each deployment with `docs/operations/DEPLOYMENT_RECORD.md`.

## Known Risks

- Local/header-backed actor adapter is not production authentication.
- Real-data deployments require OIDC, MFA, external secrets, HTTPS, alert routing, encrypted backups, access logging, and restore ownership.
- Stage 8 validation used local Docker Compose, not a shared production-like staging environment.
- Dashboards and alert policies are documented but not implemented against a selected monitoring provider.
- Distributed tracing is deferred until external integrations or multi-service deployment exists.

## Release Decision

Approved for validation/demo use with synthetic or anonymised data only.

Not approved for real customer financial data.
