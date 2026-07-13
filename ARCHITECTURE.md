# TripLedger Architecture

**Stage:** 4 - Domain Modeling and Architecture  
**Product:** TripLedger  
**Date:** 13 July 2026  
**Version:** 0.1  
**Status:** Architecture baseline for validation release  
**Inputs:** `PROJECT_BRIEF.md`, `PRODUCT_REQUIREMENTS.md`, `BUSINESS_RULES.md`, `ACCEPTANCE_CRITERIA.md`, `RISK_REGISTER.md`, `CONSTRAINTS_AND_TRADEOFFS.md`, `THREAT_ASSUMPTIONS.md`, `PROOF_OF_CONCEPT_RESULTS.md`

## 1. Architecture Purpose

TripLedger is a provider-neutral financial control layer for multi-supplier tourism operators. The architecture must make booking-level financial state explainable, deterministic, auditable, and secure without introducing distributed-system complexity before evidence requires it.

The validation release proves this path:

```text
Versioned CSV source files
    -> canonical booking, supplier, and financial records
    -> expected booking economics
    -> unique deterministic match
    -> reconciliation status or explainable discrepancy
    -> traceable booking timeline
```

The design deliberately excludes production provider connectors, payment execution, raw card data, statutory accounting, AI-controlled matching, public onboarding, and microservices.

## 2. Selected Architecture

TripLedger will start as a **modular monolith** backed by a **relational transactional database**.

### Why this fits now

| Requirement or risk | Architectural response |
|---|---|
| Financial operations need atomicity | Single transactional database boundary |
| Solo-engineer delivery constraint | One deployable, one runtime, simple local setup |
| Tenant isolation risk | Organisation-scoped data model, server-side policy, database constraints, tests |
| Duplicate import and allocation risk | Unique constraints, idempotency keys, row-level transactions |
| Explainability and audit | Append-only source, event, calculation, match, discrepancy, and audit records |
| Direct-user validation incomplete | Provider-neutral adapter boundary and canonical model |
| Scale target is pilot-level | Avoid Kafka, Kubernetes, microservices, and multi-region overhead |

Services may be extracted later only when real evidence shows independent team ownership, release cadence, scaling, or reliability needs.

## 3. Logical Modules

Modules are code ownership boundaries inside one application. They may share the database, but they must not bypass public module services for business operations.

| Module | Owns | Responsibilities | May depend on |
|---|---|---|---|
| Identity and Access | Users, roles, sessions, MFA claims, authorisation policy | Authenticate requests through OIDC, enforce organisation and role context, audit protected denials | Audit |
| Organisation | Organisations, settings, currencies, tolerances, source configuration | Tenant settings, base currency, materiality threshold, source-system registry | Identity, Audit |
| Import | Import batches, source records, row results, template validation | Validate CSV files, preserve provenance, apply idempotency, create canonical write commands | Organisation, Booking, Supplier, Finance, Audit, Jobs |
| Booking | Bookings, booking items, lifecycle state | Canonical booking identity, service dates, operational fields, lifecycle transitions | Organisation, Audit |
| Supplier | Suppliers, supplier obligations, credits, supplier payment allocation references | Supplier cost state and contribution to active supplier cost | Booking, Finance, Audit |
| Finance | Financial events, reversals, exchange rates, adjustments | Immutable money records, exact decimal semantics, reversal chains, FX evidence | Organisation, Booking, Audit |
| Economics | Calculation snapshots and explanation components | Expected receivable, deductions, supplier cost, margin, unknown-state handling | Booking, Supplier, Finance, Audit |
| Matching | Matches and allocation lines | Deterministic auto-match, manual match/unmatch, allocation conservation | Finance, Economics, Audit |
| Reconciliation | Reconciliation runs, results, status | State engine, repeatable recalculation, reopen logic | Economics, Matching, Discrepancy, Audit |
| Discrepancy | Discrepancies, evidence links, comments, workflow state | Material exception creation, deduplication, lifecycle, resolution evidence | Reconciliation, Finance, Audit |
| Timeline and Audit | Audit events, timeline projections | Append-only evidence and booking timeline query model | All modules emit events to it |
| Export | Export batches, generated package metadata | Versioned CSV export and formula-injection neutralisation | Reconciliation, Discrepancy, Audit |
| Jobs and Operations | Background job records, retries, health, metrics | Bounded retries, failure visibility, correlation IDs, readiness checks | Import, Reconciliation, Export, Audit |

