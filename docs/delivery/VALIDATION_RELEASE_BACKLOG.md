# TripLedger Validation Release Backlog

**Stage:** 5 - Delivery Planning  
**Date:** 13 July 2026  
**Version:** 0.1  
**Scope:** 12-14 week validation release

## 1. Sizing

| Size | Effort | Rule |
|---|---:|---|
| S | 2-4h | Small, low uncertainty |
| M | 4-8h | One short delivery cycle |
| L | 8-16h | Maximum accepted size |
| XL | >16h | Not ready; split before delivery |

## 2. Prioritized Backlog

| Order | ID | Slice | Value | Risk or learning | Depends on | Size | Acceptance or evidence |
|---:|---|---|---|---|---|---:|---|
| 1 | VR-001 | Acceptance fixtures and executable rule examples | Developers and agents have concrete import, money, matching, and discrepancy examples | Prevents ambiguous implementation | None | M | AC-011-AC-021, AC-033-AC-036, AC-040-AC-042 examples exist |
| 2 | VR-002 | Project skeleton and quality gates | A clean environment can run tests and checks early | Reduces late integration and setup risk | VR-001 | M | documented local run, test command, lint/static check command |
| 3 | VR-003 | Organisation model and actor context | Every protected operation has tenant and actor context | Tenant leakage risk R-020/R-021 | VR-002 | M | AC-002 negative cross-org tests |
| 4 | VR-004 | Role enforcement and MFA policy boundary | Operations cannot perform finance-only actions | Privilege escalation risk R-021/R-025 | VR-003 | M | AC-001, AC-004 |
| 5 | VR-005 | Source-system registry | Imports have controlled origin and time zone | Provenance and integration risk | VR-003 | S | AC-010 |
| 6 | VR-006 | Common error model and correlation ids | Users and support can diagnose safely | Observability risk R-041 | VR-003 | S | AC-073 |
| 7 | VR-007 | Import-batch lifecycle and row-result model | Users can see accepted, duplicate, rejected, and failed rows | Silent data loss risk | VR-005, VR-006 | M | AC-012, AC-020, AC-021 |
| 8 | VR-008 | Booking CSV import | Bookings become canonical records through source identity | Duplicate/stale import risk R-011/R-012 | VR-007 | L | AC-011, AC-013, AC-014 |
| 9 | VR-009 | Canonical booking detail | Users can inspect booking, items, status, and provenance | Confirms model usability early | VR-008 | M | AC-030 |
| 10 | VR-010 | Supplier-obligation import | Supplier cost enters the financial-control model | Validates differentiating domain concept | VR-007, VR-008 | M | AC-018, AC-019 |
| 11 | VR-011 | Financial-event CSV import | Payments, settlements, fees, and refunds enter safely | Core reconciliation input risk | VR-007, VR-008 | L | AC-015, AC-016, AC-017 |
| 12 | VR-012 | Immutable financial reversal path | Incorrect accepted events are corrected without overwrite | Audit and replay risk R-023/R-032 | VR-011 | M | BR-FIN-001/002 tests, AC-048 support |
| 13 | VR-013 | Exact money and currency precision | Financial values use exact decimal and currency rules | Rounding/currency risk R-015 | VR-010, VR-011 | M | AC-016, BR-MNY-001-009 |
| 14 | VR-014 | Exchange-rate evidence | Cross-currency records remain explicit and reproducible | FX variance risk R-015/R-052 | VR-013 | S | AC-042, AC-036 rate evidence |
| 15 | VR-015 | Expected booking economics calculator | Finance sees expected receivable, deductions, cost, margin | Validates main product value | VR-009-VR-014 | L | AC-033, AC-034, AC-035 |
| 16 | VR-016 | Calculation explanation | Reviewers can trace subtotals to source records and rules | Trust/explainability risk | VR-015 | M | AC-036 |
| 17 | VR-017 | Deterministic one-to-one matcher | Exact unique records match automatically | False-positive risk R-013 | VR-011, VR-015 | L | AC-040, AC-041, AC-042 |
| 18 | VR-018 | Reconciliation state engine | Each booking has actionable financial state | Core workflow completion | VR-015, VR-017 | M | AC-046, AC-047, AC-048, AC-049 |
| 19 | VR-019 | Basic discrepancy generation and deduplication | Material mismatches become explicit work items | Hidden exception risk | VR-018 | L | AC-050, AC-051 |
| 20 | VR-020 | Basic discrepancy list and detail | Finance can inspect exception evidence | User-value validation | VR-019 | M | AC-052, AC-053 reduced validation-release scope |
| 21 | VR-021 | Booking timeline and audit projection | Reviewer can reconstruct financial history | Audit risk R-023/R-032 | VR-008-VR-020 | L | AC-060, AC-061 |
| 22 | VR-022 | Background job state and bounded retry | Failed processing is visible and bounded | Job stall/duplicate risk R-040 | VR-007, VR-006 | M | AC-070, AC-071 |
| 23 | VR-023 | Logs, metrics, liveness, readiness | Operators can diagnose service state | Observability risk R-041 | VR-006, VR-022 | M | NFR-OBS evidence, AC-073 |
| 24 | VR-024 | Deployment and environment documentation | A new environment is reproducible | Delivery risk R-042 | VR-002 | M | NFR-DEP-001, deployment smoke check |
| 25 | VR-025 | Backup and restore rehearsal | Pilot data can be recovered | Data-loss risk R-033 | VR-021, VR-024 | L | AC-072 |
| 26 | VR-026 | End-to-end validation demo dataset | The product hypothesis is demonstrated across normal and discrepant cases | Reduces synthetic happy-path risk R-062 | VR-001-VR-025 | L | Demonstration scenarios subset: exact booking, OTA settlement, cancellation/refund, ambiguous payment, duplicate import, FX, short settlement, forbidden adjustment, restore |

## 3. Backlog Control Rules

- No item enters implementation unless it satisfies `DEFINITION_OF_READY.md`.
- No item is accepted unless it satisfies `DEFINITION_OF_DONE.md`.
- A slice that grows beyond L must be split before implementation continues.
- No deferred item enters the validation release unless an existing item of equivalent effort is removed.
- Security, audit, tenant isolation, exact money, and idempotency cannot be postponed to a final hardening phase.
- A successful UI happy path is not enough; failure behavior and evidence are part of the slice.

## 4. Traceability to Full MVP Backlog

| Validation item range | Full backlog source |
|---|---|
| VR-001 | F-001 |
| VR-003-VR-006 | F-002, F-003, F-004, F-027 |
| VR-007-VR-009 | F-005, F-006, F-007 |
| VR-010-VR-014 | F-008, F-010, F-011, F-012 |
| VR-015-VR-016 | F-013, F-014 |
| VR-017-VR-020 | F-015, F-017, F-018, F-019, F-020 partial |
| VR-021 | F-024 |
| VR-022-VR-025 | F-028, F-029, F-030 |
| VR-026 | F-032 validation subset |

Deferred full-MVP items remain in `FEATURE_BACKLOG.md`.
