# BUSINESS_RULES.md

## TripLedger Business-Rule Catalogue

**Version:** 0.1  
**Date:** 13 July 2026  
**Purpose:** Define valid and invalid domain behaviour before API and persistence implementation.

---

## 1. Rule Format

Each rule contains:

- **Statement:** Mandatory domain behaviour.
- **Reason:** Why the rule exists.
- **Invalid condition:** What must be rejected, held for review, or represented as an exception.
- **Enforcement:** Where the rule must be protected.
- **Evidence:** Expected test or audit output.

Severity:

- **Invariant:** Must never be violated in accepted state.
- **Control:** May be completed only by an authorised workflow.
- **Policy:** Configurable within bounded values.
- **Warning:** Does not block processing but must be visible.

---

## 2. Organisation and Access Rules

### BR-ORG-001 — Organisation ownership

**Severity:** Invariant  
Every business record belongs to exactly one organisation.

- Invalid: record with no organisation or more than one owning organisation.
- Enforcement: write model, query filters, authorisation, background jobs, exports.
- Evidence: cross-organisation tests return no data.

### BR-ORG-002 — Cross-organisation references

**Severity:** Invariant  
A record may reference only records owned by the same organisation.

- Invalid: booking in Organisation A linked to a payment, supplier obligation, user, or source in Organisation B.
- Result: reject with `ORG_REFERENCE_MISMATCH`.

### BR-ORG-003 — Historical actor preservation

**Severity:** Invariant  
User deactivation does not remove or anonymise the actor identity required for audit history.

### BR-AUTH-001 — Financial authority

**Severity:** Control  
Only Finance or Administrator roles may manually match financial records, create controlled financial adjustments, or accept material variance.

### BR-AUTH-002 — Operational authority

**Severity:** Control  
Operations may change operational booking fields and unapproved expected supplier costs but may not alter imported financial events.

### BR-AUTH-003 — Server-side enforcement

**Severity:** Invariant  
Permission checks must be enforced on the server for every protected operation.

---

## 3. Source and Import Rules

### BR-IMP-001 — Stable source identity

**Severity:** Invariant  
A source record is uniquely identified by:

`organisation + source_system + record_type + external_record_id + source_version`

When the source has no explicit version, a normalised content checksum shall act as the version identity.

### BR-IMP-002 — Idempotent import

**Severity:** Invariant  
Re-importing the same source identity produces no duplicate domain record and no additional financial effect.

- Duplicate rows are counted and reported.
- Duplicate import is not treated as an application error.

### BR-IMP-003 — Source version ordering

**Severity:** Control  
A lower source version may not overwrite a higher accepted source version.

- Result: reject or warn with `STALE_SOURCE_VERSION`.
- An authorised reversal or corrective source version is required.

### BR-IMP-004 — Visible row result

**Severity:** Invariant  
Every input row ends in exactly one reported result:

- accepted,
- duplicate,
- rejected,
- or accepted with warning.

No row may disappear silently.

### BR-IMP-005 — Required import provenance

**Severity:** Invariant  
Every accepted imported record retains:

- import batch,
- source system,
- external record identifier,
- template version,
- source-row number,
- accepted time,
- and source checksum or payload reference.

### BR-IMP-006 — Partial batch handling

**Severity:** Policy  
Valid rows may be accepted when other rows fail, provided:

- row-level errors are visible,
- accepted rows remain internally consistent,
- and batch status becomes `COMPLETED_WITH_ERRORS`.

A file-level format or security failure causes the whole batch to become `FAILED`.

### BR-IMP-007 — Template version

**Severity:** Invariant  
Unknown or unsupported template versions are rejected before domain records are created.

### BR-IMP-008 — File limits

**Severity:** Policy  
The MVP accepts CSV only, maximum 25 MB and 100,000 data rows per file. Larger files are rejected with `IMPORT_LIMIT_EXCEEDED`.

---

## 4. Money and Currency Rules

### BR-MNY-001 — Money representation

**Severity:** Invariant  
Money is represented by exact decimal amount plus currency. Binary floating-point is invalid for persisted or calculated financial values.

### BR-MNY-002 — Currency required

**Severity:** Invariant  
Every financial amount has exactly one supported three-letter currency code.

