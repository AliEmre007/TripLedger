# ADR-001: Start with a Modular Monolith

**Status:** Accepted  
**Date:** 13 July 2026

## Context

TripLedger is a solo-engineer validation release with strong financial consistency, auditability, tenant isolation, and delivery-capacity constraints. The target workload is a pilot dataset, not internet-scale traffic. Stage 3 explicitly warns against microservices, event streams, or Kubernetes without evidence.

## Decision

Build one deployable modular monolith. Modules are explicit code ownership boundaries with controlled dependencies:

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

## Alternatives

- Microservices from the start.
- Separate deployable import, reconciliation, and reporting services.
- Single unstructured application with no module boundaries.

## Consequences

Benefits:

- simple deployment and debugging;
- one database transaction boundary for financial operations;
- lower operational burden;
- faster validation release delivery.

Costs:

- module boundaries require discipline;
- independent scaling and release cadence are limited;
- later extraction requires clear module interfaces and data ownership.

## Revisit When

- separate teams need independent ownership;
- one workload has materially different scale needs;
- independent release cadence is proven necessary;
- service extraction has clear operational ownership and tracing.
