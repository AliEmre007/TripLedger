# ADR-004: Use Exact Money and Explicit FX Evidence

**Status:** Accepted  
**Date:** 13 July 2026

## Context

Financial correctness is central to TripLedger. Requirements forbid binary floating-point financial results and require currency-specific precision, explicit exchange rates, rounding evidence, and explainable calculations.

## Decision

Persist money as exact decimal amount plus ISO 4217 currency. Persist exchange-rate records for every base-currency conversion with source currency, target currency, rate, effective time, source, rounding policy, and rounded result.

## Alternatives

- Store all values as floating point.
- Convert everything silently to base currency.
- Store only minor units.

## Consequences

Benefits:

- reproducible calculations;
- prevents implicit cross-currency comparison;
- supports audit and explanation.

Costs:

- more schema and validation complexity;
- every cross-currency workflow must provide rate evidence;
- rounding policy must be versioned.

## Non-Negotiable Rules

- Missing rate means `NOT_READY`, not zero or assumed equality.
- Different currencies cannot be added, compared, allocated, or reconciled without explicit conversion evidence.
