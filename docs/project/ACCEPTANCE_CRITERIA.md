# ACCEPTANCE_CRITERIA.md

## TripLedger MVP Acceptance Criteria

**Version:** 0.1  
**Date:** 13 July 2026  
**Format:** Given / When / Then

---

## A. Identity and Access

### AC-001 — Role enforcement

**Given** an Operations user is authenticated  
**When** the user attempts to create a manual financial adjustment through the UI or direct API request  
**Then** the operation is rejected  
**And** no financial record is created  
**And** the attempt is audited with a stable error code.

### AC-002 — Organisation isolation

**Given** a user belongs to Organisation A  
**And** a booking belongs to Organisation B  
**When** the user requests, searches for, exports, or modifies that booking using its identifier  
**Then** no Organisation B data is returned  
**And** the response does not reveal whether the booking exists.

### AC-003 — Deactivated user

**Given** a user has an active session  
**When** an Administrator deactivates the user  
**Then** the user cannot start a new session  
**And** the existing session loses access no later than the next protected request or token-policy boundary  
**And** prior audit events still show the historical actor.

### AC-004 — MFA for Finance and Administrator

**Given** a Finance or Administrator user has not completed MFA  
**When** the user attempts to access a financial function  
**Then** access is denied or redirected to MFA completion  
**And** no protected data is returned.

---

## B. Source and Import

### AC-010 — Register unique source

**Given** an Administrator is creating a source system  
**When** a unique external code and valid category are submitted  
**Then** the source is created  
**And** it can be selected for an import.

**Given** the external code already exists in the organisation  
**When** the Administrator submits it again  
**Then** creation is rejected with `DUPLICATE_SOURCE_CODE`.

### AC-011 — Accept valid booking import

**Given** a supported booking CSV template containing valid rows  
**When** an authorised user commits the import  
**Then** every valid source record creates or updates one canonical booking according to source-version rules  
**And** the import summary counts accepted rows correctly  
**And** provenance is visible on each booking.

### AC-012 — Mixed valid and invalid booking rows

**Given** a booking CSV containing eight valid rows and two invalid rows  
**When** the file is processed  
**Then** eight rows are accepted  
**And** two rows are rejected  
**And** the batch status is `COMPLETED_WITH_ERRORS`  
**And** each rejection identifies row, field, error code, and reason.

### AC-013 — Idempotent booking re-import

**Given** a booking file has completed successfully  
**When** the unchanged file is imported again for the same source  
**Then** no duplicate booking, item, or financial effect is created  
**And** rows are reported as duplicates  
**And** existing audit and source identities remain stable.

### AC-014 — Reject stale source version

**Given** booking version 3 is accepted  
**When** version 2 for the same source booking is imported  
**Then** version 3 remains current  
**And** version 2 does not overwrite it  
**And** the row is rejected or warned as `STALE_SOURCE_VERSION`.

### AC-015 — Accept valid financial events

**Given** a supported financial-event CSV with a payment, commission, fee, settlement, and refund  
**When** a Finance user imports it  
**Then** each valid event is accepted once  
**And** event type, amount, currency, effective time, source identity, and provenance are preserved.

### AC-016 — Reject invalid money

**Given** a financial row with an unsupported currency or invalid currency precision  
**When** it is validated  
**Then** the row is rejected  
**And** no financial event is created  
**And** the exact field and error code are reported.

### AC-017 — Preserve unmatched event

**Given** a valid payment has no recognised booking reference  
**When** it is imported  
**Then** the payment is accepted as an unmatched financial event  
**And** it is visible for review  
**And** it does not contribute to a booking result.

### AC-018 — Import supplier obligation

**Given** a valid obligation references a known booking item  
**When** it is imported  
**Then** it contributes to active supplier cost according to its state  
**And** provenance is visible.

### AC-019 — Unlinked supplier obligation