### BR-MNY-003 — Currency precision

**Severity:** Invariant  
An amount may not contain more fractional digits than allowed by the currency configuration.

- Result: reject with `INVALID_CURRENCY_PRECISION`.

### BR-MNY-004 — No implicit conversion

**Severity:** Invariant  
Amounts in different currencies may not be added, compared for equality, allocated, or reconciled without an explicit conversion record.

### BR-MNY-005 — Exchange-rate evidence

**Severity:** Invariant  
Every base-currency amount derived from another currency records:

- source currency,
- target currency,
- rate,
- effective date/time,
- rate source or manual authority,
- and rounding result.

### BR-MNY-006 — Rounding

**Severity:** Invariant  
Rounding occurs only at declared currency or reporting boundaries. Intermediate calculations retain sufficient precision. The selected rounding method is consistent within an organisation and versioned.

### BR-MNY-007 — Negative values

**Severity:** Invariant  
Negative imported amounts are permitted only for event types whose contract explicitly allows them. Otherwise the row is rejected.

### BR-MNY-008 — Materiality

**Severity:** Policy  
The organisation defines a base-currency materiality threshold. Default: 1.00 in base currency.

- A variance at or above the threshold creates a material discrepancy.
- A smaller non-zero variance remains visible and cannot be silently discarded.

### BR-MNY-009 — Amount tolerance

**Severity:** Policy  
The default reconciliation amount tolerance is one minor currency unit. An Administrator may configure a non-negative tolerance per currency.

---

## 5. Booking Rules

### BR-BKG-001 — Booking identity

**Severity:** Invariant  
Within one source system, an external booking identifier identifies one canonical booking in an organisation.

### BR-BKG-002 — Booking items required

**Severity:** Invariant  
A confirmed, in-service, or completed booking contains at least one active or historically active booking item.

### BR-BKG-003 — Contracted gross sale

**Severity:** Invariant  
Contracted gross sale equals the sum of original selling amounts for booking items, adjusted only through explicit discount, cancellation, or correction records.

### BR-BKG-004 — Service date validity

**Severity:** Invariant  
An item end date may not precede its start date. A booking service-end date may not precede the earliest active item start date.

### BR-BKG-005 — Cancellation history

**Severity:** Invariant  
Cancellation does not delete booking items, payments, obligations, or prior calculations. It changes state and creates the required financial expectation events.

### BR-BKG-006 — Lifecycle transition

**Severity:** Control  
Allowed booking transitions:

- `DRAFT → CONFIRMED`
- `CONFIRMED → IN_SERVICE`
- `CONFIRMED → CANCELLED`
- `IN_SERVICE → COMPLETED`
- `IN_SERVICE → CANCELLED`
- `CANCELLED → CONFIRMED` only through authorised reinstatement with audit reason.

Invalid transitions return `INVALID_BOOKING_TRANSITION`.

### BR-BKG-007 — Material change recalculation

**Severity:** Invariant  
A change to amount, currency, active items, supplier cost, cancellation, refund expectation, or fee expectation marks the booking for recalculation.

---

## 6. Supplier-Obligation Rules

### BR-SUP-001 — Positive obligation

**Severity:** Invariant  
An obligation amount is positive. Reductions are represented by supplier credits or cancellation events, not by changing the original obligation to a negative amount.

### BR-SUP-002 — Booking relationship

**Severity:** Control  
An obligation must link to a booking or booking item before it can contribute to booking economics.

Unlinked obligations remain visible but excluded from booking reconciliation.

### BR-SUP-003 — State transition

**Severity:** Control  
Supported transitions preserve history:

- `EXPECTED → CONFIRMED`
- `EXPECTED → CANCELLED`
- `CONFIRMED → INVOICED`
- `CONFIRMED → CANCELLED`
- `INVOICED → PAID`
- Any reduction after confirmation uses a credit event.

### BR-SUP-004 — Finance control boundary

**Severity:** Control  
After a booking first reaches `RECONCILED`, changing supplier cost requires a controlled adjustment or a new source version and reopens reconciliation.

### BR-SUP-005 — Supplier allocation

**Severity:** Invariant  
A supplier payment allocation may not exceed the remaining unpaid obligation amount, except when an explicit prepayment or overpayment state is supported and visible.

