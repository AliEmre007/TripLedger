# ADR-002: Use a Relational Transactional Database

**Status:** Accepted  
**Date:** 13 July 2026

## Context

TripLedger must prevent duplicate source effects, cross-organisation references, financial overallocation, and inconsistent reconciliation. Stage 3 PoCs show relational constraints and transactional allocation patterns are feasible, but production database concurrency must still be tested.

## Decision

Use a relational transactional database for accepted application state. PostgreSQL is the preferred implementation target unless a later implementation constraint requires another relational database.

## Alternatives

- Document database.
- Event stream as the primary persistence layer.
- Separate database per module.
- Spreadsheet/file persistence.

## Consequences

Benefits:

- strong constraints and transactions;
- mature backup and restore;
- organisation-scoped foreign keys;
- practical query support for discrepancy queues and timelines.

Costs:

- SQL schema changes need migration discipline;
- locking and isolation must be tested;
- analytical workloads may need careful indexing later.

## Required Evidence

- allocation concurrency test repeated on selected database;
- restore rehearsal;
- migration rollback/recovery procedure;
- query performance test for reference dataset.
