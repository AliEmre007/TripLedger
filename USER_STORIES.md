# USER_STORIES.md

## TripLedger MVP User Stories

**Version:** 0.1  
**Date:** 13 July 2026

Story format:

> As a **role**, I want **capability**, so that **measurable outcome**.

Each P0 story references acceptance criteria in `ACCEPTANCE_CRITERIA.md`.

---

## Epic E1 — Identity and Controlled Access

### US-001 — Use role-appropriate access

**Priority:** P0  
**Size:** M  
**Story:** As an Administrator, I want users to have explicit roles so that financial actions are limited to authorised staff.

**Outcome:** Operations cannot perform controlled financial actions; Finance can reconcile; Manager can review without changing data.

**Acceptance:** AC-001, AC-002, AC-003  
**Business rules:** BR-ORG-001, BR-ORG-002, BR-AUTH-001, BR-AUTH-002, BR-AUTH-003  
**Edge cases:** deactivated user, stale session, direct API request, guessed foreign organisation identifier.

### US-002 — Require stronger authentication for finance

**Priority:** P0  
**Size:** S  
**Story:** As an owner, I want Finance and Administrator users to use MFA so that sensitive financial workflows are better protected.

**Acceptance:** AC-004  
**Edge cases:** MFA not enrolled, recovery flow, deactivated account.

---

## Epic E2 — Source and Import Management

### US-010 — Register a source system

**Priority:** P0  
**Size:** S  
**Story:** As an Administrator, I want to register booking, payment, channel, and supplier sources so that imported records preserve their origin.

**Acceptance:** AC-010  
**Business rules:** BR-IMP-001, BR-IMP-005  
**Edge cases:** duplicate external code, inactive source, time-zone omission.

### US-011 — Import bookings from a controlled template

**Priority:** P0  
**Size:** L  
**Story:** As an Operations or Finance user, I want to import booking records from a versioned CSV template so that I do not re-enter existing bookings.

**Acceptance:** AC-011, AC-012, AC-013, AC-014  
**Business rules:** BR-IMP-001–BR-IMP-008, BR-BKG-001–BR-BKG-004  
**Edge cases:** malformed file, mixed valid/invalid rows, duplicate records, stale version, unsupported currency.

### US-012 — Import financial events

**Priority:** P0  
**Size:** L  
**Story:** As a Finance user, I want to import payments, settlements, fees, commissions, and refunds so that they can be reconciled against bookings.

**Acceptance:** AC-015, AC-016, AC-017  
**Business rules:** BR-IMP-001–BR-IMP-008, BR-MNY-001–BR-MNY-009, BR-FIN-001–BR-FIN-008  
**Edge cases:** negative amount, invalid precision, unknown booking reference, duplicate payment, missing effective date.

### US-013 — Import supplier obligations

**Priority:** P0  
**Size:** M  
**Story:** As an Operations or Finance user, I want to import supplier obligations so that expected booking cost is visible.

**Acceptance:** AC-018, AC-019  
**Business rules:** BR-SUP-001–BR-SUP-005  
**Edge cases:** unlinked obligation, supplier credit, cancelled service, changed invoice.

### US-014 — Understand import failures

**Priority:** P0  
**Size:** M  
**Story:** As a user, I want row-level import results so that I can correct invalid data without database intervention.

**Acceptance:** AC-020, AC-021  
**Business rules:** BR-IMP-004, BR-IMP-006  
**Edge cases:** file-level failure, partial success, unexpected processing failure.

---

## Epic E3 — Booking Financial View

### US-020 — Inspect a canonical booking

**Priority:** P0  
**Size:** M  
**Story:** As a Finance user, I want one booking page with items, source identities, and statuses so that I do not search several exports.

**Acceptance:** AC-030  
**Business rules:** BR-BKG-001–BR-BKG-007  
**Edge cases:** cancelled item, multiple source versions, no customer details.