The MVP rejects excess allocation with `SUPPLIER_OVERALLOCATION`.

---

## 7. Financial-Event Rules

### BR-FIN-001 — Immutable accepted event

**Severity:** Invariant  
An accepted financial event cannot be edited or hard-deleted.

### BR-FIN-002 — Correction by reversal

**Severity:** Invariant  
A source correction uses:

1. a reversal linked to the original event, and
2. a replacement event when applicable.

The net effect of a full reversal is zero in the original currency.

### BR-FIN-003 — External identity uniqueness

**Severity:** Invariant  
A financial source identity can affect the ledger only once.

### BR-FIN-004 — Event sign convention

**Severity:** Invariant  
The canonical financial direction is defined by event type, not by user interpretation:

- customer payment and channel settlement: increase received value;
- refund and payment reversal: decrease received value;
- channel commission and payment fee: increase deductions;
- supplier obligation and supplier payment: increase supplier cost or settlement evidence according to their distinct models;
- supplier credit: decreases supplier cost;
- adjustment: direction declared explicitly and controlled.

### BR-FIN-005 — Booking link

**Severity:** Control  
A financial event may be accepted without a booking link, but it remains unmatched and cannot contribute to a booking's reconciled status until allocated.

### BR-FIN-006 — Event date

**Severity:** Invariant  
Every financial event has an effective timestamp. Import time is not a substitute for event time.

### BR-FIN-007 — Manual event evidence

**Severity:** Control  
A manual financial event requires:

- authorised actor,
- reason,
- amount and currency,
- effective timestamp,
- evidence reference when available,
- and audit event.

### BR-FIN-008 — Raw payment data prohibition

**Severity:** Invariant  
A financial event must not contain raw card number, security code, authentication secret, or unrestricted bank credential.

---

## 8. Booking-Economics Rules

### BR-ECO-001 — Contracted gross sale

`contracted_gross_sale = sum(original active and historically contracted booking-item selling amounts)`

Cancellations and discounts do not rewrite the original amount; they appear as explicit reductions.

### BR-ECO-002 — Expected customer receivable

`expected_customer_receivable = contracted_gross_sale - approved_discounts - expected_customer_refunds - waived_amounts`

A retained cancellation fee remains receivable and is not subtracted.

### BR-ECO-003 — Expected deductions

`expected_deductions = expected_channel_commissions + expected_payment_fees`

Taxes are excluded from this MVP formula unless represented by an explicit configured component.

### BR-ECO-004 — Active supplier cost

`active_supplier_cost = confirmed_or_invoiced_obligations - supplier_credits - cancelled_obligations`

Expected but unconfirmed obligations may be shown separately and make the booking `NOT_READY` when supplier confirmation is required.

### BR-ECO-005 — Estimated gross margin

`estimated_gross_margin = expected_customer_receivable - expected_deductions - active_supplier_cost`

This is an operational gross-margin estimate, not statutory profit or tax accounting.

### BR-ECO-006 — Missing data is not zero

**Severity:** Invariant  
A required missing component produces `UNKNOWN`/`NOT_READY`. The system must not replace unknown commission, supplier cost, exchange rate, or refund expectation with zero.

### BR-ECO-007 — Formula explainability

**Severity:** Invariant  
Every subtotal and result exposes the contributing records, currency treatment, and rule version.

### BR-ECO-008 — Calculation version

**Severity:** Invariant  
A reconciliation result records the business-rule/calculation version that produced it.

---

## 9. Matching Rules

### BR-MAT-001 — Deterministic auto-match

**Severity:** Invariant  
Automatic matching is allowed only when an approved rule returns exactly one valid candidate.

### BR-MAT-002 — Auto-match rule order

The MVP evaluates:

1. exact booking reference + compatible event type + currency + amount within tolerance;
2. exact source-specific booking reference + compatible event type + currency + amount within tolerance;
3. exact settlement allocation reference supplied by the source.

A lower-priority rule is used only when a higher-priority rule did not produce a valid unique result.

### BR-MAT-003 — Date window

**Severity:** Policy  
Default candidate date window: 7 calendar days before through 30 calendar days after the expected event date. Administrator configuration is allowed within 0–90 days.

### BR-MAT-004 — Ambiguity

