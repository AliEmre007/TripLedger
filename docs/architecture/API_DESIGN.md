# TripLedger API Design

This document is the Stage 6 implementation-facing API map. The detailed Stage 4 API contract remains in `API_SPECIFICATION.md`.

## Current Implemented Endpoints

### `GET /api/v1/health`

Returns application-level service health.

Example response:

```json
{
  "status": "UP",
  "service": "tripledger",
  "version": "0.1.0-SNAPSHOT",
  "timestamp": "2026-07-13T12:00:00Z"
}
```

Every response includes `X-Correlation-Id`.

### `GET /api/v1/health/live`

Returns process liveness only. It does not check the database, migrations, or background job store.

Example response:

```json
{
  "status": "UP",
  "service": "tripledger",
  "version": "0.1.0-SNAPSHOT",
  "timestamp": "2026-07-16T08:00:00Z"
}
```

### `GET /api/v1/health/ready`

Returns dependency readiness and responds with HTTP 503 when a required dependency is not ready.

Checks:

- database connection;
- migration state;
- background job store.

Example response:

```json
{
  "status": "UP",
  "service": "tripledger",
  "version": "0.1.0-SNAPSHOT",
  "timestamp": "2026-07-16T08:00:00Z",
  "checks": [
    {
      "name": "database",
      "status": "UP",
      "detail": "Database connection is valid."
    }
  ]
}
```

Correlation id rules:

- A client may send `X-Correlation-Id`.
- Safe client values are trimmed and echoed back.
- Missing, blank, unsafe, or overlong values are replaced with a server-generated id.
- Accepted client values are limited to letters, numbers, `.`, `_`, `:`, and `-`, up to 128 characters.

### `GET /api/v1/me`

Returns the current protected actor context.

Current Stage 7 implementation resolves the actor through local development headers and validates that the actor exists as an active `app_user` in the requested organisation. This is the temporary adapter boundary for the future OIDC integration.

Required request headers until OIDC is integrated:

- `X-TripLedger-Actor-Subject`
- `X-TripLedger-Organisation-Id`

Optional request header:

- `X-TripLedger-Mfa-Satisfied`

Example response:

```json
{
  "userId": "uuid",
  "organisationId": "uuid",
  "displayName": "Finance User",
  "role": "FINANCE",
  "mfaSatisfied": true
}
```

Errors:

- `AUTHENTICATION_REQUIRED`
- `ORG_REFERENCE_MISMATCH`
- `INVALID_REQUEST`

## Current Authorisation Boundary

Protected application services use named permissions rather than ad hoc role checks.

Current permissions:

- `PROTECTED_READ`: Administrator, Finance, Operations, Read-only Manager.
- `SOURCE_SYSTEM_MANAGE`: Administrator.
- `OPERATIONAL_WRITE`: Administrator, Finance, Operations.
- `FINANCIAL_ACTION`: Administrator, Finance.
- `FINANCIAL_ACTION_WITH_MFA`: Administrator or Finance with MFA satisfied.

Stable denial codes:

- `UNAUTHORISED_FINANCIAL_ACTION` when the actor role is not permitted.
- `MFA_REQUIRED` when the role is permitted but MFA is not satisfied.

### `POST /api/v1/source-systems`

Creates a source system inside the actor organisation.

Roles:

- Administrator.

Request:

```json
{
  "name": "OTA Export",
  "category": "BOOKING_CHANNEL",
  "externalCode": "OTA_EXPORT",
  "timeZone": "Europe/Istanbul",
  "active": true
}
```

Rules:

- `externalCode` is normalised to uppercase.
- `externalCode` must be unique inside the actor organisation.
- `timeZone` must be a valid IANA time zone.

Errors:

- `DUPLICATE_SOURCE_CODE`
- `INVALID_REQUEST`
- `UNAUTHORISED_FINANCIAL_ACTION`

### `GET /api/v1/source-systems`

Lists source systems inside the actor organisation.

Roles:

- Administrator.
- Finance.
- Operations.
- Read-only Manager.

### `POST /api/v1/import-batches`

Starts an import batch for a source system inside the actor organisation.

Roles:

- Administrator.
- Finance.
- Operations.

Request:

```json
{
  "sourceSystemId": "uuid",
  "templateType": "BOOKING_CSV",
  "templateVersion": "v1",
  "fileName": "bookings.csv",
  "fileChecksum": "sha256:abc"
}
```

