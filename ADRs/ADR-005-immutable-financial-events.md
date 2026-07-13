# ADR-005: Make Accepted Financial Events Immutable

**Status:** Accepted  
**Date:** 13 July 2026

## Context

TripLedger must preserve auditability and deterministic replay. Imported financial events may later be corrected, but destructive edits would hide evidence and invalidate prior reconciliation results.

## Decision

Accepted financial events cannot be edited or hard-deleted through application functions. Corrections use linked reversal and replacement events, or controlled manual adjustments with authority, reason, and audit evidence.

## Alternatives

- Allow Finance users to edit accepted rows.
- Keep only latest financial state.
- Event-source the entire system from day one.

## Consequences

Benefits:

- audit history remains explainable;
- recalculation can reconstruct prior results;
- duplicate and correction behavior is explicit.

Costs:

- user workflows must explain reversals;
- queries must account for net effect;
- storage grows with correction history.

## Enforcement

- no update/delete API for accepted financial events;
- restricted database grants or triggers for immutable tables;
- reversal chain tests;
- audit completeness tests.