**Given** a valid obligation has no known booking or item  
**When** it is imported  
**Then** it is accepted as unlinked  
**And** it is excluded from booking economics  
**And** the system exposes it for review.

### AC-020 — Download rejected rows

**Given** an import contains rejected rows  
**When** the user downloads the error file  
**Then** it contains the original row identity, rejected values, field, code, and reason  
**And** it contains no secret or hidden system value.

### AC-021 — File-level failure

**Given** an unsupported template version or invalid file format  
**When** the file is submitted  
**Then** the batch becomes `FAILED`  
**And** no domain rows are created  
**And** the reason is visible without database access.

---

## C. Booking and Economics

### AC-030 — Canonical booking view

**Given** a booking has multiple items, two suppliers, and several source versions  
**When** an authorised user opens it  
**Then** the current booking state, items, service dates, source identity, currencies, and status are visible  
**And** historical source versions remain traceable.

### AC-031 — Operational supplier-cost update before control

**Given** a booking has not reached reconciliation  
**When** Operations changes an unapproved expected supplier cost  
**Then** the new expected cost is recorded with audit evidence  
**And** booking economics are recalculated.

### AC-032 — Supplier-cost change after reconciliation

**Given** a booking is `RECONCILED`  
**When** Operations attempts to overwrite supplier cost  
**Then** direct overwrite is rejected  
**And** Finance must process a new source version or controlled adjustment  
**And** reconciliation reopens when the accepted change occurs.

### AC-033 — Calculate normal booking economics

**Given**
- contracted gross sale is EUR 1,000,
- approved discount is EUR 50,
- expected channel commission is EUR 142.50,
- expected payment fee is EUR 20,
- active supplier cost is EUR 500,
- and no refund is expected

**When** economics are calculated  
**Then**
- expected customer receivable is EUR 950,
- expected deductions are EUR 162.50,
- active supplier cost is EUR 500,
- and estimated gross margin is EUR 287.50.

### AC-034 — Cancellation and refund calculation

**Given** a EUR 1,000 booking is cancelled  
**And** the customer is entitled to an EUR 800 refund  
**And** EUR 200 is retained as cancellation fee  
**When** economics are calculated  
**Then** expected customer receivable is EUR 200  
**And** the original contracted gross sale remains EUR 1,000  
**And** the refund is displayed as an explicit component.

### AC-035 — Missing required value

**Given** supplier cost is required but unknown  
**When** economics are calculated  
**Then** supplier cost is `UNKNOWN`  
**And** estimated gross margin is not presented as a complete value  
**And** reconciliation status is `NOT_READY`.

### AC-036 — Calculation explanation

**Given** a user selects an economics subtotal  
**When** the explanation is opened  
**Then** the formula, rule version, component records, original currencies, exchange rates, and rounding are visible.

---

## D. Matching and Reconciliation

### AC-040 — Unique automatic match

**Given** one payment has the same booking reference, compatible type, currency, and amount as one expected receipt  
**And** its date is within the configured window  
**When** reconciliation runs  
**Then** one automatic match is created  
**And** the matching rule and participating records are recorded.

### AC-041 — Ambiguous automatic match

**Given** one payment satisfies the approved rule for two bookings  
**When** reconciliation runs  
**Then** no automatic match is created  
**And** the payment remains available  
**And** an `AMBIGUOUS_MATCH` discrepancy or review item is created.

### AC-042 — Cross-currency match

**Given** expected receipt is EUR 100  
**And** payment is TRY with a recorded EUR-to-TRY conversion for the event  
**When** converted value is within tolerance  
**Then** the record may match using the recorded conversion  
**And** both original and converted amounts remain visible.

**Given** no conversion record exists  
**Then** no cross-currency match occurs.

### AC-043 — One settlement allocated to multiple bookings

**Given** one settlement of EUR 900 covers Booking A for EUR 500 and Booking B for EUR 400  
**When** Finance creates both allocations  
**Then** the settlement is fully allocated  
**And** both bookings use only their allocated amounts  
**And** total allocation equals EUR 900.

