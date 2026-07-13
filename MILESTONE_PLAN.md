# TripLedger Milestone Plan

**Stage:** 5 - Delivery Planning  
**Date:** 13 July 2026  
**Version:** 0.1  
**Planning basis:** 10-12 focused hours per week

## 1. Milestone Plan

| Milestone | Weeks | Backlog items | Planned effort | Exit evidence |
|---|---:|---|---:|---|
| M0 - Executable Requirements | 1 | VR-001 | 6h | Fixtures and executable rule examples committed |
| M1 - Security and Provenance Foundation | 2-3 | VR-002-VR-006 | 22h | Local run, actor context, role/MFA checks, source registry, error model |
| M2 - Import Control | 4-5 | VR-007-VR-010 | 30h | Booking and supplier imports with row results, provenance, idempotency |
| M3 - Financial Evidence | 6-7 | VR-011-VR-014 | 22h | Financial event import, reversal, exact money, FX evidence |
| M4 - Economics | 8 | VR-015-VR-016 | 18h | Expected economics and explanation for acceptance cases |
| M5 - Matching and Reconciliation | 9-10 | VR-017-VR-020 | 28h | Unique match, ambiguity refusal, status engine, basic discrepancy view |
| M6 - Timeline and Operations | 11-12 | VR-021-VR-023 | 20h | Audit timeline, bounded jobs, logs, metrics, health |
| M7 - Recovery and Demo | 13-14 | VR-024-VR-026 | 30h | Deployment docs, backup/restore, end-to-end demo |

The plan intentionally exceeds the 116h likely estimate because it includes uncertainty and operational evidence. If actual weekly capacity is closer to 12h and early slices stay small, the plan can finish nearer 12 weeks. If capacity is closer to 10h, 14 weeks is the realistic target.

## 2. Milestone Detail

### M0 - Executable Requirements

Goal:

- make the domain executable before implementation grows.

Exit:

- import templates and sample CSVs exist;
- expected row outcomes are documented;
- calculation expected values are represented as tests or machine-readable fixtures;
- duplicate, stale, invalid money, ambiguous match, and cross-org examples exist.

### M1 - Security and Provenance Foundation

Goal:

- make every future feature inherit tenant, actor, role, source, and error context.

Exit:

- app runs locally;
- authenticated actor context is available to services;
- role denial tests pass;
- cross-org access test shape exists;
- source-system registry works;
- errors include stable code and correlation id.

### M2 - Import Control

Goal:

- prove source files can safely create canonical operational records.

Exit:

- import batch statuses work;
- every row has accepted, duplicate, rejected, or warning result;
- booking import is idempotent;
- stale source version does not overwrite newer state;
- booking detail exposes provenance;
- supplier obligations import and unlinked obligations are visible.

### M3 - Financial Evidence

Goal:

- prove financial data can enter without unsafe mutation or hidden conversion.

Exit:

- financial event import supports payment, settlement, fee, commission, refund;
- invalid currency and precision are rejected;
- unmatched events are preserved;
- accepted event cannot be edited;
- reversal/replacement path exists;
- exchange-rate evidence supports cross-currency acceptance cases.

### M4 - Economics

Goal:

- prove TripLedger can calculate expected booking outcome.

Exit:

- normal booking economics match AC-033;
- cancellation/refund case matches AC-034;
- missing supplier cost produces `UNKNOWN` and `NOT_READY`;
- formula explanation shows component records and rule version.

### M5 - Matching and Reconciliation

Goal:

- prove exact safe records reconcile and unsafe records stay visible.

Exit:

- unique exact match creates match with rule evidence;
- ambiguous match creates reviewable discrepancy or warning;
- cross-currency match requires rate evidence;
- reconciliation statuses cover `NOT_READY`, `PARTIALLY_RECONCILED`, `RECONCILED`, `DISCREPANT`;
- rerun does not duplicate financial effect.

### M6 - Timeline and Operations

Goal:

- prove a reviewer and operator can reconstruct and diagnose the system.

Exit:

- booking timeline distinguishes source, system, and user-controlled events;
- material financial actions have audit events;
- bounded retry and final failed state are visible;
- logs and metrics include correlation ids and safe categories;
- health and readiness checks are documented.

### M7 - Recovery and Demo

Goal:

- prove the validation release is reproducible, recoverable, and demonstrable.

Exit:

- environment setup and deployment notes are current;
- backup/restore rehearsal meets validation target;
- end-to-end demo covers normal, discrepant, duplicate, ambiguous, FX, cancellation/refund, forbidden action, and restore scenarios;
- known limitations and deferred items are documented.

## 3. Replanning Triggers

Replan immediately when:

- a planned item grows beyond L;
- cross-org or financial invariant tests fail repeatedly;
- real user evidence contradicts supplier-obligation or CSV-first assumptions;
- production database concurrency test fails;
- legal/privacy review changes data retention or hosting assumptions;
- weekly capacity drops below 8 focused hours for two consecutive weeks.

Replanning must remove, split, or resequence scope. It must not weaken security, audit, idempotency, exact money, or deterministic matching rules.