**Severity:** Invariant  
Two or more valid candidates prohibit auto-match. Records remain unmatched with `AMBIGUOUS_MATCH`.

### BR-MAT-005 — Allocation conservation

**Severity:** Invariant  
For each financial event:

`sum(active match allocations) <= absolute available event amount`

For a fully matched record, the allocated sum equals the available amount within tolerance.

### BR-MAT-006 — Currency compatibility

**Severity:** Invariant  
Direct matching requires the same currency. Cross-currency matching requires a recorded conversion and compares the approved converted amount.

### BR-MAT-007 — Manual-match evidence

**Severity:** Control  
A manual match requires Finance authority and a reason. It records actor, time, allocations, and records involved.

### BR-MAT-008 — Unmatch

**Severity:** Control  
Removing a match requires Finance authority and a reason, recalculates affected bookings, and preserves the prior match in audit history.

### BR-MAT-009 — No match by free text alone

**Severity:** Invariant  
Customer name, memo similarity, or other free text may be shown as a suggestion but cannot independently create an automatic match in the MVP.

---

## 10. Reconciliation Rules

### BR-REC-001 — Eligibility

A booking is eligible when:

- required booking identity and currency exist;
- at least one booking item exists;
- required expected economics are known;
- required exchange rates exist;
- no blocking invalid state exists.

Otherwise status is `NOT_READY`.

### BR-REC-002 — Reconciled state

A booking is `RECONCILED` only when:

- expected customer receipts are matched within tolerance;
- expected deductions are matched or explicitly confirmed within tolerance;
- required refunds are matched;
- active supplier obligations are recorded and supplier-payment matching requirements for the configured workflow are satisfied;
- no open material discrepancy exists.

### BR-REC-003 — Partial state

A booking is `PARTIALLY_RECONCILED` when at least one required component is validly matched and at least one remains unmatched without a blocking invalid state.

### BR-REC-004 — Discrepant state

A booking is `DISCREPANT` when any material mismatch, duplicate effect, ambiguous match, invalid state, or overdue required record is open.

### BR-REC-005 — Closed with variance

**Severity:** Control  
`CLOSED_WITH_VARIANCE` requires:

- Finance or Administrator authority;
- documented reason;
- variance amount and currency;
- materiality assessment;
- evidence reference;
- and no unresolved security or data-integrity violation.

A duplicate financial effect or cross-organisation reference can never be accepted as a business variance.

### BR-REC-006 — Repeatability

**Severity:** Invariant  
Unchanged inputs and settings produce the same result and component totals.

### BR-REC-007 — Reopen

**Severity:** Invariant  
A later accepted source event or material booking change that invalidates a closed result reopens reconciliation and creates audit evidence.

---

## 11. Discrepancy Rules

### BR-DIS-001 — Unique active discrepancy

**Severity:** Control  
One active discrepancy exists per booking, discrepancy type, component, and cause identity. Recalculation updates or resolves it rather than creating endless duplicates.

### BR-DIS-002 — Severity

Severity is determined by:

- financial materiality,
- age,
- booking service proximity,
- and invalid-condition category.

Default levels: `LOW`, `MEDIUM`, `HIGH`, `CRITICAL`.

### BR-DIS-003 — Required ownership

**Severity:** Policy  
A material discrepancy must be assigned within one business day in the pilot.

### BR-DIS-004 — Resolution evidence

**Severity:** Control  
Resolution requires one of:

- source corrected,
- mapping corrected,
- matching corrected,
- external confirmation received,
- timing difference completed,
- controlled adjustment,
- accepted variance,
- false positive caused by rule defect.

A note is mandatory.

### BR-DIS-005 — Reopen on invalidation

**Severity:** Invariant  
A resolved discrepancy reopens if new evidence invalidates its resolution.

### BR-DIS-006 — Timing difference

**Severity:** Control  
A timing difference may move to `WAITING_EXTERNAL` with expected resolution date. When that date passes, it returns to active review automatically.

---

## 12. Audit and Retention Rules

### BR-AUD-001 — Append-only audit

**Severity:** Invariant  
Audit events cannot be edited or deleted through normal application functions.

### BR-AUD-002 — Required audit fields

Every audit event includes:

