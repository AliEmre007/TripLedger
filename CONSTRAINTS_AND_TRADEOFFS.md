# CONSTRAINTS_AND_TRADEOFFS.md

## TripLedger Constraints and Trade-off Notes

**Version:** 0.1  
**Date:** 13 July 2026

---

## 1. Delivery Feasibility

Stage 2 contains:

- 4 small feature slices,
- 18 medium slices,
- 10 large slices.

Using the declared size ranges:

- Minimum estimate: `4×2 + 18×4 + 10×8 = 160 hours`.
- Likely estimate: `4×3 + 18×6 + 10×12 = 240 hours`.
- Maximum estimate: `4×4 + 18×8 + 10×16 = 320 hours`.

Available 12-week capacity is approximately:

- 120 hours at 10 hours/week.
- 144 hours at 12 hours/week.

Therefore, the full P0 backlog cannot credibly fit the original 12-week window, even at the optimistic minimum. This is not a productivity problem; it is a scope-capacity mismatch.

### Decision

Split delivery into:

1. **Validation Release:** approximately 12–14 weeks, proving import → economics → exact match → discrepancy → explanation.
2. **Full MVP:** estimated 20–24 focused weeks at the current capacity, subject to discovery evidence and actual velocity.

No quality, security, or financial-invariant work may be removed merely to preserve an arbitrary date.

---

## 2. First Validation Release Boundary

Included:

- One configured organisation.
- Identity-provider authentication with Finance, Operations, and Manager roles; no self-service user-management UI.
- Seeded/configured source systems.
- Versioned CSV imports for bookings, supplier obligations, and financial events.
- Source provenance and idempotency.
- Immutable financial events and reversals.
- Exact money and one explicit base-currency conversion path.
- Expected booking economics.
- Unique one-to-one deterministic matching.
- `NOT_READY`, `PARTIALLY_RECONCILED`, `RECONCILED`, and `DISCREPANT`.
- Basic discrepancy list and evidence detail.
- Booking financial timeline.
- Structured errors, logs, health checks, backup/restore evidence.
- Automated acceptance dataset.

Deferred to the next release:

- User invitation/deactivation UI.
- Configurable tolerances UI.
- One-to-many and many-to-one manual allocation.
- Accepted-variance workflow.
- Assignment/comments/waiting-external workflow.
- Management dashboard.
- Accounting export.
- 100k-booking performance claim.
- Production connectors.
- Notifications.
- Advanced multi-currency presentation.

This validation release proves the domain while preserving a path to the full Stage 2 requirements.

---

## 3. Major Constraints

| Constraint | Effect | Engineering response |
|---|---|---|
| Solo engineer, 10–12 focused hours/week | Limited parallel work and review capacity | Small vertical slices, automation, written decisions, no broad integrations |
| Direct-user validation incomplete | Requirements may change | Stable financial invariants; configurable terminology; delay provider-specific design |
| No guaranteed provider API access | Production connector risk | CSV/simulator-first adapter boundary |
| Synthetic/anonymised data initially | Real formats may expose gaps | Preserve source payload and add real anonymised fixtures before pilot |
| Financial correctness and auditability | Lower tolerance for convenience shortcuts | Immutable events, exact money, deterministic matching, explicit unknown states |
| Privacy and legal uncertainty | Real-data pilot blocked | Minimise data and require legal/data-flow gate |
| Low infrastructure budget | Limits managed services and redundancy | Single-region pilot with tested backup; avoid complex distributed architecture |
| Portfolio and potential commercial objective | Needs credible evidence, not only code | Traceability, threat model, PoCs, acceptance tests, runbook, deployed demo |

---

## 4. Trade-offs

### 4.1 CSV first versus production APIs

**Chosen:** CSV first.

**Benefit:** Reproducible tests, lower dependency risk, faster domain validation.  
**Cost:** Not real-time; users perform exports; schema errors are visible later.  
**Reversibility:** High, provided adapters map to a canonical contract.

### 4.2 Deterministic matching versus probabilistic matching

**Chosen:** Deterministic unique match only.

**Benefit:** Explainable and low false-positive risk.  
**Cost:** Lower automatic coverage and more manual review.  
**Reversibility:** High; suggestion models can be added later without weakening automatic-match rules.

### 4.3 Immutable events versus editable finance rows

**Chosen:** Immutable accepted events with reversal/replacement.

**Benefit:** Auditability, reproducibility, safe recalculation.  
**Cost:** More domain complexity and user-interface explanation.  
**Reversibility:** Low; changing later would require migration and audit redesign.

### 4.4 One deployable system versus microservices

**Chosen constraint for Stage 4:** Prefer one deployable modular application unless a measured requirement proves otherwise.

**Benefit:** Lower operational burden and easier transactions for a solo team.  
**Cost:** Module boundaries require discipline; independent scaling is limited.  
**Reversibility:** Medium if modules and contracts are explicit.

### 4.5 Relational database versus distributed/eventual storage

**Chosen constraint for Stage 4:** Use a relational transactional model for accepted financial state unless a PoC disproves feasibility.

**Benefit:** Constraints, transactions, consistent allocation, mature backup.  
**Cost:** Some import and analytical workloads need careful indexing/batching.  
**Reversibility:** Medium-to-low after production data accumulates.

### 4.6 Logical tenant isolation versus physical database per tenant

**Chosen for MVP:** Organisation-scoped logical isolation with relational constraints and tests.

**Benefit:** Lower cost and operational complexity.  
**Cost:** A query-scoping defect can have high impact.  
**Reversibility:** Medium; later physical separation requires migration tooling.

### 4.7 Synchronous versus asynchronous import

**Chosen:** Small validation may be synchronous; accepted production-like files use visible jobs.

**Benefit:** Simpler user flow for small files while preserving bounded processing.  
**Cost:** Two execution paths can diverge.  
**Control:** Both paths call the same import domain service and idempotency rules.

### 4.8 Keep raw source payload versus minimise retention

**Chosen:** Preserve a checksum and structured provenance; retain raw file only for an approved limited period.

**Benefit:** Explainability without indefinite source-file exposure.  
**Cost:** Older disputes may need external evidence after deletion.  
**Reversibility:** Low once raw data is deleted; retention must be approved before pilot.

### 4.9 Build versus buy identity

**Chosen constraint:** Use a mature identity provider; do not implement password/MFA storage.

**Benefit:** Reduces authentication implementation risk.  
**Cost:** Configuration, upgrade, and provider availability remain risks.  
**Reversibility:** Medium if OIDC boundaries are kept standard.

---

## 5. Cost Guardrails

- Development and acceptance must work without paid provider APIs.
- No Kubernetes, Kafka, service mesh, or multi-region infrastructure in the first release.
- Use synthetic or anonymised datasets.
- Store only required source files and exports.
- Prefer one production-like environment plus reproducible local environment.
- Add a managed service only when it reduces a measured operational risk more than it increases cost or lock-in.

---

## 6. Constraint Change Rule

A constraint may change only with:

1. evidence that the current constraint blocks an acceptance criterion;
2. impact on security, data, cost, operations, and delivery;
3. reversibility classification;
4. updated risk register;
5. an ADR in Stage 4;
6. removal or rescheduling of equivalent scope when capacity increases are not available.
