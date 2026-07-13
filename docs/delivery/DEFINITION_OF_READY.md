# TripLedger Definition of Ready

**Stage:** 5 - Delivery Planning  
**Date:** 13 July 2026  
**Version:** 0.1

## 1. Purpose

A backlog item is ready only when a developer or agent can implement it without inventing product scope, financial rules, security boundaries, or acceptance behavior.

## 2. Ready Checklist

Before implementation starts, the item must have:

- clear user, reviewer, or operator value;
- linked requirement, business rule, risk, or architecture decision;
- linked acceptance criteria or explicit evidence target;
- known role and authorisation behavior;
- known organisation/tenant ownership behavior;
- input and output examples;
- failure behavior and error codes;
- audit requirement stated;
- observability requirement stated when the item runs a request or job;
- dependencies available or explicitly mocked;
- size S, M, or L;
- no unresolved question that could change the core workflow.

## 3. Slice Quality Rules

A ready item must be a vertical slice whenever possible.

Good examples:

- "Import booking CSV and show accepted/rejected row outcomes."
- "Create immutable financial event reversal and show it on timeline."
- "Run deterministic exact match and create discrepancy when ambiguous."

Not ready:

- "Build backend."
- "Create all database tables."
- "Finish UI."
- "Implement reconciliation."
- "Add security."

Broad technical work may exist only when tied to immediate vertical evidence, such as local project skeleton plus first executable fixture test.

## 4. Risk Readiness

High-risk areas need explicit test or evidence before work starts:

- tenant isolation;
- role and MFA enforcement;
- idempotent import;
- exact money and currency precision;
- immutable financial events;
- allocation conservation;
- deterministic matching;
- audit completeness;
- backup and restore.

## 5. Not Ready Outcomes

If an item is not ready:

- split it;
- add missing examples;
- link acceptance criteria;
- document a decision;
- or defer it.

Do not start implementation by assuming missing financial or security behavior.
