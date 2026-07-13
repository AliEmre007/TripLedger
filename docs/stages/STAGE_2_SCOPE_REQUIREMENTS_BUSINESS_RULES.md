# Stage 2 — Scope, Requirements, and Business Rules

**Product:** TripLedger  
**Owner:** Ali Emre GÜŞLÜ  
**Date:** 13 July 2026  
**Version:** 0.1  
**Status:** Requirements baseline complete

---

## 1. Stage Purpose

Stage 2 turns the discovery hypothesis into a deliverable MVP with concrete behaviour.

The MVP is:

> A provider-neutral financial control application that imports booking, financial, and supplier records; calculates expected booking economics; performs deterministic matching; exposes reconciliation state; and manages explainable discrepancies with complete audit evidence.

The MVP is not a booking engine, payment gateway, accounting replacement, AI concierge, or broad tourism operating system.

---

## 2. Scope Decisions

### Must-have

- Organisation and role boundary.
- MFA for Administrator and Finance.
- Source-system registration.
- Versioned CSV imports.
- Idempotency and source provenance.
- Canonical bookings and booking items.
- Supplier obligations and credits.
- Immutable financial events and reversal corrections.
- Exact money and explicit exchange rates.
- Expected receivable, deductions, supplier cost, and estimated gross margin.
- Deterministic automatic matching.
- Manual allocation with conservation controls.
- Reconciliation state engine.
- Explainable discrepancy queue.
- Resolution, waiting-external, and accepted-variance workflows.
- Append-only audit timeline.
- Management dashboard and versioned export.
- Security, privacy, observability, performance, backup, restore, and deployment evidence.

### Later

- Production OTA, bank, gateway, and accounting connectors.
- Saved views and notifications.
- Configurable rule administration.
- AI-assisted suggestions.
- Commercial SaaS onboarding.
- Advanced analytics.

---

## 3. Measurable MVP Behaviour

The release must demonstrate:

- At least 80% automatic reconciliation of eligible, unambiguous acceptance records.
- Zero material false-positive reconciliations.
- Detection of every intentionally inserted material discrepancy.
- No duplicate financial effect after unchanged re-import.
- No financial overallocation.
- No cross-organisation access.
- No direct edit or deletion of accepted financial events.
- Full explanation of displayed financial totals.
- Booking-state investigation by a trained user in under 60 seconds.
- 10,000-row import in no more than 5 minutes on the reference environment.
- Backup recovery point no older than 24 hours.
- Restore within four hours for the reference pilot dataset.

---

## 4. Main Business Invariants

1. Every record belongs to one organisation.
2. Imported source identity affects the financial model only once.
3. Financial events are immutable after acceptance.
4. Corrections use reversal and replacement or controlled adjustment.
5. Money always includes currency and exact decimal semantics.
6. Different currencies are never combined without explicit conversion evidence.
7. Missing financial data is unknown, not zero.
8. Auto-match requires one deterministic candidate.
9. Allocations cannot exceed available amount.
10. A reconciled booking has no open material discrepancy.
11. Accepted variance cannot hide a data-integrity or security defect.
12. Audit evidence is append-only.

---

## 5. Output Evidence

- `PRODUCT_REQUIREMENTS.md`
- `FEATURE_BACKLOG.md`
- `USER_STORIES.md`
- `ACCEPTANCE_CRITERIA.md`
- `BUSINESS_RULES.md`
- `OUT_OF_SCOPE.md`
- `TRACEABILITY_MATRIX.md`

---

## 6. Exit Gate

Stage 2 is complete because each P0 feature has:

- A specific user outcome.
- Functional requirements.
- Concrete non-functional behaviour.
- Linked business rules.
- Given/When/Then acceptance scenarios.
- Known edge cases.
- Dependencies.
- A size of S, M, or L.
- Explicit exclusions.

The project may proceed to architecture and detailed design. Direct-user validation may refine terminology, priority, connector order, and tolerances, but it must not silently weaken financial integrity, security, provenance, or audit rules.