### AC-044 — Prevent overallocation

**Given** a payment has EUR 100 available  
**When** a user attempts allocations totalling EUR 110  
**Then** the operation is rejected atomically with `MATCH_OVERALLOCATION`  
**And** previous valid allocations remain unchanged.

### AC-045 — Manual unmatch

**Given** a Finance user manually removes a match with a reason  
**When** the operation succeeds  
**Then** the participating records become available according to their remaining balances  
**And** affected bookings recalculate  
**And** the removed match remains in audit history.

### AC-046 — Reconciled booking

**Given** all required receipts, deductions, refunds, and supplier components match within tolerance  
**And** no material discrepancy is open  
**When** reconciliation completes  
**Then** status is `RECONCILED`.

### AC-047 — Partially reconciled booking

**Given** at least one required component is matched  
**And** another required component remains unmatched  
**When** reconciliation completes  
**Then** status is `PARTIALLY_RECONCILED`.

### AC-048 — Later event reopens booking

**Given** a booking is `RECONCILED`  
**When** a later accepted refund or corrected supplier obligation changes the valid result  
**Then** the booking leaves `RECONCILED`  
**And** affected discrepancies are created or reopened  
**And** the prior result remains auditable.

### AC-049 — Repeatable re-run

**Given** records, settings, rules, and exchange rates are unchanged  
**When** reconciliation is run twice  
**Then** component totals, matches, discrepancies, and status are equivalent  
**And** no duplicate financial effect is created.

---

## E. Discrepancy Workflow

### AC-050 — Create material short-settlement discrepancy

**Given** expected settlement is EUR 850  
**And** received settlement is EUR 800  
**And** EUR 50 exceeds materiality  
**When** reconciliation runs  
**Then** a `SHORT_SETTLEMENT` discrepancy is created  
**And** it shows expected, actual, variance, evidence, age, severity, and owner state.

### AC-051 — Avoid duplicate active discrepancies

**Given** an active short-settlement discrepancy already represents the same cause  
**When** reconciliation runs again without relevant change  
**Then** a duplicate discrepancy is not created  
**And** the existing discrepancy is updated or left unchanged.

### AC-052 — Filter queue

**Given** discrepancies exist across several types, owners, ages, and currencies  
**When** a Finance user applies filters  
**Then** only matching discrepancies are returned  
**And** summary counts and values reflect the same filter.

### AC-053 — Investigation evidence

**Given** a user opens a discrepancy  
**Then** the user can see related booking, expected component, actual records, matching attempts, source provenance, comments, status changes, and permitted audit events.

### AC-054 — Resolve after source correction

**Given** a missing fee caused a discrepancy  
**When** the corrected source event is imported and reconciliation runs  
**Then** the discrepancy resolves only if the booking now satisfies the rules  
**And** resolution type identifies source correction.

### AC-055 — Reopen invalidated resolution

**Given** a discrepancy is resolved  
**When** later evidence invalidates the resolution  
**Then** it reopens  
**And** the prior resolution remains visible.

### AC-056 — Accept legitimate material variance

**Given** a Finance user provides a permitted variance reason and evidence  
**When** the variance is accepted  
**Then** status becomes `ACCEPTED_VARIANCE`  
**And** the booking may become `CLOSED_WITH_VARIANCE`  
**And** the amount and reason remain visible in reports.

### AC-057 — Reject invalid variance acceptance

**Given** the cause is duplicate financial effect, cross-organisation reference, or missing authorisation  
**When** a user attempts to accept it as variance  
**Then** the operation is rejected  
**And** the integrity issue remains open.

### AC-058 — Timing difference returns when overdue

**Given** a discrepancy is marked `WAITING_EXTERNAL` with an expected date  
**When** that date passes without the expected event  
**Then** it returns to active review  
**And** its age continues from original creation.