Rules:

- `sourceSystemId` must belong to the actor organisation.
- Inactive source systems cannot receive new imports.
- The batch starts in `RECEIVED` status with zero row counts.

Errors:

- `SOURCE_SYSTEM_NOT_FOUND`
- `INACTIVE_SOURCE_SYSTEM`
- `INVALID_REQUEST`
- `UNAUTHORISED_FINANCIAL_ACTION`

### `GET /api/v1/import-batches`

Lists import batches inside the actor organisation, newest first.

Roles:

- Administrator.
- Finance.
- Operations.
- Read-only Manager.

### `GET /api/v1/import-batches/{batchId}`

Returns one import batch inside the actor organisation.

Errors:

- `IMPORT_BATCH_NOT_FOUND`

### `POST /api/v1/import-batches/{batchId}/row-results`

Records a visible outcome for one imported row.

Roles:

- Administrator.
- Finance.
- Operations.

Request:

```json
{
  "rowNumber": 3,
  "outcome": "REJECTED",
  "fieldName": "sellingAmount",
  "errorCode": "INVALID_AMOUNT",
  "reason": "Amount is required.",
  "sourceRecordId": null
}
```

Rules:

- `outcome` is one of `ACCEPTED`, `DUPLICATE`, `REJECTED`, or `FAILED`.
- `rowNumber` is unique inside the batch.
- `REJECTED` and `FAILED` rows require `errorCode` and `reason`.
- Terminal batches cannot receive new row results.

Errors:

- `IMPORT_BATCH_NOT_FOUND`
- `DUPLICATE_IMPORT_ROW_RESULT`
- `IMPORT_BATCH_TERMINAL`
- `INVALID_REQUEST`

### `GET /api/v1/import-batches/{batchId}/row-results`

Lists row results for a batch inside the actor organisation, ordered by row number.

### `POST /api/v1/import-batches/{batchId}/complete`

Marks an open import batch as `COMPLETED`.

### `POST /api/v1/import-batches/{batchId}/fail`

Marks an open import batch as `FAILED`.

Request:

```json
{
  "errorCode": "UNSUPPORTED_TEMPLATE_VERSION",
  "reason": "Template version v9 is not supported."
}
```

### `GET /api/v1/jobs/{jobId}`

Returns visible background job state for one job inside the actor organisation.

Roles:

- Administrator.
- Finance.
- Operations.
- Read-only Manager.

Example response:

```json
{
  "id": "uuid",
  "organisationId": "uuid",
  "jobType": "IMPORT_PROCESSING",
  "status": "FAILED",
  "targetType": "IMPORT_BATCH",
  "targetId": "uuid",
  "requestedByUserId": "uuid",
  "maxAttempts": 3,
  "attemptCount": 3,
  "diagnosticCategory": "IMPORT_DEPENDENCY",
  "diagnosticMessage": "Import dependency unavailable.",
  "correlationId": "corr-123",
  "requestedAt": "2026-07-16T06:00:00Z",
  "lastAttemptAt": "2026-07-16T06:00:00Z",
  "nextAttemptAt": null,
  "completedAt": "2026-07-16T06:00:00Z"
}
```

Rules:

- `jobId` must belong to the actor organisation.
- Validation-release retry attempts are bounded to a maximum of three.
- Terminal failed jobs expose diagnostic category and correlation id.
- Missing or cross-organisation ids return `JOB_NOT_FOUND`.

### `POST /api/v1/booking-imports`

Imports booking CSV content and creates canonical booking records.

Roles:

- Administrator.
- Finance.
- Operations.

Request:

```json
{
  "sourceSystemId": "uuid",
  "fileName": "booking_valid_v1.csv",
  "fileChecksum": "sha256:abc",
  "csvContent": "template_type,template_version,..."
}
```

Rules:

- Supported booking template is `BOOKING` version `1`.
- Valid rows create or update one canonical booking and one booking item.
- Duplicate unchanged source identities are counted as `DUPLICATE`.
- Older source versions are rejected as `STALE_SOURCE_VERSION`.
- Mixed accepted and rejected rows finish as `COMPLETED_WITH_ERRORS`.

### `GET /api/v1/bookings/{bookingId}`

Returns canonical booking detail for one booking inside the actor organisation.

Roles:

- Administrator.
- Finance.
- Operations.
- Read-only Manager.

Example response:

