# FIRST_RELEASE_RISK_ADJUSTMENT.md

## TripLedger Validation Release Adjustment

**Version:** 0.1  
**Reason:** Stage 2 full P0 backlog exceeds the 12-week capacity constraint.

---

## 1. Validation Release Goal

Prove this complete path:

```text
Versioned source files
    → canonical booking and financial records
    → expected booking economics
    → unique deterministic match
    → reconciliation status or explainable discrepancy
    → traceable booking timeline
```

The release is successful when the system can explain normal and discrepant bookings safely. It does not need to operationalise every exception workflow.

---

## 2. Delivery Slices and Likely Effort

| Slice | Likely hours |
|---|---:|
| Acceptance fixtures and executable rule examples | 6 |
| Identity-provider integration, organisation context, three roles | 8 |
| Source registry and import-batch foundation | 10 |
| Booking and supplier-obligation import | 14 |
| Financial-event import, idempotency, reversal | 12 |
| Exact money and explicit FX evidence | 6 |
| Expected booking economics | 12 |
| Unique one-to-one matching | 12 |
| Reconciliation state and basic discrepancy generation | 12 |
| Booking explanation and audit timeline | 10 |
| Structured errors, logs, metrics, health | 6 |
| Deployment, backup/restore, final evidence | 8 |
| **Likely total** | **116** |
| 20% uncertainty reserve | 23 |
| **Risk-adjusted total** | **139** |

This fits 12 weeks only near 12 focused hours/week. A 14-week planning horizon is safer at the lower capacity bound.

---

## 3. Explicitly Deferred from Validation Release

- User invitation/deactivation interface.
- Tolerance administration interface.
- Manual one-to-many and many-to-one allocations.
- Accepted-variance close.
- Discrepancy assignment, comments, and waiting-external automation.
- Management dashboard.
- Accounting export.
- Large-scale performance claim.
- Production source connectors.
- Notifications.
- AI features.

The full Stage 2 documents remain the target product requirements. This file changes delivery sequencing, not the long-term domain rules.

---

## 4. Validation Release Exit

- No duplicate effect on re-import.
- Exact booking economics for the acceptance cases.
- Unique exact records match automatically.
- Ambiguous records remain unmatched.
- Material mismatches create explainable discrepancies.
- Accepted financial events cannot be edited.
- Cross-role and cross-organisation tests pass.
- Backup restore succeeds.
- Final demonstration can trace every displayed amount.