---

## F. Audit, Dashboard, and Export

### AC-060 — Complete booking timeline

**Given** a booking has imports, calculations, a manual match, a refund, and a resolved discrepancy  
**When** the timeline is viewed  
**Then** events appear chronologically  
**And** source, system-derived, and user-controlled events are visually and semantically distinct.

### AC-061 — Immutable audit evidence

**Given** an audit event exists for a financial action  
**When** a normal application user attempts to edit or delete it  
**Then** the operation is unavailable and rejected  
**And** the audit event remains unchanged.

### AC-062 — Dashboard reconciles to detail

**Given** a dashboard filter returns 20 discrepant bookings with EUR 5,000 unresolved value  
**When** the user opens the linked detail list  
**Then** the same 20 bookings are returned  
**And** their displayed unresolved values total EUR 5,000 using the same conversion basis.

### AC-063 — Manager is read-only

**Given** a Read-only Manager views the dashboard and booking details  
**When** the user attempts to change a match, adjustment, booking, or discrepancy  
**Then** the action is rejected  
**And** no write occurs.

### AC-064 — Versioned export

**Given** a Finance user generates an accounting export  
**When** generation completes  
**Then** the export includes the selected records and required source/audit references  
**And** records format version, filters, row count, actor, time, and checksum.

### AC-065 — Export authorisation

**Given** an Operations user attempts to generate a financial export  
**When** the request is submitted  
**Then** it is rejected and audited.

---

## G. Operational Quality

### AC-070 — Bounded transient retry

**Given** a background import dependency fails transiently twice and succeeds on the third attempt  
**When** processing runs  
**Then** the job completes once  
**And** no duplicate effect occurs  
**And** retry metrics are recorded.

### AC-071 — Persistent job failure

**Given** a job fails on all permitted attempts  
**When** retries are exhausted  
**Then** the job enters a final failed state  
**And** the prior accepted financial state remains valid  
**And** a user can see diagnostic category and correlation identifier.

### AC-072 — Backup and restore evidence

**Given** a backup no older than 24 hours  
**When** the documented restore procedure is executed on the reference environment  
**Then** the service returns to a ready state within four hours  
**And** booking, financial, match, discrepancy, and audit counts match the backup manifest.

### AC-073 — Error correlation

**Given** a user-visible operation fails  
**When** the error is displayed  
**Then** it contains a stable error code and correlation identifier  
**And** support can locate the corresponding structured log without exposing restricted data.

---

## H. Performance Acceptance

### AC-080 — Booking-detail latency

**Given** the reference dataset and documented concurrent load  
**When** booking-detail requests are measured  
**Then** at least 95% complete within 2 seconds.

### AC-081 — Import throughput

**Given** a valid 10,000-row import  
**When** it is processed on the reference environment  
**Then** validation and persistence complete within 5 minutes  
**And** counts are correct.

### AC-082 — Reconciliation throughput

**Given** 10,000 affected eligible bookings  
**When** reconciliation runs  
**Then** it completes within 10 minutes  
**And** no deterministic result differs from a single-booking calculation.

---

## I. MVP Demonstration Scenario

The final demonstration shall include:

1. A direct booking with exact payment and supplier cost that reconciles automatically.
2. An OTA booking with settlement and commission that reconciles automatically.
3. A batched settlement allocated across several bookings.
4. A partial customer payment that remains partially reconciled.
5. A cancellation with retained fee and partial refund.
6. A supplier invoice increase that reopens a reconciled booking.
7. An ambiguous payment that cannot auto-match.
8. A duplicate import that creates no duplicate effect.
9. A cross-currency record with a documented rate.
10. A material short settlement that enters the discrepancy queue.
11. A timing difference that waits and returns when overdue.
12. A controlled accepted variance.
13. A forbidden Operations financial adjustment.
14. A successful export and audit reconstruction.
15. A backup and restore exercise.
