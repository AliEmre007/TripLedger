# TripLedger Domain Model

**Stage:** 4 - Domain Modeling and Architecture  
**Date:** 13 July 2026  
**Version:** 0.1  
**Status:** Domain baseline for validation release

## 1. Domain Boundary

TripLedger owns the financial-control view of tourism bookings. It does not own booking sale, payment execution, supplier fulfilment, statutory accounting, or tax filing.

Core domain question:

> For this booking, what was contracted, what was expected financially, what actually arrived or changed, what matched, what remains discrepant, and what evidence explains the result?

## 2. Bounded Contexts

| Context | Purpose | Owns |
|---|---|---|
| Identity and Access | Decide who may perform an action | User, role assignment, MFA policy, actor context |
| Source Ingestion | Convert external records into accepted canonical inputs | Source system, import batch, source record, row result |
| Booking Control | Represent the commercial booking and service items | Booking, booking item, lifecycle status |
| Supplier Cost Control | Represent expected and confirmed supplier costs | Supplier, supplier obligation, supplier credit |
| Financial Ledger Evidence | Represent received, deducted, refunded, adjusted, and reversed financial evidence | Financial event, reversal, adjustment, exchange rate |
| Economics Calculation | Produce expected booking economics | Calculation snapshot, calculation component |
| Matching and Allocation | Relate expected and actual financial records | Match, allocation line |
| Reconciliation | Determine booking financial status | Reconciliation run, reconciliation result |
| Discrepancy Management | Turn exceptions into reviewable work | Discrepancy, discrepancy event, comment |
| Audit and Timeline | Preserve material evidence | Audit event, timeline event |
| Export | Produce traceable external review packages | Export batch, export file manifest |

## 3. Aggregate Ownership

| Aggregate | Root | Main invariants |
|---|---|---|
| Organisation | Organisation | Settings apply only within organisation; base currency and materiality are explicit |
| Source System | SourceSystem | External code unique inside organisation; inactive sources reject new imports |
| Import Batch | ImportBatch | Every row gets a visible outcome; file-level failures create no domain rows |
| Booking | Booking | One canonical booking per source booking identity; confirmed bookings have items |
| Supplier Obligation | SupplierObligation | Positive obligation; reductions use credit/cancellation events |
| Financial Event | FinancialEvent | Immutable after acceptance; one external identity affects ledger once |
| Exchange Rate | ExchangeRate | Conversion evidence is immutable once referenced |
| Match | Match | Allocations cannot exceed available amount; manual matches require reason |
| Reconciliation Result | ReconciliationResult | Status is derived from accepted inputs and rule version |
| Discrepancy | Discrepancy | Unique active discrepancy per booking/type/component/cause |
| Audit Event | AuditEvent | Append-only through normal application path |

## 4. Entity Catalogue

### Money

Money is represented as exact decimal amount plus supported currency.

Validation-release rules:

- supported currencies are `EUR`, `GBP`, `TRY`, and `USD`;
- supported currencies allow two fractional digits;
- binary floating-point values are not used for persisted financial amounts;
- unsupported currencies and invalid precision are rejected before domain writes;
- database constraints also enforce the supported currency set and two-decimal precision on money-bearing tables.

### Organisation

The tenant and ownership boundary.

Fields:

- `id`
- `name`
- `base_currency`
- `materiality_threshold`
- `default_amount_tolerance`
- `default_date_window`
- `rounding_policy_version`
- `status`

Rules:

- Every business record belongs to exactly one organisation.
- Organisation settings are versioned when they affect calculations.

### User

Authenticated actor mapped from the identity provider.

Fields:

- `id`
- `organisation_id`
- `identity_subject`
- `display_name`
- `role`
- `status`
- `created_at`
- `deactivated_at`

Rules:

- One supported role per user in the MVP.
- Deactivation prevents new access but preserves historical audit identity.

### Source System

External or manual origin of imported records.

Fields:

- `id`
- `organisation_id`
- `name`
- `category`
- `external_code`
- `time_zone`
- `active`

Rules:

- `external_code` is unique per organisation.
- Deactivation stops new imports but not historical reads.

### Import Batch

One uploaded file or simulator run.

Fields:

- `id`
- `organisation_id`
- `source_system_id`
- `template_type`
- `template_version`
- `status`
- `file_checksum`
- `file_name`
- `received_by`
- `received_at`
- `completed_at`
- `failure_code`
- `failure_reason`
- `row_counts`

Rules:

- Batch status is `RECEIVED`, `COMPLETED`, or `FAILED`.
- Mixed accepted and rejected row outcomes use `COMPLETED_WITH_ERRORS`.
- Row outcomes are counted as `ACCEPTED`, `DUPLICATE`, `REJECTED`, or `FAILED`.
- Unsupported template version fails before domain writes.
- Valid rows may be accepted while invalid rows are rejected visibly.
- Rejected and failed rows carry row number, optional field, stable error code, and user-safe reason.

### Source Record

Preserved external identity and provenance for an accepted or duplicate row.

Fields:

- `id`
- `organisation_id`
- `source_system_id`
- `import_batch_id`
- `record_type`
- `external_record_id`
- `source_version`
- `source_row_number`
- `content_checksum`
- `payload_reference`
- `accepted_at`

Rules:

- Unique identity: `organisation + source_system + record_type + external_record_id + source_version`.
- Versionless sources use normalised content checksum for idempotency and require conflict review when changed.

### Booking

Canonical commercial booking.

Fields:

- `id`
- `organisation_id`
- `source_system_id`
- `external_booking_id`
- `current_source_record_id`
- `booking_date`
- `service_start_date`
- `service_end_date`
- `lifecycle_status`
- `selling_currency`
- `contracted_selling_amount`
- `customer_reference`

Rules:

- External booking id identifies one canonical booking per source system in an organisation.
- Newer source versions update the current canonical booking.
- Unchanged re-imports are counted as duplicates and do not create duplicate bookings or items.
- Older source versions are rejected as stale and do not overwrite the current booking.
- Booking detail reads expose the current source-record provenance and booking items inside the same organisation.
- Cancellation changes state and creates financial expectations; it does not delete history.

### Booking Item

Service component inside a booking.

Fields:

- `id`
- `organisation_id`
- `booking_id`
- `source_record_id`
- `item_external_id`
- `service_type`
- `service_start_date`
- `service_end_date`
- `selling_amount`
- `selling_currency`
- `supplier_id`
- `state`

Rules:

- End date cannot precede start date.
- Contracted gross sale is derived from original item amounts plus explicit reductions/corrections.

### Supplier

Supplier reference used for costs and obligations.

Fields:

- `id`
- `organisation_id`
- `name`
- `external_reference`
- `status`

### Supplier Obligation

Expected, confirmed, invoiced, credited, cancelled, or paid supplier cost.

Fields:

- `id`
- `organisation_id`
- `booking_id`
- `booking_item_id`
- `supplier_id`
- `source_record_id`
- `amount`
- `currency`
- `due_date`
- `status`
- `created_at`

Rules:

- Obligation amount is positive.
- Credits and cancellations reduce active supplier cost through separate records or versions.
- Unlinked obligations are visible but excluded from booking economics.
- Imported obligations preserve source-record provenance and duplicate source identities do not create duplicate obligations.
- Non-cancelled linked obligations contribute to active supplier cost; unlinked obligations do not.

### Financial Event

Accepted financial evidence.

Types:

- `CUSTOMER_PAYMENT`
- `CHANNEL_SETTLEMENT`
- `CHANNEL_COMMISSION`
- `PAYMENT_FEE`
- `REFUND`
- `SUPPLIER_PAYMENT`
- `SUPPLIER_CREDIT`
- `REVERSAL`
- `MANUAL_ADJUSTMENT`

Fields:

- `id`
- `organisation_id`
- `source_record_id`
- `booking_id`
- `event_type`
- `direction`
- `amount`
- `currency`
- `effective_at`
- `external_reference`
- `reverses_event_id`
- `adjustment_reason`

Rules:

- Accepted events are immutable.
- Reversal events link to the original event.
- Manual adjustments require reason and authority.
- Imported events preserve source-record provenance and duplicate source identities do not create duplicate financial effect.
- Events without a known booking link remain unmatched and are excluded from booking reconciliation until allocation.
- Corrections create a separate reversal event and optional replacement event; the original event remains unchanged.
- Only one reversal may exist for a given original event.
- Corrections use reversal/replacement or controlled adjustment.
- Event direction comes from event type, not user interpretation.
- Raw card data and unrestricted bank credentials are prohibited.

### Exchange Rate

Conversion evidence.

Fields:

- `id`
- `organisation_id`
- `source_currency`
- `target_currency`
- `rate`
- `effective_at`
- `rate_source`
- `rounding_policy_version`
- `created_by`

Rules:

