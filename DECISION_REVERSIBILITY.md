# DECISION_REVERSIBILITY.md

## TripLedger Decision Reversibility Map

**Version:** 0.1  
**Date:** 13 July 2026

A reversible decision can be changed with local code/configuration and limited data migration. An expensive-to-reverse decision shapes persisted data, security boundaries, customer contracts, or operational procedures.

| Decision | Current direction | Reversibility | Why | Required action before commitment |
|---|---|---|---|---|
| CSV versus first production API | CSV first | High | Adapter can be replaced behind canonical contract | Validate canonical fixtures |
| UI framework | Not selected | High | Mostly presentation concern if API/use cases remain separate | Avoid domain logic in UI |
| Application framework | Not selected | Medium | Code rewrite cost, but domain concepts transferable | Stage 4 ADR and thin framework boundary |
| One deployable versus microservices | One modular deployable preferred | Medium | Deployment topology can change if modules are explicit | Define module ownership and prohibited dependencies |
| Relational database product | Not selected; relational required | Medium | SQL/migration/locking details create migration cost | Repeat concurrency PoC on candidate DB |
| Organisation-scoped logical tenancy | Preferred for MVP | Medium | Physical separation later requires data migration | Composite keys, query tests, tenant context |
| Canonical source identity | Composite provenance identity | Low | All imports and corrections depend on it | Validate with real exports before schema freeze |
| Exact decimal and currency model | Mandatory | Low | Persisted money migration is dangerous | Decide precision and rounding ADR |
| Immutable financial events | Mandatory | Low | Editing model would invalidate audit history | Design reversal chain before APIs |
| Match/allocation representation | Explicit allocation records | Low | Reconciliation history depends on it | Concurrency and conservation tests |
| Calculation/rule versioning | Mandatory | Low | Reproducing past results depends on it | Include version in persisted result |
| Raw source retention period | Not yet approved | Low after data is deleted | Deleted evidence cannot be reconstructed | Legal/business approval before pilot |
| Identity provider | OIDC provider, product not fixed | Medium | Claims, users, MFA, and operations create migration work | Use standard OIDC claims and config-as-code |
| Hosting region/provider | Not selected | Medium-to-low after real data | Legal, backup, networking, and contract effects | Data-flow and transfer review |
| Payment data boundary | No raw card data | Low and deliberate | Expanding scope creates major compliance burden | Formal PCI scope assessment before change |
| Direct-user segment | Inbound tour operators/DMCs | High before contracts, low after product specialisation | Terminology and connectors become segment-specific | Complete Stage 1 interviews |
| Discrepancy taxonomy | Initial catalogue | Medium | Persisted reporting/history uses categories | Allow versioning and `OTHER` with controlled review |
| Base currency per organisation | Preferred | Medium | Historical reporting and FX records depend on it | Validate accountant/manager need |
| Audit storage design | Append-only required; mechanism not selected | Low concept, medium mechanism | Trust and investigations depend on completeness | Threat model and DB privilege ADR |

## Decision Policy

- Low-reversibility decisions require an ADR, migration/rollback analysis, and explicit acceptance evidence.
- High-reversibility decisions should be made quickly with a time-boxed experiment.
- Familiarity with a technology is not evidence that a low-reversibility decision is safe.