```json
{
  "id": "uuid",
  "organisationId": "uuid",
  "sourceSystemId": "uuid",
  "externalBookingId": "TL-BKG-1001",
  "bookingDate": "2026-07-01",
  "serviceStartDate": "2026-08-01",
  "serviceEndDate": "2026-08-07",
  "lifecycleStatus": "CONFIRMED",
  "contractedSellingAmount": 1000.00,
  "sellingCurrency": "EUR",
  "customerReference": "CUST-1001",
  "currentSourceRecord": {
    "id": "uuid",
    "sourceSystemId": "uuid",
    "importBatchId": "uuid",
    "recordType": "BOOKING",
    "externalRecordId": "TL-BKG-1001",
    "sourceVersion": "1",
    "sourceRowNumber": 2,
    "contentChecksum": "sha256:abc",
    "payloadReference": null,
    "acceptedAt": "2026-07-14T06:00:00Z"
  },
  "items": [
    {
      "id": "uuid",
      "itemExternalId": "ITEM-1",
      "serviceType": "HOTEL",
      "serviceStartDate": "2026-08-01",
      "serviceEndDate": "2026-08-07",
      "sellingAmount": 1000.00,
      "sellingCurrency": "EUR",
      "state": "ACTIVE",
      "sourceRecord": {
        "id": "uuid",
        "sourceVersion": "1"
      }
    }
  ],
  "createdAt": "2026-07-14T06:00:00Z",
  "updatedAt": "2026-07-14T06:00:00Z"
}
```

Rules:

- The booking lookup is filtered by the actor organisation.
- Missing bookings and cross-organisation bookings return `BOOKING_NOT_FOUND`.
- The response includes source identity and checksum provenance, not raw CSV content.

Errors:

- `BOOKING_NOT_FOUND`
- `UNAUTHORISED_FINANCIAL_ACTION`
- Source identity is `organisation + source system + BOOKING + external booking id + source version`.
- Unchanged source identities are counted as duplicates and do not create duplicate bookings or items.
- Older source versions are rejected with `STALE_SOURCE_VERSION` and do not overwrite the current booking.
- Mixed valid and invalid rows complete the batch as `COMPLETED_WITH_ERRORS`.

Errors:

- `UNSUPPORTED_TEMPLATE_VERSION`
- `MISSING_REQUIRED_COLUMN`
- `MISSING_REQUIRED_FIELD`
- `INVALID_FIELD_TYPE`
- `INVALID_CURRENCY`
- `INVALID_CURRENCY_PRECISION`
- `INVALID_BOOKING_DATE`
- `STALE_SOURCE_VERSION`
- `SOURCE_SYSTEM_NOT_FOUND`
- `INACTIVE_SOURCE_SYSTEM`

### `POST /api/v1/supplier-obligation-imports`

Imports supplier-obligation CSV content and creates supplier cost records.

Roles:

- Administrator.
- Finance.
- Operations.

Request:

```json
{
  "sourceSystemId": "uuid",
  "fileName": "supplier_obligations.csv",
  "fileChecksum": "sha256:abc",
  "csvContent": "template_type,template_version,..."
}
```

Rules:

- Supported supplier-obligation template is `SUPPLIER_OBLIGATION` version `1`.
- Valid rows create one source record, one supplier when needed, and one supplier obligation.
- Source identity is `organisation + source system + SUPPLIER_OBLIGATION + external obligation id + source version`.
- Duplicate unchanged source identities are counted as `DUPLICATE`.
- Changed content for an existing source identity is rejected with `STALE_SOURCE_VERSION`.
- Amounts must be positive and use at most two fractional digits.
- Unknown booking or item references are accepted as unlinked obligations for review.
- Linked non-cancelled obligations expose `contributesToActiveSupplierCost=true`; unlinked obligations are excluded.

Errors:

- `UNSUPPORTED_TEMPLATE_VERSION`
- `MISSING_REQUIRED_COLUMN`
- `MISSING_REQUIRED_FIELD`
- `INVALID_FIELD_TYPE`
- `INVALID_CURRENCY`
- `INVALID_CURRENCY_PRECISION`
- `STALE_SOURCE_VERSION`
- `SOURCE_SYSTEM_NOT_FOUND`
- `INACTIVE_SOURCE_SYSTEM`

### `GET /api/v1/supplier-obligations`

Lists supplier obligations inside the actor organisation.

