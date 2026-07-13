# STAGE_3_RISK_FEASIBILITY_CONSTRAINTS.md

## TripLedger Stage 3 Summary

**Owner:** Ali Emre GÜŞLÜ  
**Date:** 13 July 2026  
**Version:** 0.1  
**Exit decision:** Proceed conditionally to architecture and detailed design

---

## 1. Critical Findings

1. The core reconciliation mechanics are technically feasible.
2. Product demand and adoption are not yet validated directly.
3. The full Stage 2 P0 backlog is infeasible inside the 12-week capacity boundary.
4. Versionless source data can be deduplicated by checksum but cannot be reliably ordered as stale/new.
5. Automatic matching must remain deterministic and refuse ambiguity.
6. Allocation safety requires a database transaction and a production-database concurrency test.
7. Tenant isolation requires defence in depth: application authorisation, organisation-scoped queries, relational constraints, cache/export isolation, and tests.
8. Real personal data cannot enter a pilot before legal, privacy, hosting, retention, and security gates are closed.
9. Raw payment-card data and production payment integrations remain outside the first release.
10. Low-reversibility domain decisions require ADRs before implementation.

---

## 2. Evidence Produced

- `RISK_REGISTER.md`
- `PROOF_OF_CONCEPT_RESULTS.md`
- `THREAT_ASSUMPTIONS.md`
- `CONSTRAINTS_AND_TRADEOFFS.md`
- `DECISION_REVERSIBILITY.md`
- `FEASIBILITY_ASSESSMENT.md`
- `FIRST_RELEASE_RISK_ADJUSTMENT.md`
- Executed PoC results and artefacts under `poc/`

---

## 3. Proof-of-Concept Result

Six targeted PoCs passed:

- Import idempotency and stale-version handling.
- Exact decimal, booking economics, FX and rounding.
- Deterministic matching and ambiguity refusal.
- Concurrent allocation conservation.
- Tenant-scoped relational references.
- CSV formula-injection control.

The PoCs validate patterns, not the final production stack. Database concurrency, identity integration, full persistence performance, and real source compatibility remain Stage 4/implementation gates.

---

## 4. Delivery Decision

The Stage 2 P0 backlog is estimated at:

- 160 hours minimum.
- 240 hours likely.
- 320 hours maximum.

The 12-week capacity is 120–144 hours. Therefore:

- Build a risk-adjusted validation release first.
- Plan approximately 12–14 weeks for that release.
- Treat the full MVP as approximately 20–24 focused weeks until velocity evidence improves the estimate.

---

## 5. Exit Gate

High-impact unknowns have been handled as follows:

### Tested

- Money, FX, idempotency, matching ambiguity, allocation safety pattern, tenant-reference constraints, and CSV export injection.

### Mitigated

- False-positive matching.
- Duplicate effects.
- provenance loss.
- unnecessary personal-data collection.
- delivery over-scope through release split.

### Removed from first release

- Production APIs.
- raw payment data.
- payment execution.
- AI-controlled matching.
- broad dashboard/export workflow.
- complex manual allocations.

### Deliberately open behind gates

- Product adoption.
- legal basis and data-processing roles.
- hosting/cross-border assessment.
- real source schema compatibility.
- production-database concurrency.
- restore rehearsal.
- independent security verification.

The project may proceed to Stage 4 only within the validation-release boundary and with ADRs for expensive-to-reverse decisions.