- organisation,
- actor or system identity,
- action,
- target type and identifier,
- timestamp,
- correlation identifier,
- outcome,
- and material before/after values or references.

### BR-AUD-003 — Source versus derived data

**Severity:** Invariant  
The system visibly distinguishes:

- imported source data,
- manually entered source-like data,
- system-derived calculations,
- and controlled adjustments.

### BR-AUD-004 — Hard deletion

**Severity:** Invariant  
Accepted financial events, matches, adjustments, discrepancy resolutions, and audit events are never hard-deleted.

### BR-AUD-005 — Export traceability

**Severity:** Invariant  
Every generated export has a version, filter definition, row count, checksum, actor, and generation timestamp.

---

## 13. Invalid-Condition Catalogue

| Error code | Condition | Required behaviour |
|---|---|---|
| `ORG_REFERENCE_MISMATCH` | Cross-organisation reference | Reject and security-log |
| `UNAUTHORISED_FINANCIAL_ACTION` | Role lacks authority | Reject and audit |
| `MFA_REQUIRED` | MFA claim is missing for MFA-protected financial function | Reject and audit |
| `UNSUPPORTED_TEMPLATE_VERSION` | Import template unknown | Fail batch before writes |
| `SOURCE_SYSTEM_NOT_FOUND` | Import references missing or cross-organisation source system | Reject without revealing other organisations |
| `INACTIVE_SOURCE_SYSTEM` | Import targets inactive source system | Reject batch creation |
| `IMPORT_BATCH_NOT_FOUND` | Import batch is missing or outside actor organisation | Reject without revealing other organisations |
| `DUPLICATE_IMPORT_ROW_RESULT` | Row result already exists for the batch row number | Reject duplicate row outcome |
| `IMPORT_BATCH_TERMINAL` | Completed or failed batch is changed | Reject mutation |
| `IMPORT_LIMIT_EXCEEDED` | File size or row limit exceeded | Reject file |
| `MISSING_REQUIRED_COLUMN` | Required CSV column absent | Fail validation |
| `MISSING_REQUIRED_FIELD` | Required row field absent | Reject row |
| `INVALID_FIELD_TYPE` | Field cannot be parsed | Reject row |
| `INVALID_BOOKING_DATE` | Booking or item date range is invalid | Reject row |
| `INVALID_CURRENCY` | Currency unsupported or missing | Reject row |
| `INVALID_CURRENCY_PRECISION` | Too many fractional digits | Reject row |
| `STALE_SOURCE_VERSION` | Older version attempts overwrite | Reject or warning without overwrite |
| `DUPLICATE_SOURCE_RECORD` | Same source identity imported | Count as duplicate; no new effect |
| `INVALID_BOOKING_TRANSITION` | Lifecycle transition forbidden | Reject action |
| `MISSING_BOOKING_ITEM` | Eligible booking has no item | Mark `NOT_READY` |
| `MISSING_EXCHANGE_RATE` | Required conversion unavailable | Mark `NOT_READY`; no implicit conversion |
| `MISSING_EXPECTED_COST` | Required supplier cost unknown | Mark `NOT_READY` |
| `FINANCIAL_EVENT_IMMUTABLE` | Direct edit/delete attempted | Reject; require reversal |
| `AMBIGUOUS_MATCH` | More than one candidate | Do not auto-match; create review item |
| `MATCH_OVERALLOCATION` | Allocations exceed event amount | Reject transaction |
| `SUPPLIER_OVERALLOCATION` | Payment exceeds supported obligation balance | Reject allocation |
| `INVALID_MANUAL_ADJUSTMENT` | Missing authority, reason, or evidence | Reject |
| `UNACCEPTABLE_VARIANCE` | Data-integrity/security error treated as variance | Reject |
| `IMPORT_PROCESSING_FAILED` | Processing terminates unexpectedly | Preserve prior accepted state; expose failure |
| `EXPORT_SCOPE_FORBIDDEN` | User requests unauthorised export | Reject and audit |

---

## 14. Business-Rule Acceptance Gate

A business rule is implementation-ready only when:

- Its owner and severity are clear.
- Valid and invalid examples exist.
- Error behaviour is defined.
- It is mapped to automated tests.
- It does not depend on an undocumented UI convention.
- Its audit requirement is defined.