Query parameters:

- `unlinked=true` returns only obligations with no booking or booking-item link.

Roles:

- Administrator.
- Finance.
- Operations.
- Read-only Manager.

Rules:

- Results are filtered by the actor organisation.
- Responses include source-record provenance and supplier reference/name.
- Raw source payload content is not returned.

### `POST /api/v1/financial-event-imports`

Imports financial-event CSV content and creates immutable financial evidence records.

Roles:

- Administrator with MFA.
- Finance with MFA.

Request:

```json
{
  "sourceSystemId": "uuid",
  "fileName": "financial_events.csv",
  "fileChecksum": "sha256:abc",
  "csvContent": "template_type,template_version,..."
}
```

Rules:

- Supported financial-event template is `FINANCIAL_EVENT` version `1`.
- Valid rows create one source record and one immutable financial event.
- Source identity is `organisation + source system + FINANCIAL_EVENT + external event id + source version`.
- Duplicate unchanged source identities are counted as `DUPLICATE`.
- Changed content for an existing source identity is rejected with `STALE_SOURCE_VERSION`.
- Amounts must be positive and use at most two fractional digits.
- Supported validation-release currencies are `EUR`, `GBP`, `TRY`, and `USD`.
- `APPROVED_DISCOUNT` is accepted as an economics reduction event and derives a `DECREASE_RECEIVED` direction.
- Unknown or missing booking references are accepted as unmatched events for review.
- Raw card numbers, CVV values, and unrestricted payment credentials are rejected.

Errors:

- `UNSUPPORTED_TEMPLATE_VERSION`
- `MISSING_REQUIRED_COLUMN`
- `MISSING_REQUIRED_FIELD`
- `INVALID_FIELD_TYPE`
- `INVALID_CURRENCY`
- `INVALID_CURRENCY_PRECISION`
- `PROHIBITED_PAYMENT_DATA`
- `STALE_SOURCE_VERSION`
- `SOURCE_SYSTEM_NOT_FOUND`
- `INACTIVE_SOURCE_SYSTEM`
- `MFA_REQUIRED`
- `UNAUTHORISED_FINANCIAL_ACTION`

### `GET /api/v1/financial-events`

Lists financial events inside the actor organisation.

Query parameters:

- `unmatched=true` returns only events with no booking link.

Roles:

- Administrator.
- Finance.
- Operations.
- Read-only Manager.

Rules:

- Results are filtered by the actor organisation.
- Responses include event type, direction, amount, currency, effective timestamp, source-record provenance, and booking link state.
- Raw source payload content and prohibited payment data are not returned.

### `POST /api/v1/financial-events/{eventId}/reversal`

Creates a full reversal for an accepted financial event and optionally creates a replacement event.

Roles:

- Administrator with MFA.
- Finance with MFA.

Request:

```json
{
  "reason": "Gateway corrected duplicate payment.",
  "effectiveAt": "2026-07-15T09:00:00Z",
  "replacementEvent": {
    "eventType": "CUSTOMER_PAYMENT",
    "amount": 900.00,
    "currency": "EUR",
    "effectiveAt": "2026-07-15T09:05:00Z",
    "externalReference": "PAY-1001-CORRECTED"
  }
}
```

Rules:

- The original event is never updated or deleted.
- The reversal uses the original amount, currency, booking link, and a `reversesEventId` link.
- A financial event can have only one reversal.
- Reversal events cannot be reversed through this path.
- Replacement events are optional and are stored as new accepted financial events.
- A reason is required for auditability.

Errors:

- `FINANCIAL_EVENT_NOT_FOUND`
- `FINANCIAL_EVENT_ALREADY_REVERSED`
- `INVALID_FINANCIAL_REVERSAL`
- `INVALID_REQUEST`
- `INVALID_CURRENCY`
- `INVALID_CURRENCY_PRECISION`
- `MFA_REQUIRED`
- `UNAUTHORISED_FINANCIAL_ACTION`

### `POST /api/v1/exchange-rate-evidence`

Creates explicit exchange-rate evidence for a cross-currency financial record or calculation input.

Roles:

- Administrator with MFA.
- Finance with MFA.

Request:

```json
{
  "financialEventId": "uuid",
  "sourceAmount": 3500.00,
  "sourceCurrency": "TRY",
  "targetCurrency": "EUR",
  "rate": 0.0285714286,
  "effectiveAt": "2026-07-10T09:00:00Z",
  "rateSource": "manual-finance-evidence",
  "roundingPolicyVersion": "HALF_UP-v1"
}
```