## 4. Dependency Rules

1. Every module operation receives an authenticated `ActorContext` containing actor id, organisation id, role, MFA state, and correlation id.
2. Cross-organisation references are forbidden at service and database levels.
3. UI handlers and API controllers may call application services only; they must not contain financial rules.
4. Import adapters produce canonical commands; they do not write canonical tables directly.
5. Economics, matching, and reconciliation are deterministic domain services with versioned rules.
6. Financial events, audit events, accepted source records, matches, adjustments, and discrepancy resolutions are never hard-deleted.
7. Background jobs use the same application services as synchronous paths.
8. Export paths must reuse authorisation and tenant filters used by read APIs.

## 5. Request and Job Flows

### 5.1 CSV import

```text
Upload request
 -> authorise source/import permission
 -> create import batch
 -> validate file metadata, size, template version
 -> parse rows with streaming CSV parser
 -> validate row types, currency, source identity, references
 -> classify each row as accepted, duplicate, rejected, or warning
 -> execute canonical writes in transactions
 -> write source provenance and audit events
 -> mark batch completed, completed_with_errors, or failed
```

File-level security failures fail the whole batch before domain writes. Row-level failures do not silently discard valid rows.

### 5.2 Reconciliation

```text
Booking or import change
 -> mark affected booking for recalculation
 -> load immutable accepted inputs and current settings
 -> calculate economics snapshot with rule version
 -> run deterministic match candidates
 -> create only unique valid automatic matches
 -> evaluate reconciliation status
 -> create, update, resolve, or reopen discrepancies
 -> append audit and timeline records
```

Unchanged records, settings, exchange rates, and rule versions must produce equivalent results on rerun.

### 5.3 Manual match or unmatch

```text
Finance/Admin request
 -> require MFA and authority
 -> validate same organisation
 -> lock affected financial event allocation balance
 -> reject if allocation would exceed available amount
 -> persist match or inactive prior match state
 -> recalculate affected bookings
 -> audit actor, reason, records, and allocation amounts
```

## 6. Data Ownership

| Data | Owning module | Notes |
|---|---|---|
| Organisation settings | Organisation | Base currency, tolerances, materiality, rounding policy |
| Users and role assignments | Identity and Access | Historical actor identity is preserved after deactivation |
| Source systems and import batches | Import | Source identity is the idempotency boundary |
| Canonical bookings and items | Booking | Imported source versions remain traceable |
| Supplier obligations and credits | Supplier | Unlinked obligations are visible but excluded from economics |
| Financial events and reversals | Finance | Immutable after acceptance |
| Exchange rates | Finance | Required for every base-currency conversion |
| Calculation snapshots | Economics | Store rule version and component references |
| Matches and allocations | Matching | Allocation conservation is enforced transactionally |
| Reconciliation results | Reconciliation | Status is derived, versioned, and auditable |
| Discrepancies | Discrepancy | One active discrepancy per booking/type/component/cause |
| Audit events | Timeline and Audit | Append-only normal application path |
| Exports | Export | Include filter, version, row count, checksum, actor |

## 7. Data Model Summary

The database design is detailed in `DATABASE_DESIGN.md`. Core invariants:

- Every business table includes `organisation_id`.
- Cross-entity references include `organisation_id` and use composite foreign keys where practical.
- Source identity is unique by `organisation_id + source_system_id + record_type + external_record_id + source_version`.
- Money is stored as exact decimal amount plus ISO 4217 currency.
- Accepted financial events are immutable and corrected by reversal/replacement or controlled adjustment.
- Matches use allocation lines and cannot allocate more than available event amount.
- Calculation and reconciliation snapshots store rule version and component references.
- Audit is append-only through application permissions.

