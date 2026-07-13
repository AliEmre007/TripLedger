# FEATURE_BACKLOG.md

## TripLedger MVP Feature Backlog

**Version:** 0.1  
**Date:** 13 July 2026  
**Planning capacity:** approximately 10–12 focused engineering hours per week

---

## 1. Sizing Model

| Size | Expected focused effort | Delivery rule |
|---|---:|---|
| S | 2–4 hours | Fits comfortably inside one iteration |
| M | 4–8 hours | Fits inside one iteration |
| L | 8–16 hours | Maximum permitted feature size; split if uncertainty is high |
| XL | More than 16 hours | Not implementation-ready; must be decomposed |

Effort includes implementation, automated tests, documentation, and acceptance evidence.

Priority:

- **P0:** MVP exit requirement.
- **P1:** Next increment after P0 stability.
- **P2:** Later product enhancement.

---

## 2. Ordered P0 Backlog

| Order | ID | Feature slice | User outcome | Size | Depends on | Stories | Acceptance |
|---:|---|---|---|---:|---|---|---|
| 1 | F-001 | Canonical import contracts and acceptance fixtures | Team has executable examples of valid and invalid booking, finance, and supplier data | M | — | US-011, US-012, US-013 | AC-011–AC-021 |
| 2 | F-002 | Organisation boundary and role enforcement | Users can access only permitted organisation data and actions | L | F-001 | US-001 | AC-001–AC-003 |
| 3 | F-003 | MFA policy and protected financial access | Finance workflows require stronger authentication | S | F-002 | US-002 | AC-004 |
| 4 | F-004 | Source-system registry | Every import has a controlled origin | S | F-002 | US-010 | AC-010 |
| 5 | F-005 | Import-batch lifecycle and row result model | Users can see complete import outcomes | M | F-001, F-004 | US-014 | AC-020, AC-021 |
| 6 | F-006 | Booking CSV import | Users create canonical bookings without manual re-entry | L | F-005 | US-011 | AC-011–AC-014 |
| 7 | F-007 | Canonical booking detail | Users can inspect current booking items, source, and history | M | F-006 | US-020 | AC-030 |
| 8 | F-008 | Supplier-obligation import | Booking cost enters the same control model | M | F-005, F-006 | US-013 | AC-018, AC-019 |
| 9 | F-009 | Supplier-cost controlled update | Operations and Finance share updated cost without destructive overwrite | M | F-007, F-008 | US-021 | AC-031, AC-032 |
| 10 | F-010 | Financial-event CSV import | Payments, settlements, fees, and refunds enter the control model | L | F-005, F-006 | US-012 | AC-015–AC-017 |
| 11 | F-011 | Immutable financial correction | Incorrect events are reversed and replaced safely | M | F-010 | US-012 | AC-015–AC-017 plus BR-FIN-001/002 tests |
| 12 | F-012 | Currency and exchange-rate handling | Mixed currencies remain explicit and reproducible | M | F-008, F-010 | US-022, US-030 | AC-035, AC-036, AC-042 |
| 13 | F-013 | Expected booking-economics calculator | Finance sees receivable, deductions, cost, and estimated margin | L | F-007–F-012 | US-022 | AC-033–AC-035 |
| 14 | F-014 | Calculation explanation | Reviewers can trace every subtotal to source and formula | M | F-013 | US-023 | AC-036 |
| 15 | F-015 | Deterministic one-to-one matching | Exact, unique records reconcile automatically | L | F-010, F-013 | US-030 | AC-040–AC-042 |
| 16 | F-016 | Allocation model and manual match | Batched and partial payments can be controlled safely | L | F-015 | US-031 | AC-043–AC-045 |
| 17 | F-017 | Reconciliation state engine | Each booking has an accurate actionable state | M | F-013, F-015, F-016 | US-032 | AC-046–AC-048 |
| 18 | F-018 | Discrepancy generation and deduplication | Material exceptions become explicit work items | L | F-017 | US-040 | AC-050, AC-051 |
| 19 | F-019 | Discrepancy queue and filters | Finance can prioritise exceptions | M | F-018 | US-040 | AC-052 |
| 20 | F-020 | Discrepancy evidence and comments | Investigation context remains with the case | M | F-018 | US-041 | AC-053 |
| 21 | F-021 | Correct-and-reprocess resolution | Corrected records close discrepancies only when rules pass | M | F-017–F-020 | US-042 | AC-054, AC-055 |
| 22 | F-022 | Accepted variance control | Legitimate business differences close with visible evidence | M | F-020 | US-043 | AC-056, AC-057 |
| 23 | F-023 | Waiting-external lifecycle | Timing differences stay controlled and return when overdue | S | F-019 | US-044 | AC-058 |
| 24 | F-024 | Booking audit timeline | A reviewer can reconstruct the financial history | L | F-006–F-023 | US-050 | AC-060, AC-061 |
| 25 | F-025 | Management dashboard | Managers see reconciled status and unresolved exposure | M | F-017–F-023 | US-051 | AC-062, AC-063 |
| 26 | F-026 | Versioned accounting export | Finance can provide traceable records externally | M | F-024, F-025 | US-052 | AC-064, AC-065 |
| 27 | F-027 | Structured errors and correlation IDs | Users and support can diagnose failures safely | S | F-002 | US-062 | AC-073 |
| 28 | F-028 | Bounded job retry and failed-job visibility | Processing failures are visible and do not duplicate effects | M | F-005, F-027 | US-060 | AC-070, AC-071 |
| 29 | F-029 | Metrics, health, and operational runbook | Operators can detect and investigate service problems | M | F-027, F-028 | US-060, US-062 | NFR-OBS evidence |
| 30 | F-030 | Backup, restore, deployment verification | The pilot can be recovered and reproduced | L | All data features stable | US-061 | AC-072 |
| 31 | F-031 | Reference performance suite | Capacity claims are measured rather than assumed | M | F-006–F-026 | — | AC-080–AC-082 |
| 32 | F-032 | End-to-end MVP demonstration dataset | Product hypothesis is demonstrated across normal and failure cases | L | F-001–F-031 | All P0 | Demonstration scenarios 1–15 |

