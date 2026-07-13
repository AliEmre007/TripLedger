# PROOF_OF_CONCEPT_RESULTS.md

## TripLedger Targeted Feasibility Results

**Executed:** 2026-07-13T12:13:51.530569+00:00
**Runtime:** Python 3.13.5, dependency-free standard library
**Result:** 6 passed, 0 failed

These PoCs test uncertain domain and data-integrity choices. They are not a substitute for tests in the selected production language, framework, identity provider, or database.

## POC-001 — Idempotent import identity and stale-version handling

**Status:** PASS  
**Elapsed:** 0.0294 seconds

Explicit source versions support duplicate prevention and stale-update rejection. Checksums prevent exact duplicates for versionless sources, but cannot prove whether a changed payload is newer or older; those sources require ingestion-order policy or manual review.

### Assertions

- [x] `10k_unique_accepted`
- [x] `duplicates_prevented`
- [x] `stale_versions_detected`
- [x] `versionless_ordering_limitation_exposed`

### Observed counts

- Processed: 10803
- Accepted: 10202
- Duplicate: 501
- Stale: 100
- Rejected: 0

**Decision:** Keep explicit source version where available. For versionless sources, checksum guarantees idempotency but changed payload ordering remains a reviewable risk.

## POC-002 — Exact money, booking economics, FX evidence, and rounding

**Status:** PASS  
**Elapsed:** 4.1e-05 seconds

Decimal arithmetic and explicit rounding reproduce the acceptance formula exactly. The conversion result is reproducible only when the rate, effective context, rounding mode, and unrounded value are retained.

### Assertions

- [x] `decimal_exact`
- [x] `float_is_not_exactly_0_3`
- [x] `receivable`
- [x] `deductions`
- [x] `margin`
- [x] `fx_rounded`
- [x] `minor_unit_tolerance`

### Key output

- Decimal `0.1 + 0.2`: `0.3`
- Float `0.1 + 0.2`: `0.30000000000000004`
- Expected receivable: `950.00`
- Expected deductions: `162.50`
- Estimated gross margin: `287.50`
- Converted TRY before rounding: `3512.34560000`
- Converted TRY after rounding: `3512.35`

**Decision:** Exact decimal is mandatory; conversion evidence and rounding policy are part of the financial record.

## POC-003 — Deterministic matching and ambiguity handling

**Status:** PASS  
**Elapsed:** 8e-05 seconds

The matcher can safely automate exact unique cases while refusing ambiguous, out-of-window, free-text-only, and unsupported cross-currency cases. This lowers false-positive risk but leaves a deliberate manual-review workload.

### Assertions

- [x] `unique_exact_match`
- [x] `ambiguous_not_auto_matched`
- [x] `outside_window_unmatched`
- [x] `free_text_alone_unmatched`
- [x] `fx_with_evidence_matched`
- [x] `fx_without_evidence_unmatched`

### Cases

- `unique` → `{'result': 'MATCHED', 'booking_id': 'B-1'}`
- `ambiguous` → `{'result': 'AMBIGUOUS', 'candidate_count': 2}`
- `outside_window` → `{'result': 'UNMATCHED'}`
- `free_text_only` → `{'result': 'UNMATCHED'}`
- `fx_with_evidence` → `{'result': 'MATCHED', 'booking_id': 'B-1'}`
- `fx_without_evidence` → `{'result': 'UNMATCHED'}`

**Decision:** Auto-match only unique deterministic candidates. Free-text similarity may never independently create an MVP match.

## POC-004 — Concurrent allocation conservation

**Status:** PASS  
**Elapsed:** 0.0051 seconds

A transaction that locks the allocation decision can preserve the conservation invariant under concurrent requests. SQLite proves the pattern, not final PostgreSQL locking semantics; the same test must be repeated against the selected production database.

### Assertions

- [x] `never_overallocated`
- [x] `one_committed`
- [x] `one_rejected`
- [x] `atomic_total`

### Concurrent outcome

- Available amount: `10000` minor units
- Final allocated amount: `5000` minor units
- Requests: `[{'booking': 'B-A', 'requested_minor': 7000, 'result': 'REJECTED_OVERALLOCATION'}, {'booking': 'B-B', 'requested_minor': 5000, 'result': 'COMMITTED'}]`

**Decision:** Allocation writes require a database transaction and concurrency test against the selected production database.

## POC-005 — Tenant-scoped relational constraints

**Status:** PASS  
**Elapsed:** 0 seconds

Composite organisation-scoped keys can prevent a class of cross-tenant references at the database boundary. They do not replace application authorisation, query scoping, cache isolation, or export tests.

### Assertions

- [x] `valid_same_org_reference_accepted`
- [x] `cross_org_reference_rejected`
- [x] `org_scoped_query_no_leak`

**Decision:** Use organisation-scoped identifiers and relational constraints where practical, plus application and integration authorisation tests.

## POC-006 — Spreadsheet formula-injection control for CSV export

**Status:** PASS  
**Elapsed:** 1.2e-05 seconds

Formula-leading cells can be neutralised before CSV export. This control requires regression tests and documentation because spreadsheet applications differ and escaped output may affect consumers.

### Assertions

- [x] `all_prefixed`
- [x] `plain_value_unchanged`
- [x] `numeric_text_unchanged`

### Sanitisation examples

- `=SUM(A1:A2)` → `'=SUM(A1:A2)`
- `+cmd|' /C calc'!A0` → `'+cmd|' /C calc'!A0`
- `-10+20` → `'-10+20`
- `@HYPERLINK("https://example.invalid")` → `'@HYPERLINK("https://example.invalid")`
- `	=1+1` → `'	=1+1`
- `=1+1` → `'=1+1`

**Decision:** All spreadsheet-compatible exports require a formula-injection control and regression tests.

## Overall Feasibility Decision

The core financial-control mechanics are technically feasible with conventional relational transactions and deterministic domain code.

The remaining high-impact unknowns are not algorithmic:

- whether target operators need and will adopt the product;
- whether real source exports provide usable identity and date semantics;
- whether the final database implementation preserves allocation safety under its real isolation level;
- and whether real-data processing is legally and contractually approved.

Machine-readable results and relational PoC artefacts are included under `poc/`.
