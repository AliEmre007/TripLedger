# FEASIBILITY_ASSESSMENT.md

## TripLedger Feasibility Assessment

**Version:** 0.1  
**Date:** 13 July 2026  
**Decision:** Conditionally feasible

---

## 1. Product Feasibility

**Status:** Unproven.

Public discovery supports fragmented systems and reconciliation problems, but direct interviews and real workflow evidence remain incomplete. Existing products already solve substantial parts of the problem. Product feasibility therefore depends on proving the provider-neutral, multi-supplier control-layer gap.

**Gate before commercial pilot:**

- Five representative interviews.
- Two real/anonymised end-to-end cases.
- Ten real exception examples.
- Existing-system report comparison.
- Quantified baseline time or financial effect.
- Adoption conditions from a decision-maker.

---

## 2. Technical Feasibility

**Status:** Feasible for the constrained MVP.

PoCs demonstrate that:

- explicit source identity can prevent duplicate effects;
- exact decimal arithmetic can reproduce booking economics;
- deterministic matching can refuse ambiguity;
- transactional allocation can preserve available balance;
- relational organisation keys can reject cross-tenant references;
- CSV export can neutralise formula-leading content.

Open technical gates:

- repeat allocation test against selected production database and isolation level;
- validate real provider export formats;
- prove deterministic replay in the implementation language;
- benchmark full persistence, reconciliation, and query path;
- prove identity-provider and tenant-isolation integration.

---

## 3. Security Feasibility

**Status:** Feasible with disciplined scope.

The absence of raw card data, anonymous uploads, production connectors, and public self-service significantly reduces first-release exposure. High-impact risks remain around tenant isolation, authorisation, import processing, audit completeness, secrets, exports, and backups.

Security is feasible only when controls are built into each vertical slice rather than postponed to a final hardening sprint.

---

## 4. Data Feasibility

**Status:** Conditional.

The canonical model can represent the current acceptance scenarios. It has not yet been validated against enough real source reports. Versionless records remain intrinsically unable to prove ordering from checksum alone.

Required approach:

- preserve provenance and raw/normalised distinction;
- support unmatched and unknown states;
- reject silent assumptions;
- include real anonymised fixtures before schema freeze.

---

## 5. Operational Feasibility

**Status:** Feasible for a pilot, not yet proven.

A single-region, single-deployable pilot is operationally reasonable for a solo engineer. Backup, restore, job failure handling, and observability must be demonstrated before real data.

---

## 6. Delivery Feasibility

**Status:** Full Stage 2 P0 scope is not feasible in 12 weeks.

Estimated P0 effort is 160–320 hours, likely approximately 240 hours. Available capacity is 120–144 hours. The risk is removed by splitting a validation release from the full MVP.

- Validation release: approximately 12–14 weeks.
- Full MVP: approximately 20–24 focused weeks, revised after measured velocity.

---

## 7. Legal and Contractual Feasibility

**Status:** Unknown for real personal data.

Synthetic/anonymised development is feasible. A real-data pilot requires:

- controller/processor and lawful-basis assessment;
- data-processing terms;
- retention/deletion schedule;
- hosting, subprocessor, and cross-border review;
- third-party source-data contractual rights;
- payment-data scope confirmation.

No document in this package is legal advice.

---

## 8. Overall Decision

Proceed to Stage 4 architecture and detailed design under these conditions:

1. Design the 12–14 week validation release, not the entire full-MVP backlog as one delivery.
2. Use a relational transactional model and one modular deployable as default hypotheses.
3. Preserve exact money, immutable events, provenance, tenant context, and rule versioning as hard constraints.
4. Do not implement production provider connectors or raw payment-data handling.
5. Create ADRs for every low-reversibility decision.
6. Keep real-data pilot approval behind the legal, privacy, security, and restore gates.