---

## 3. Recommended Iteration Groups

These are planning groups, not commitments to a specific framework.

### Iteration 0 — Executable requirements

- F-001 Canonical import contracts and acceptance fixtures
- Initial business-rule tests
- Demonstration dataset skeleton

**Exit:** Examples make the domain rules unambiguous before APIs are built.

### Iteration 1 — Security and provenance foundation

- F-002 Organisation boundary and role enforcement
- F-003 MFA policy
- F-004 Source-system registry
- F-027 Structured errors

**Exit:** Every future record has an owner, origin, actor, and controlled access path.

### Iteration 2 — Import control

- F-005 Import-batch lifecycle
- F-006 Booking CSV import
- F-007 Booking detail
- F-028 Initial retry and failure visibility

**Exit:** Bookings can be imported idempotently with visible errors and provenance.

### Iteration 3 — Cost and financial records

- F-008 Supplier obligations
- F-009 Controlled supplier updates
- F-010 Financial-event import
- F-011 Immutable correction
- F-012 Currency handling

**Exit:** All financial inputs exist without destructive editing or hidden conversion.

### Iteration 4 — Expected economics

- F-013 Economics calculator
- F-014 Calculation explanation

**Exit:** The system can explain planned financial outcome before matching actual records.

### Iteration 5 — Matching

- F-015 Deterministic matching
- F-016 Allocation and manual match
- F-017 Reconciliation state

**Exit:** Normal records reconcile and ambiguous records remain safely open.

### Iteration 6 — Exception workflow

- F-018 Discrepancy generation
- F-019 Queue and filters
- F-020 Evidence and comments
- F-023 Waiting-external lifecycle

**Exit:** A user has a controlled work queue rather than an unexplained difference.

### Iteration 7 — Resolution and audit

- F-021 Correct-and-reprocess
- F-022 Accepted variance
- F-024 Booking audit timeline

**Exit:** Every closure is explainable and reversible through new evidence, not hidden mutation.

### Iteration 8 — Reporting and operational evidence

- F-025 Dashboard
- F-026 Export
- F-029 Observability and runbook
- F-030 Backup and restore
- F-031 Performance suite
- F-032 Final demonstration

**Exit:** The system is reviewable, deployable, diagnosable, recoverable, and demonstrable.

Because available capacity is approximately 10–12 hours per week, an iteration group may span more than one calendar week. Individual feature slices must still fit within one short engineering iteration.

---

## 4. P1 Backlog

| ID | Feature | Outcome | Size | Reason deferred |
|---|---|---|---:|---|
| P1-001 | Import preview | User verifies interpretation before commit | M | Useful but not required for proof |
| P1-002 | Single-booking and batch reconciliation rerun controls | Faster correction workflow | S | Automatic recalculation covers MVP |
| P1-003 | Saved dashboard filters | Repeated management views | S | Convenience |
| P1-004 | Discrepancy email notification | Assigned owner is notified | M | Queue is sufficient initially |
| P1-005 | Base/original currency comparison view | Better FX analysis | M | Core conversion evidence exists |
| P1-006 | Unlinked-record workbench | Faster mapping of orphan events and obligations | M | Basic lists can support MVP |
| P1-007 | Configurable matching-rule administration | Adapt rules without release | L | Rules remain code/config controlled in MVP |
| P1-008 | Accounting-system-specific export mapping | Lower accountant effort | L | Versioned generic export proves workflow |
| P1-009 | Additional customer-data privacy workflows | Data-subject export/anonymisation | L | MVP minimises personal data |
| P1-010 | Pilot ageing alerts | Notify when material discrepancy is overdue | M | Manual queue monitoring acceptable in MVP |

---

## 5. P2 Backlog

| ID | Feature | Reason deferred |
|---|---|---|
| P2-001 | Production OTA API connector | Provider access and user priority unvalidated |
| P2-002 | Bank feed or open-banking connector | Regulatory, provider, and security scope |
| P2-003 | Payment-gateway API connector | File import proves core model first |
| P2-004 | Accounting API synchronisation | Requires target product and field mapping |
| P2-005 | AI-assisted discrepancy classification | Deterministic workflow must be trustworthy first |
| P2-006 | Probabilistic match suggestions | Requires labelled data and strict confidence controls |
| P2-007 | Dynamic pricing | Different product problem |
| P2-008 | Supplier portal | Not required for finance-control hypothesis |
| P2-009 | Native mobile application | Web workflow is sufficient for MVP |
| P2-010 | Multi-organisation SaaS self-service onboarding | Commercialisation after pilot |
| P2-011 | Automated payouts | Regulated and operationally risky |
| P2-012 | Statutory accounting and tax reporting | Jurisdiction-specific accounting product scope |

---

## 6. Backlog Control Rules

- No P1 or P2 item enters implementation while a P0 integrity defect remains.
- A feature that exceeds L must be decomposed.
- A provider-specific integration requires direct user evidence showing it is an adoption blocker.
- An AI feature requires a deterministic fallback and measurable improvement over the existing workflow.
- A performance optimisation requires a measured breach of an NFR or a proven design constraint.
- A backlog item cannot be called complete from a successful happy-path demonstration alone.