### US-021 — Record supplier cost changes safely

**Priority:** P0  
**Size:** M  
**Story:** As an Operations user, I want to update expected supplier information before finance control so that operations and finance use the same booking cost.

**Acceptance:** AC-031, AC-032  
**Business rules:** BR-SUP-003, BR-SUP-004, BR-BKG-007  
**Edge cases:** change after reconciliation, supplier credit, missing booking item.

### US-022 — View expected booking economics

**Priority:** P0  
**Size:** L  
**Story:** As a Finance user, I want to see expected receivable, fees, supplier cost, and estimated gross margin so that I understand the booking's planned financial result.

**Acceptance:** AC-033, AC-034, AC-035  
**Business rules:** BR-ECO-001–BR-ECO-008  
**Edge cases:** unknown supplier cost, cancellation fee, partial refund, mixed currencies, missing rate.

### US-023 — Trace every calculation

**Priority:** P0  
**Size:** M  
**Story:** As a reviewer, I want every subtotal to show its component records and formula so that I can trust and verify the result.

**Acceptance:** AC-036  
**Business rules:** BR-ECO-007, BR-ECO-008, BR-AUD-003  
**Edge cases:** reversed event, replaced source version, converted amount.

---

## Epic E4 — Matching and Reconciliation

### US-030 — Automatically match unambiguous records

**Priority:** P0  
**Size:** L  
**Story:** As a Finance user, I want exact and unambiguous records to match automatically so that I review only exceptions.

**Acceptance:** AC-040, AC-041, AC-042  
**Business rules:** BR-MAT-001–BR-MAT-006  
**Edge cases:** two candidate bookings, amount outside tolerance, cross-currency payment, one settlement covering several bookings.

### US-031 — Manually allocate a financial record

**Priority:** P0  
**Size:** L  
**Story:** As a Finance user, I want to allocate one payment or settlement across bookings so that batched or partial records can be reconciled.

**Acceptance:** AC-043, AC-044, AC-045  
**Business rules:** BR-MAT-005–BR-MAT-008  
**Edge cases:** overallocation, removed match, concurrent allocation, remaining balance.

### US-032 — See an accurate reconciliation status

**Priority:** P0  
**Size:** M  
**Story:** As a Finance user, I want the system to distinguish not-ready, partial, reconciled, discrepant, and accepted-variance states so that the next action is clear.

**Acceptance:** AC-046, AC-047, AC-048  
**Business rules:** BR-REC-001–BR-REC-007  
**Edge cases:** later source event, missing supplier cost, unresolved small variance, cancelled booking.

### US-033 — Re-run reconciliation safely

**Priority:** P1  
**Size:** S  
**Story:** As a Finance user, I want to re-run one booking or import batch so that corrected data is evaluated without duplicating effects.

**Acceptance:** AC-049  
**Business rules:** BR-IMP-002, BR-REC-006, BR-REC-007

---

## Epic E5 — Discrepancy Workflow

### US-040 — Review an explainable discrepancy queue

**Priority:** P0  
**Size:** L  
**Story:** As a Finance user, I want discrepancies classified and prioritised so that I focus on material exceptions.

**Acceptance:** AC-050, AC-051, AC-052  
**Business rules:** BR-DIS-001–BR-DIS-003  
**Edge cases:** repeated recalculation, zero-value data error, ageing threshold, no owner.

### US-041 — Investigate a discrepancy

**Priority:** P0  
**Size:** M  
**Story:** As a Finance or Operations user, I want evidence, source records, comments, and history on the discrepancy so that investigation is not repeated.

**Acceptance:** AC-053  
**Business rules:** BR-DIS-004, BR-AUD-002, BR-AUD-003  
**Edge cases:** source file unavailable, multiple related events, restricted financial detail for Operations.

### US-042 — Resolve a corrected discrepancy