Rules:

- Source and target currencies must be supported validation-release currencies and must differ.
- Rate must be positive and may use up to 12 fractional digits.
- `targetAmount` is calculated with exact decimal multiplication and target-currency rounding, then persisted as the rounding result.
- When `financialEventId` is supplied, the event must belong to the actor organisation and the source amount/currency must match that event.
- Cross-organisation financial-event ids return `FINANCIAL_EVENT_NOT_FOUND`.

Errors:

- `FINANCIAL_EVENT_NOT_FOUND`
- `INVALID_EXCHANGE_RATE`
- `INVALID_CURRENCY`
- `INVALID_CURRENCY_PRECISION`
- `MFA_REQUIRED`
- `UNAUTHORISED_FINANCIAL_ACTION`

### `GET /api/v1/exchange-rate-evidence`

Lists exchange-rate evidence inside the actor organisation.

Query parameters:

- `financialEventId=uuid` returns evidence for one financial event.

Roles:

- Administrator.
- Finance.
- Operations.
- Read-only Manager.

### `GET /api/v1/bookings/{bookingId}/economics`

Calculates and returns the current booking economics snapshot.

Roles:

- Administrator.
- Finance.
- Operations.
- Read-only Manager.

Example response:

```json
{
  "snapshotId": "uuid",
  "bookingId": "uuid",
  "ruleVersion": "economics-v1",
  "currency": "EUR",
  "contractedGrossSale": 1000.00,
  "expectedCustomerReceivable": 950.00,
  "expectedDeductions": 162.50,
  "activeSupplierCost": 500.00,
  "estimatedGrossMargin": 287.50,
  "status": "READY",
  "unknownComponents": [],
  "createdAt": "2026-07-16T06:00:00Z"
}
```

Rules:

- Contracted gross sale comes from the current canonical booking amount.
- `APPROVED_DISCOUNT` and `REFUND` events reduce expected customer receivable.
- `CHANNEL_COMMISSION` and `PAYMENT_FEE` events produce expected deductions.
- Confirmed, invoiced, and paid supplier obligations produce active supplier cost.
- Missing supplier cost or missing exchange-rate evidence returns `status=NOT_READY` and leaves incomplete values null rather than substituting zero.
- Cross-organisation booking ids return `BOOKING_NOT_FOUND`.

Errors:

- `BOOKING_NOT_FOUND`
- `UNAUTHORISED_FINANCIAL_ACTION`

### `GET /api/v1/bookings/{bookingId}/economics/explanation`

Returns the calculation explanation for the current booking economics inputs.

Roles:

- Administrator.
- Finance.
- Operations.
- Read-only Manager.

Example response:

```json
{
  "bookingId": "uuid",
  "ruleVersion": "economics-v1",
  "currency": "EUR",
  "formulas": [
    {
      "subtotal": "expectedCustomerReceivable",
      "formula": "contractedGrossSale - approvedDiscounts - expectedCustomerRefunds - waivedAmounts",
      "ruleReference": "BR-ECO-002"
    }
  ],
  "components": [
    {
      "componentType": "APPROVED_DISCOUNT",
      "subtotal": "expectedCustomerReceivable",
      "sourceTable": "financial_event",
      "sourceId": "uuid",
      "sourceRecordId": "uuid",
      "originalAmount": 3500.00,
      "originalCurrency": "TRY",
      "amount": 100.00,
      "currency": "EUR",
      "exchangeRateId": "uuid",
      "formulaReference": "BR-ECO-002"
    }
  ],
  "exchangeRates": [
    {
      "id": "uuid",
      "financialEventId": "uuid",
      "sourceAmount": 3500.00,
      "sourceCurrency": "TRY",
      "targetAmount": 100.00,
      "targetCurrency": "EUR",
      "rate": 0.028571428600,
      "effectiveAt": "2026-07-10T09:00:00Z",
      "rateSource": "manual-finance-evidence",
      "roundingPolicyVersion": "HALF_UP-v1"
    }
  ],
  "rounding": "HALF_UP to target currency minor unit"
}
```

Rules:

- The response is filtered by actor organisation.
- Formulas expose business-rule references.
- Components expose source table, source id, source record id, original currency, calculation currency, and exchange-rate id when conversion evidence is used.
- Missing cross-currency evidence appears as a component with null converted amount/currency and no exchange-rate id.