## 8. API Design Summary

The API is JSON over HTTPS with version prefix `/api/v1`. API contracts are detailed in `API_SPECIFICATION.md`.

Common rules:

- All protected requests require authentication.
- All protected requests derive organisation from the authenticated context or an authorised explicit selector.
- Server-side authorisation is mandatory for every endpoint.
- All errors return stable error code and correlation id.
- Financial write endpoints require Finance or Administrator authority and MFA where specified.
- Public health endpoints expose no tenant or financial data.

## 9. Authentication and Authorisation

TripLedger uses a mature OIDC identity provider rather than storing passwords or MFA secrets in the application.

Roles:

- `ADMINISTRATOR`
- `FINANCE`
- `OPERATIONS`
- `READ_ONLY_MANAGER`

Authorisation is deny-by-default. Role checks are not sufficient by themselves; every object access is also checked against `organisation_id`. Administrator and Finance financial functions require an MFA claim accepted by the application policy.

## 10. Security and Privacy Design

The security model is detailed in `SECURITY_DESIGN.md`.

Primary controls:

- encrypted transport in production-like environments;
- no raw card data, passport scans, medical records, or bank credentials;
- minimal customer data, external customer reference preferred;
- organisation-scoped queries and composite references;
- CSV size/type/content limits and non-executable storage;
- formula-injection neutralisation on spreadsheet exports;
- structured allow-list logging with no source payloads or secrets;
- encrypted backups and restore evidence;
- dependency, secret, and container scans in release pipeline.

## 11. Deployment Shape

The deployment design is detailed in `DEPLOYMENT_DIAGRAM.md`.

Validation release runtime:

- browser client;
- one TripLedger application container or process;
- relational database;
- object/file storage for uploads and generated exports;
- OIDC identity provider;
- structured logs and metrics;
- backup storage.

No Kubernetes, Kafka, service mesh, multi-region active-active deployment, or microservices are required for the validation release.

## 12. Material Trade-offs

| Decision | Benefit | Cost | ADR |
|---|---|---|---|
| Modular monolith | Simple deploy/debug/transactions | Requires module discipline | `ADRs/ADR-001-modular-monolith.md` |
| Relational PostgreSQL-style model | Constraints, transactions, backup maturity | SQL and locking details matter | `ADRs/ADR-002-relational-database.md` |
| CSV/simulator-first imports | Reproducible validation, no provider dependency | Not real-time | `ADRs/ADR-003-csv-first-integration.md` |
| Exact money and FX evidence | Financial correctness and reproducibility | More schema/rule complexity | `ADRs/ADR-004-money-currency-model.md` |
| Immutable financial events | Auditability and deterministic replay | Corrections are more explicit | `ADRs/ADR-005-immutable-financial-events.md` |
| Deterministic matching only | Avoids material false positives | More manual review | `ADRs/ADR-006-deterministic-matching.md` |
| OIDC identity provider | Reduces password/MFA risk | External configuration dependency | `ADRs/ADR-007-oidc-identity-provider.md` |
| Logical tenant isolation | Lower cost and simpler pilot ops | Query defects have high impact | `ADRs/ADR-008-logical-tenant-isolation.md` |

## 13. Implementation Gates

Before implementation starts:

- Stage 4 documents and ADRs are reviewed.
- Database migrations and test fixtures map to business rules.
- API error codes reuse `BUSINESS_RULES.md`.
- Acceptance fixtures exist for import, money, matching, and tenant isolation.

Before real-data pilot:

- five user interviews and real workflow samples are reviewed;
- legal/data-flow/retention/hosting gates are closed;
- production database allocation concurrency test passes;
- cross-role and cross-organisation tests pass;
- backup restore rehearsal passes;
- audit completeness test passes for all material financial actions;
- security scan policy passes.

## 14. Stage 4 Exit Assessment

This architecture makes critical workflows, data ownership, authentication boundaries, and trade-offs explicit enough for a developer or agent to implement without inventing the system design. Open questions remain behind documented gates and do not require microservices, event streams, or Kubernetes for the validation release.
