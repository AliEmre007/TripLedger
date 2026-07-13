# ADR-006: Automatic Matching Must Be Deterministic and Unique

**Status:** Accepted  
**Date:** 13 July 2026

## Context

A false-positive reconciliation is high impact. Stage 3 PoCs show deterministic unique matching can safely automate exact cases while leaving ambiguous cases open.

## Decision

Automatic matching is allowed only when an approved deterministic rule returns exactly one valid candidate. Free-text similarity, customer names, or AI suggestions may not independently create an automatic MVP match.

## Alternatives

- Probabilistic auto-matching.
- AI-controlled matching.
- Manual matching only.

## Consequences

Benefits:

- lowers false-positive risk;
- every match can be explained;
- ambiguous cases remain visible.

Costs:

- lower automatic coverage;
- more manual review;
- later suggestion systems need separate controls.

## Approved MVP Rule Order

1. exact booking reference + compatible type + currency + amount within tolerance;
2. exact source-specific booking reference + compatible type + currency + amount within tolerance;
3. exact settlement allocation reference supplied by source.

A lower-priority rule is used only when higher-priority rules do not produce a valid unique result.
