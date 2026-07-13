# ADR-003: Use CSV and Simulators Before Production Connectors

**Status:** Accepted  
**Date:** 13 July 2026

## Context

Production OTA, bank, payment gateway, and accounting API access is uncertain and commercially constrained. The product hypothesis can be tested with representative source records, versioned templates, and deterministic acceptance datasets.

## Decision

The validation release uses versioned CSV imports and realistic simulators. Production connectors are deferred until direct-user evidence proves which systems matter first.

## Alternatives

- Build a production OTA connector first.
- Build a bank or payment provider connector first.
- Manually enter all records.

## Consequences

Benefits:

- reproducible acceptance tests;
- avoids provider-contract delays;
- validates canonical model before provider-specific code;
- lowers security and compliance scope.

Costs:

- not real-time;
- users must export files;
- real provider schemas may expose gaps later.

## Revisit When

- target users identify a specific connector as an adoption blocker;
- real anonymised samples validate the canonical contract;
- security, legal, and provider terms are reviewed.