- Different currencies cannot be added, compared, allocated, or reconciled without explicit conversion evidence.

### Calculation Snapshot

Versioned expected economics for a booking.

Fields:

- `id`
- `organisation_id`
- `booking_id`
- `rule_version`
- `contracted_gross_sale`
- `expected_customer_receivable`
- `expected_deductions`
- `active_supplier_cost`
- `estimated_gross_margin`
- `unknown_components`
- `created_at`

Rules:

- Missing required data is `UNKNOWN`, not zero.
- Every subtotal exposes formula, component records, currency treatment, and rule version.

### Match and Allocation Line

Explicit relationship between expected components and actual financial events.

Fields:

- `match.id`
- `organisation_id`
- `booking_id`
- `match_type`
- `rule_code`
- `created_by`
- `created_at`
- `status`
- `reason`
- `allocation_line.financial_event_id`
- `allocation_line.amount`
- `allocation_line.currency`

Rules:

- Auto-match only when exactly one candidate satisfies an approved rule.
- Sum of active allocations cannot exceed available financial event amount.
- Manual match/unmatch requires Finance or Administrator authority and a reason.

### Reconciliation Result

Derived booking financial state.

Statuses:

- `NOT_READY`
- `PENDING`
- `PARTIALLY_RECONCILED`
- `RECONCILED`
- `DISCREPANT`
- `CLOSED_WITH_VARIANCE`

Fields:

- `id`
- `organisation_id`
- `booking_id`
- `calculation_snapshot_id`
- `rule_version`
- `status`
- `variance_amount`
- `variance_currency`
- `created_at`
- `superseded_at`

Rules:

- A booking is reconciled only when all required components match within tolerance and no material discrepancy is open.
- Later accepted evidence reopens an invalidated result.

### Discrepancy

Explainable exception requiring review.

Types include:

- missing payment
- short settlement
- unmatched payment
- incorrect fee
- supplier variance
- refund mismatch
- duplicate source record
- exchange-rate variance
- timing difference
- invalid state
- ambiguous match

Fields:

- `id`
- `organisation_id`
- `booking_id`
- `type`
- `severity`
- `component`
- `cause_identity`
- `amount`
- `currency`
- `status`
- `owner_user_id`
- `explanation`
- `created_at`
- `resolved_at`

Rules:

- One active discrepancy per booking/type/component/cause identity.
- Accepting material variance cannot hide security or data-integrity defects.

### Audit Event

Append-only material action record.

Fields:

- `id`
- `organisation_id`
- `actor_user_id`
- `system_actor`
- `action`
- `target_type`
- `target_id`
- `outcome`
- `before_reference`
- `after_reference`
- `reason`
- `correlation_id`
- `created_at`

Rules:

- Financial and security-sensitive actions require audit.
- Audit events are not editable or deletable through normal application functions.

## 5. Critical Workflows

### Import booking source

1. Source system must exist and be active.
2. Import batch validates template and file.
3. Source record identity is calculated.
4. Duplicate source identity produces duplicate row result and no new financial effect.
5. New accepted source record creates or updates canonical booking according to version ordering.
6. Material booking changes mark the booking for recalculation.

### Calculate booking economics

1. Load active and historically contracted booking items.
2. Apply explicit discounts, cancellations, retained fees, refunds, fee expectations, supplier obligations, credits, and exchange rates.
3. If a required component is missing, set it to `UNKNOWN` and status cannot be `RECONCILED`.
4. Persist calculation snapshot with rule version and component references.

### Match and reconcile

1. Build expected components from calculation snapshot.
2. Search candidate financial events inside organisation, currency, amount tolerance, compatible type, and date window.
3. Auto-match only one deterministic candidate.
4. Refuse ambiguity and create review item.
5. Recalculate status and discrepancy state.

## 6. Domain Events for Timeline

These events are internal application events, not a requirement for an event-stream architecture.

- `ImportBatchReceived`
- `ImportRowAccepted`
- `ImportRowRejected`
- `BookingCreated`
- `BookingChanged`
- `SupplierObligationAccepted`
- `FinancialEventAccepted`
- `FinancialEventReversed`
- `CalculationCompleted`
- `MatchCreated`
- `MatchRemoved`
- `ReconciliationCompleted`
- `DiscrepancyOpened`
- `DiscrepancyResolved`
- `VarianceAccepted`
- `ExportGenerated`
- `AuthorisationDenied`

Events write audit/timeline records inside the same transaction when the action is material.
