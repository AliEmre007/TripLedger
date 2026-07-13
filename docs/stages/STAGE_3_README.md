# TripLedger Stage 3 — Risk, Feasibility, and Constraints

This package contains the Stage 3 engineering evidence.

## Documents

- `STAGE_3_RISK_FEASIBILITY_CONSTRAINTS.md` — concise stage decision and exit assessment.
- `RISK_REGISTER.md` — ranked product, technical, security, data, operational, integration, delivery, and legal risks.
- `PROOF_OF_CONCEPT_RESULTS.md` — actual executed PoC results.
- `THREAT_ASSUMPTIONS.md` — data classification, assets, trust boundaries, actors, abuse cases, and security gates.
- `CONSTRAINTS_AND_TRADEOFFS.md` — delivery constraints and explicit technical trade-offs.
- `DECISION_REVERSIBILITY.md` — reversible versus expensive-to-reverse decisions.
- `FEASIBILITY_ASSESSMENT.md` — product, technical, security, data, operational, delivery, and legal feasibility.
- `FIRST_RELEASE_RISK_ADJUSTMENT.md` — risk-adjusted release split caused by the Stage 2 scope/capacity mismatch.
- `poc/results.json` — machine-readable PoC output.
- `poc/show_results.py` — dependency-free result viewer.
- `poc/*.sqlite` — temporary relational PoC artefacts.

## Main Decision

Proceed to architecture for a constrained validation release. Do not treat the full Stage 2 backlog as a 12-week delivery, and do not introduce production connectors or real personal data before the identified gates are closed.
