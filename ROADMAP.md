# TripLedger Roadmap

**Stage:** 5 - Delivery Planning  
**Date:** 13 July 2026  
**Version:** 0.1  
**Planning horizon:** Validation release, 12-14 weeks

## 1. Roadmap Principle

The roadmap moves from foundations to the core reconciliation workflow, then to reliability and demonstration evidence.

It is not organised by technical layers. Each milestone must leave the system more demonstrable than before.

## 2. Roadmap Summary

| Milestone | Theme | Outcome | Target effort |
|---|---|---|---:|
| M0 | Executable requirements | Acceptance fixtures and rule examples make financial behavior testable before implementation expands | 6h |
| M1 | Security and provenance foundation | Every future record has organisation, actor, role, source, error, and audit context | 18h |
| M2 | Booking and supplier import | Bookings and supplier costs enter canonical state through idempotent imports | 24h |
| M3 | Financial evidence and money model | Payments, fees, refunds, reversals, and FX evidence enter safely | 18h |
| M4 | Expected economics | Booking-level receivable, deductions, supplier cost, and margin are calculated and explained | 12h |
| M5 | Matching and reconciliation | Unique exact matches reconcile; ambiguous or mismatched records stay open | 24h |
| M6 | Timeline and discrepancy evidence | Users can inspect why a booking reconciled or became discrepant | 10h |
| M7 | Operational proof | Errors, logs, metrics, health, deployment, backup, restore, and final demo are evidenced | 27h |

Total risk-adjusted target: **139h**.

## 3. Milestone Outcomes

### M0 - Executable Requirements

Value:

- prevents architecture and code from drifting away from business rules.

Evidence:

- synthetic CSV fixtures;
- executable examples for valid and invalid import rows;
- expected calculation cases;
- duplicate, stale, ambiguous, and cross-org examples.

### M1 - Security and Provenance Foundation

Value:

- protects tenant isolation and role boundaries before financial records exist.

Evidence:

- authenticated actor context;
- organisation-scoped test data;
- server-side role enforcement;
- MFA policy stub/integration;
- source registry;
- stable error model and correlation ids.

### M2 - Booking and Supplier Import

Value:

- operations and finance can see canonical bookings and supplier costs without manual reconstruction.

Evidence:

- import batch lifecycle;
- row-level results;
- booking CSV import;
- booking detail;
- supplier obligation import;
- idempotency and stale-version tests.

### M3 - Financial Evidence and Money Model

Value:

- financial records enter the system without destructive editing, duplicate effects, or hidden conversion.

Evidence:

- financial-event import;
- immutable event tests;
- reversal/replacement path;
- exact decimal money and currency precision tests;
- exchange-rate evidence.

### M4 - Expected Economics

Value:

- Finance can understand expected booking outcome before matching actual records.

Evidence:

- contracted gross sale;
- expected customer receivable;
- expected deductions;
- active supplier cost;
- estimated gross margin;
- unknown-state handling;
- calculation explanation.

### M5 - Matching and Reconciliation

Value:

- routine exact records reconcile automatically; unsafe cases become visible.

Evidence:

- deterministic one-to-one matcher;
- ambiguous match refusal;
- basic reconciliation state engine;
- material discrepancy generation;
- repeatable rerun tests.

### M6 - Timeline and Discrepancy Evidence

Value:

- a reviewer can reconstruct the booking's financial history and exception reason.

Evidence:

- booking timeline;
- discrepancy detail;
- audit events for imports, calculations, matches, reversals, and denials.

### M7 - Operational Proof

Value:

- the validation release is diagnosable, recoverable, and demonstrable.

Evidence:

- structured logs;
- metrics;
- health/readiness checks;
- failed-job visibility;
- deployment notes;
- backup/restore rehearsal;
- final end-to-end demonstration dataset.

## 4. Deferred Roadmap Items

Deferred from validation release:

- user invitation/deactivation interface;
- tolerance administration UI;
- one-to-many and many-to-one manual allocation;
- accepted variance closure;
- full discrepancy assignment/comment/waiting-external workflow;
- management dashboard;
- accounting export;
- large-scale performance suite;
- production connectors;
- notifications;
- AI features.

These remain future MVP or post-MVP items unless risk or user evidence changes the plan.