Errors:

- `BOOKING_NOT_FOUND`
- `UNAUTHORISED_FINANCIAL_ACTION`

### `POST /api/v1/bookings/{bookingId}/matching-runs`

Runs the validation-release deterministic one-to-one matcher for one booking.

Roles:

- Administrator.
- Finance.

Example response:

```json
{
  "bookingId": "uuid",
  "status": "ACTIVE",
  "ruleCode": "EXACT_BOOKING_AMOUNT",
  "matchId": "uuid",
  "financialEventId": "uuid",
  "amount": 950.00,
  "currency": "EUR",
  "exchangeRateId": null,
  "originalAmount": 950.00,
  "originalCurrency": "EUR",
  "reason": null
}
```

Rules:

- The matcher compares the booking expected customer receivable with compatible received financial events.
- Compatible validation-release event types are `CUSTOMER_PAYMENT` and `CHANNEL_SETTLEMENT`.
- Direct matches require the same currency and exact amount.
- Cross-currency matches require exchange-rate evidence whose converted target amount equals the expected amount.
- Already allocated financial events are ignored.
- Exactly one candidate creates an `ACTIVE` automatic match and allocation.
- Multiple candidates create a `REVIEW_REQUIRED` match record with `AMBIGUOUS_MATCH` and no allocation.
- No valid candidate returns `REVIEW_REQUIRED` without allocation.

Errors:

- `BOOKING_NOT_FOUND`
- `UNAUTHORISED_FINANCIAL_ACTION`

### `POST /api/v1/bookings/{bookingId}/reconciliation-runs`

Runs the reconciliation state engine for one booking.

Roles:

- Administrator.
- Finance.

Example response:

```json
{
  "id": "uuid",
  "bookingId": "uuid",
  "calculationSnapshotId": "uuid",
  "ruleVersion": "reconciliation-v1",
  "status": "RECONCILED",
  "expectedAmount": 950.00,
  "matchedAmount": 950.00,
  "varianceAmount": 0.00,
  "currency": "EUR",
  "createdAt": "2026-07-16T07:00:00Z"
}
```

Rules:

- `NOT_READY` is returned when required economics are incomplete.
- `RECONCILED` requires the expected customer receivable to be fully matched.
- `PARTIALLY_RECONCILED` is returned when some amount is matched and some remains unmatched.
- `DISCREPANT` is returned when current match evidence includes a review-required state such as `AMBIGUOUS_MATCH`.
- A new run supersedes the previous current reconciliation result, preserving prior results for audit.
- Re-running unchanged inputs produces equivalent status and totals and does not create duplicate financial allocations.
- Material short settlement creates a `SHORT_SETTLEMENT` discrepancy.
- Ambiguous match evidence creates an `AMBIGUOUS_MATCH` discrepancy.
- Existing active discrepancies with the same booking, type, component, and cause are reused instead of duplicated.

Errors:

- `BOOKING_NOT_FOUND`
- `UNAUTHORISED_FINANCIAL_ACTION`

### `GET /api/v1/discrepancies`

Lists discrepancies inside the actor organisation.

Roles:

- Administrator.
- Finance.
- Operations.
- Read-only Manager.

Query filters:

- `status`: `ACTIVE` or `RESOLVED`.
- `type`: `SHORT_SETTLEMENT` or `AMBIGUOUS_MATCH`.
- `severity`: `LOW`, `MEDIUM`, `HIGH`, or `CRITICAL`.
- `ownerUserId`: assigned user id.
- `currency`: three-letter currency code.
- `page`: zero-based page number, default `0`.
- `size`: page size from `1` to `100`, default `50`.

Example response:

```json
{
  "items": [
    {
      "id": "uuid",
      "organisationId": "uuid",
      "bookingId": "uuid",
      "type": "SHORT_SETTLEMENT",
      "severity": "HIGH",
      "component": "EXPECTED_CUSTOMER_RECEIVABLE",
      "amount": 50.00,
      "currency": "EUR",
      "status": "ACTIVE",
      "ownerUserId": null,
      "explanation": "Expected EUR 850.00 but matched EUR 800.00; variance EUR 50.00.",
      "ageDays": 2,
      "createdAt": "2026-07-14T10:00:00Z",
      "resolvedAt": null
    }
  ],
  "summary": {
    "totalCount": 1,
    "activeCount": 1,
    "resolvedCount": 0,
    "totalAmount": 50.00
  },
  "page": 0,
  "size": 50,
  "totalItems": 1,
  "totalPages": 1
}
```