**Priority:** P0  
**Size:** M  
**Story:** As a Finance user, I want a corrected source or match to reprocess the booking so that the discrepancy closes only when the numbers are valid.

**Acceptance:** AC-054, AC-055  
**Business rules:** BR-DIS-004, BR-DIS-005, BR-REC-007  
**Edge cases:** later invalidating event, correction creates another discrepancy.

### US-043 — Accept a documented business variance

**Priority:** P0  
**Size:** M  
**Story:** As an authorised Finance user, I want to accept a legitimate variance with evidence so that the booking can close without hiding the difference.

**Acceptance:** AC-056, AC-057  
**Business rules:** BR-REC-005, BR-DIS-004, BR-AUTH-001  
**Edge cases:** unauthorised role, missing note, duplicate financial effect, cross-tenant defect.

### US-044 — Wait for an external event

**Priority:** P0  
**Size:** S  
**Story:** As a Finance user, I want to mark a timing difference with an expected date so that it does not disappear and returns when overdue.

**Acceptance:** AC-058  
**Business rules:** BR-DIS-006  
**Edge cases:** due date passes, source arrives early, owner deactivated.

---

## Epic E6 — Audit, Reporting, and Export

### US-050 — Inspect the booking timeline

**Priority:** P0  
**Size:** L  
**Story:** As a Finance user, I want one chronological timeline of source events, calculations, matches, discrepancies, and adjustments so that I can reconstruct the booking.

**Acceptance:** AC-060, AC-061  
**Business rules:** BR-AUD-001–BR-AUD-004  
**Edge cases:** reversed events, source re-import, system actor, user deactivation.

### US-051 — Review management exposure

**Priority:** P0  
**Size:** M  
**Story:** As a Manager, I want summary values and counts linked to underlying records so that I can understand unresolved financial exposure.

**Acceptance:** AC-062, AC-063  
**Edge cases:** date filters, currency conversion, stale dashboard calculation.

### US-052 — Export controlled accounting evidence

**Priority:** P0  
**Size:** M  
**Story:** As a Finance user, I want a versioned CSV export with source and audit references so that an accountant can review the result.

**Acceptance:** AC-064, AC-065  
**Business rules:** BR-AUD-005  
**Edge cases:** unauthorised export, no rows, large export, later regeneration.

---

## Epic E7 — Operational Quality

### US-060 — Recover from processing failure

**Priority:** P0  
**Size:** M  
**Story:** As an operator, I want failed jobs to retry in a bounded way and expose diagnostics so that data does not silently stop processing.

**Acceptance:** AC-070, AC-071  
**Business rules:** BR-IMP-004, NFR-REL-003, NFR-OBS-005  
**Edge cases:** persistent failure, duplicate retry, partial prior result.

### US-061 — Restore the service and data

**Priority:** P0  
**Size:** M  
**Story:** As an operator, I want tested backup and restore procedures so that the pilot can recover from data loss or failed deployment.

**Acceptance:** AC-072  
**Edge cases:** missing backup, corrupt backup, configuration mismatch.

### US-062 — Diagnose a user-visible error

**Priority:** P0  
**Size:** S  
**Story:** As support personnel, I want stable error codes and correlation identifiers so that I can investigate without asking users for database details.

**Acceptance:** AC-073  
**Edge cases:** security-sensitive error, background job, repeated request.

---

## Story Definition of Ready

A story may enter implementation only when:

- User outcome is explicit.
- Acceptance scenarios are reviewed.
- Business rules are linked.
- Required data examples exist.
- Role and permission are known.
- Edge cases are listed.
- Size is S, M, or L.
- Dependencies are available.
- No unresolved question could change the core workflow.

## Story Definition of Done

A P0 story is done only when:

- Acceptance criteria pass.
- Domain and authorisation tests pass.
- Audit evidence is verified.
- Failure scenarios are demonstrated.
- Observability is present.
- Documentation and example data are updated.
- No critical or high-severity defect remains open.
