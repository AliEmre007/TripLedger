# Stage 4 - Domain Modeling and Architecture

**Product:** TripLedger  
**Owner:** Ali Emre GÜŞLÜ  
**Date:** 13 July 2026  
**Version:** 0.1  
**Status:** Architecture baseline complete for validation release

## 1. Stage Purpose

Stage 4 translates the requirements, business rules, risks, and feasibility constraints into an implementable system design.

The architecture is intentionally conservative:

- one deployable modular monolith;
- relational transactional persistence;
- CSV/simulator-first imports;
- exact money and explicit FX evidence;
- immutable financial events;
- deterministic matching only;
- OIDC authentication;
- logical tenant isolation with defence in depth.

This avoids premature microservices, event streams, Kubernetes, production connectors, and AI-controlled finance logic.

## 2. Output Evidence

| Evidence | File |
|---|---|
| Architecture overview | `ARCHITECTURE.md` |
| Domain model | `DOMAIN_MODEL.md` |
| ERD and database design | `DATABASE_DESIGN.md` |
| API contract baseline | `API_SPECIFICATION.md` |
| Threat model and security design | `SECURITY_DESIGN.md` |
| Deployment diagram | `DEPLOYMENT_DIAGRAM.md` |
| Architecture Decision Records | `ADRs/` |

## 3. Critical Architecture Decisions

| Decision | ADR |
|---|---|
| Start with a modular monolith | `ADRs/ADR-001-modular-monolith.md` |
| Use a relational transactional database | `ADRs/ADR-002-relational-database.md` |
| Use CSV and simulators before production connectors | `ADRs/ADR-003-csv-first-integration.md` |
| Use exact money and explicit FX evidence | `ADRs/ADR-004-money-currency-model.md` |
| Make accepted financial events immutable | `ADRs/ADR-005-immutable-financial-events.md` |
| Automatic matching must be deterministic and unique | `ADRs/ADR-006-deterministic-matching.md` |
| Use an OIDC identity provider | `ADRs/ADR-007-oidc-identity-provider.md` |
| Use logical tenant isolation for MVP | `ADRs/ADR-008-logical-tenant-isolation.md` |

## 4. Ownership Boundaries

The system is divided into explicit modules:

- Identity and Access
- Organisation
- Import
- Booking
- Supplier
- Finance
- Economics
- Matching
- Reconciliation
- Discrepancy
- Timeline and Audit
- Export
- Jobs and Operations

Each module owns its write model and exposes application services. Controllers, background jobs, and UI code must not bypass those services for financial or security decisions.

## 5. Data Ownership

Every business record belongs to exactly one organisation. Accepted financial state is represented by source records, immutable financial events, supplier obligations, calculation snapshots, matches, reconciliation results, discrepancies, and audit events.

The database design enforces:

- source idempotency;
- same-organisation references;
- exact money storage;
- allocation conservation;
- append-only audit and financial evidence;
- current versus historical reconciliation results.

## 6. Authentication and Authorisation Boundary

TripLedger uses OIDC for authentication and MFA. The application remains responsible for authorisation.

Every protected operation receives an actor context with:

- user id;
- organisation id;
- role;
- MFA state;
- correlation id.

Role checks and organisation ownership checks are both mandatory. Administrator and Finance financial workflows require MFA.

## 7. Main Implementation Gates

Before implementation:

- review Stage 4 artifacts and ADRs;
- turn database design into versioned migrations;
- create acceptance fixtures for import, money, matching, reconciliation, tenant isolation, and audit;
- map API error codes to business-rule codes.

Before real-data pilot:

- complete direct-user validation gates from Stage 1;
- complete legal, privacy, hosting, retention, and data-flow review;
- repeat allocation concurrency test on the selected production database;
- pass cross-role and cross-organisation tests;
- pass audit completeness tests;
- pass backup/restore rehearsal;
- pass dependency, secret, and container/image scans.

## 8. Exit Gate Assessment

Stage 4 is complete for the validation-release boundary because:

- critical workflows are explicit;
- domain entities and ownership boundaries are defined;
- module responsibilities and dependencies are documented;
- database invariants and constraints are designed;
- API contracts, error model, authentication, and authorisation boundaries are defined;
- material architectural trade-offs are recorded in ADRs;
- threat model, deployment shape, and operational gates are documented.

Open questions remain behind documented validation, legal, security, and pilot gates. None justify premature microservices, event streams, or Kubernetes.