Rules:

- Results and summary counts are filtered by the actor organisation and the same supplied filters.
- Invalid filter values return `INVALID_REQUEST`.

### `GET /api/v1/discrepancies/{discrepancyId}`

Returns one discrepancy inside the actor organisation with available investigation evidence.

Rules:

- Missing and cross-organisation ids return `DISCREPANCY_NOT_FOUND`.
- Validation-release detail includes generated cause identity, explanation, owner/status state, amount/currency evidence, age, and related booking evidence when the discrepancy is booking-linked.
- Raw source payloads are not returned.

Errors:

- `DISCREPANCY_NOT_FOUND`
- `INVALID_REQUEST`

### `GET /api/v1/bookings/{bookingId}/timeline`

Returns a chronological booking timeline inside the actor organisation.

Roles:

- Administrator.
- Finance.
- Operations.
- Read-only Manager.

Example response:

```json
{
  "bookingId": "uuid",
  "organisationId": "uuid",
  "events": [
    {
      "id": "uuid",
      "occurredAt": "2026-07-16T08:00:00Z",
      "category": "SOURCE",
      "eventType": "FINANCIAL_EVENT_ACCEPTED",
      "title": "Financial event accepted",
      "summary": "CUSTOMER_PAYMENT EUR 950.00 accepted.",
      "targetType": "FINANCIAL_EVENT",
      "targetId": "uuid",
      "actorUserId": null,
      "amount": 950.00,
      "currency": "EUR",
      "status": "CUSTOMER_PAYMENT",
      "evidenceReference": "financial_event:uuid"
    }
  ]
}
```

Rules:

- Events are ordered chronologically.
- `SOURCE` events represent accepted imported/source evidence.
- `SYSTEM` events represent derived calculations, matches, reconciliation results, and discrepancies.
- `USER` events represent user-controlled financial actions recorded through audit events or user-created financial evidence.
- Missing and cross-organisation booking ids return `BOOKING_NOT_FOUND`.
- Raw source payloads are not returned.

Errors:

- `BOOKING_NOT_FOUND`

### Actuator

- `GET /actuator/health`
- `GET /actuator/health/liveness`
- `GET /actuator/health/readiness`
- `GET /actuator/metrics`

Current application metrics include:

- `tripledger.http.requests`
- `tripledger.http.errors`
- `tripledger.job.retries`

## Error Model

Application errors use this shape:

```json
{
  "error": {
    "code": "INVALID_REQUEST",
    "message": "Request validation failed.",
    "correlationId": "uuid",
    "timestamp": "2026-07-13T12:00:00Z",
    "details": []
  }
}
```

Rules:

- Errors must include a stable code.
- Errors must include a correlation id.
- Messages must be safe for users.
- Restricted data, source payloads, secrets, and tokens must not appear in errors.

Common framework-level error codes:

- `INVALID_REQUEST`: validation failure, malformed JSON, missing required request parameter.
- `METHOD_NOT_ALLOWED`: HTTP method is not supported for the endpoint.
- `UNSUPPORTED_MEDIA_TYPE`: request content type is not supported for the endpoint.
- `NOT_FOUND`: endpoint was not found.
- `INTERNAL_ERROR`: unexpected server failure with a safe user message.

## Current Money Policy

Validation-release money handling is centralised in the application money policy.

Rules:

- Persisted and returned financial amounts use exact decimal values, not binary floating point.
- Supported validation-release currencies are `EUR`, `GBP`, `TRY`, and `USD`.
- Supported currencies currently allow two fractional digits.
- Amount precision failures return `INVALID_CURRENCY_PRECISION`.
- Unsupported currencies return `INVALID_CURRENCY`.
- Negative imported amounts are rejected unless a later explicit correction path supports the event contract.

## Future API Rules

- Use `/api/v1`.
- Enforce server-side authorisation for every protected endpoint.
- Derive organisation from authenticated actor context.
- Do not reveal cross-organisation object existence.
- Return stable business-rule error codes from `BUSINESS_RULES.md`.
- Require MFA for Administrator and Finance financial functions.

See `API_SPECIFICATION.md` before adding or changing API contracts.
