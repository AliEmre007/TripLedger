# ADR-008: Use Logical Tenant Isolation for MVP

**Status:** Accepted  
**Date:** 13 July 2026

## Context

TripLedger starts as a low-cost pilot and portfolio-ready system. Separate physical databases per tenant would add operational complexity before commercial validation. Cross-organisation leakage remains a high-impact risk.

## Decision

Use logical tenant isolation with `organisation_id` on business records, organisation-scoped application authorisation, composite database references where practical, scoped cache/export keys, and automated cross-tenant tests.

## Alternatives

- Database per tenant.
- Schema per tenant.
- No multi-tenant design in MVP.

## Consequences

Benefits:

- lower operational cost;
- simpler local and pilot setup;
- still preserves a path to SaaS.

Costs:

- query-scoping defects can be severe;
- requires defence-in-depth and negative tests;
- later physical separation needs migration tooling.

## Required Evidence

- cross-organisation read/write API tests;
- database constraint tests for cross-org references;
- export scoping tests;
- cache key review before adding caches.
