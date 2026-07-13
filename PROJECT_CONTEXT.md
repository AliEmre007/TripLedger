# TripLedger Project Context

## Product Definition

TripLedger is a provider-neutral financial control layer for multi-supplier tourism operators. It converts fragmented booking, payment, settlement, refund, and supplier records into an explainable booking-level financial result and a managed discrepancy queue.

## Primary Users

- Finance or reconciliation specialist.
- Operations or reservations employee.
- Owner or general manager.
- External accountant or reviewer.
- Administrator.

## Validation Release Scope

The validation release proves:

```text
Versioned source files
    -> canonical booking and financial records
    -> expected booking economics
    -> unique deterministic match
    -> reconciliation status or explainable discrepancy
    -> traceable booking timeline
```

## Explicit Non-Goals

- Consumer travel marketplace.
- Booking engine or channel manager.
- Payment gateway or banking service.
- Statutory accounting or tax filing.
- Production provider connectors.
- AI-controlled financial matching.
- Microservices or distributed infrastructure.

## Hard Domain Rules

- Every business record belongs to exactly one organisation.
- Source identity must be idempotent.
- Money uses exact decimal amount plus currency.
- Different currencies require explicit conversion evidence.
- Financial events are immutable after acceptance.
- Corrections use reversals/replacements or controlled adjustments.
- Auto-match requires exactly one deterministic candidate.
- Missing required data is not assumed to be zero.
- Audit evidence is append-only through normal application paths.

## Key Documents

- Requirements: `PRODUCT_REQUIREMENTS.md`
- Business rules: `BUSINESS_RULES.md`
- Acceptance criteria: `ACCEPTANCE_CRITERIA.md`
- Risk register: `RISK_REGISTER.md`
- Architecture: `ARCHITECTURE.md`
- Delivery plan: `STAGE_5_DELIVERY_PLANNING.md`
