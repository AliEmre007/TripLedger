# TRACEABILITY_MATRIX.md

## TripLedger Stage 2 Traceability Matrix

**Version:** 0.1  
**Purpose:** Show that each MVP capability connects user outcome, requirement, business rule, acceptance evidence, and backlog delivery.

| Capability | Functional requirements | User stories | Business rules | Acceptance | Backlog |
|---|---|---|---|---|---|
| Role-controlled access | FR-001–FR-007 | US-001, US-002 | BR-ORG-001–003, BR-AUTH-001–003 | AC-001–AC-004 | F-002, F-003 |
| Source-system provenance | FR-010–FR-013 | US-010 | BR-IMP-001, BR-IMP-005 | AC-010 | F-004 |
| Booking import | FR-020–FR-036 | US-011, US-014, US-020 | BR-IMP-001–008, BR-BKG-001–007 | AC-011–AC-014, AC-020, AC-021, AC-030 | F-001, F-005–F-007 |
| Supplier obligations | FR-040–FR-045 | US-013, US-021 | BR-SUP-001–005 | AC-018, AC-019, AC-031, AC-032 | F-008, F-009 |
| Financial-event import | FR-050–FR-058 | US-012 | BR-MNY-001–009, BR-FIN-001–008 | AC-015–AC-017 | F-010–F-012 |
| Expected economics | FR-060–FR-067 | US-022, US-023 | BR-ECO-001–008 | AC-033–AC-036 | F-013, F-014 |
| Deterministic matching | FR-070–FR-079 | US-030, US-031, US-033 | BR-MAT-001–009 | AC-040–AC-045, AC-049 | F-015, F-016 |
| Reconciliation state | FR-077–FR-079 | US-032 | BR-REC-001–007 | AC-046–AC-049 | F-017 |
| Discrepancy queue | FR-080–FR-088 | US-040, US-041, US-044 | BR-DIS-001–006 | AC-050–AC-053, AC-058 | F-018–F-020, F-023 |
| Resolution and accepted variance | FR-083–FR-086 | US-042, US-043 | BR-REC-005, BR-DIS-004–005 | AC-054–AC-057 | F-021, F-022 |
| Audit timeline | FR-090–FR-092 | US-050 | BR-AUD-001–004 | AC-060, AC-061 | F-024 |
| Dashboard | FR-093–FR-095 | US-051 | BR-MNY-004–006, BR-AUD-003 | AC-062, AC-063 | F-025 |
| Export | FR-096–FR-098 | US-052 | BR-AUD-005 | AC-064, AC-065 | F-026 |
| Failure recovery and diagnostics | NFR-REL, NFR-OBS | US-060, US-061, US-062 | BR-IMP-004, BR-AUD-002 | AC-070–AC-073 | F-027–F-030 |
| Capacity evidence | NFR-PERF | — | Financial invariants remain applicable | AC-080–AC-082 | F-031 |
| End-to-end product proof | All P0 | All P0 | All invariants and controls | Demonstration scenarios 1–15 | F-032 |

---

## Coverage Rules

- Every P0 backlog item must map to at least one acceptance criterion or explicit NFR evidence.
- Every financial acceptance scenario must reference the applicable business rule in its automated test name or metadata.
- A requirement change must update this matrix in the same change set.
- Unmapped implementation code is presumed out of scope until linked to a requirement.
