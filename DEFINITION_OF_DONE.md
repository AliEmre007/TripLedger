# TripLedger Definition of Done

**Stage:** 5 - Delivery Planning  
**Date:** 13 July 2026  
**Version:** 0.1

## 1. Purpose

A backlog item is done only when it creates working, tested, reviewable evidence. A happy-path demo alone is not enough.

## 2. Done Checklist

For every implemented item:

- acceptance criteria or explicit evidence target passes;
- relevant business-rule tests pass;
- role and organisation authorisation tests pass where applicable;
- failure cases return stable error codes and correlation ids;
- audit events are produced for material actions;
- logs and metrics are safe and useful for the item;
- data writes preserve provenance and organisation ownership;
- no accepted financial event is destructively edited;
- exact money and currency rules are respected;
- documentation and examples are updated;
- the item can be demonstrated from a clean environment or documented fixture;
- no critical or high-severity defect remains open.

## 3. Financial Done Criteria

For financial or reconciliation work:

- binary floating-point does not determine persisted or displayed financial results;
- missing required data is represented as `UNKNOWN` or `NOT_READY`, not zero;
- cross-currency behavior requires exchange-rate evidence;
- duplicate source identity creates no duplicate financial effect;
- corrections use reversal/replacement or controlled adjustment;
- automatic matching creates a match only for one deterministic candidate;
- unchanged rerun creates no duplicate effect.

## 4. Security Done Criteria

For protected workflows:

- server-side authorisation is enforced;
- direct API calls cannot bypass UI restrictions;
- cross-organisation access returns no protected data;
- Administrator and Finance financial functions enforce MFA policy;
- sensitive values are not returned in APIs, logs, errors, fixtures, or exports.

## 5. Operational Done Criteria

For jobs, imports, exports, and deployment work:

- status is visible without database modification;
- retry behavior is bounded;
- terminal failure is visible and diagnosable;
- health/readiness behavior is documented when affected;
- backup/restore or deployment evidence is updated when the item changes operational state.

## 6. Documentation Done Criteria

Update the relevant document in the same change when behavior changes:

- requirements or scope behavior: `PRODUCT_REQUIREMENTS.md`;
- business rule or error behavior: `BUSINESS_RULES.md`;
- API contract: `API_SPECIFICATION.md`;
- data model or invariant: `DATABASE_DESIGN.md` or `DOMAIN_MODEL.md`;
- architecture decision: `ADRs/`;
- delivery sequencing: `VALIDATION_RELEASE_BACKLOG.md` or `MILESTONE_PLAN.md`.

## 7. Release Done Criteria

The validation release is done only when:

- no duplicate effect occurs on re-import;
- exact booking economics pass acceptance cases;
- unique exact records match automatically;
- ambiguous records remain unmatched;
- material mismatches create explainable discrepancies;
- accepted financial events cannot be edited;
- cross-role and cross-organisation tests pass;
- backup restore succeeds;
- the final demonstration traces every displayed amount to source, rule, actor, and audit evidence.
