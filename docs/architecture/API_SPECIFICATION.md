# TripLedger API Specification

**Stage:** 4 - Domain Modeling and Architecture  
**Date:** 13 July 2026  
**Version:** 0.1  
**Status:** API contract baseline for validation release  
**Base path:** `/api/v1`

## 1. API Principles

1. JSON over HTTPS for application APIs.
2. All endpoints are authenticated except login callback handling and health checks.
3. Authorisation is enforced server-side for every protected request.
4. Organisation scope comes from authenticated context and is checked against every object.
5. Financial write operations require Finance or Administrator role and valid MFA state where required.
6. Import and reconciliation operations are idempotent where business rules require it.
7. Errors return stable codes and correlation ids.
8. API version changes when public contract compatibility breaks.

## 2. Common Headers

| Header | Direction | Required | Purpose |
|---|---|---|---|
| `Authorization: Bearer <token>` | request | protected endpoints | OIDC access token |
| `X-Correlation-Id` | request | optional | Client-provided correlation id; server creates one if absent or unsafe |
| `Idempotency-Key` | request | write endpoints where specified | Prevent duplicate client retries |
| `X-Correlation-Id` | response | always | Correlate user errors to logs |

Client-provided correlation ids are trimmed before use. Accepted values are limited to letters, numbers, `.`, `_`, `:`, and `-`, up to 128 characters. Blank, unsafe, or overlong values are replaced with a server-generated id.

## 3. Common Error Model

```json
{
  "error": {
    "code": "INVALID_CURRENCY_PRECISION",
    "message": "Amount has more fractional digits than allowed for EUR.",
    "correlationId": "01JZ...",
    "details": [
      {
        "field": "amount",
        "reason": "EUR supports 2 fractional digits."
      }
    ]
  }
}
```

Rules:

- `message` is safe for users and must not reveal restricted data.
- `code` maps to `BUSINESS_RULES.md` or an API-specific validation code.
- Cross-organisation access returns a non-success response without confirming target existence.

Common API-specific validation codes:

- `INVALID_REQUEST`: request validation failed, JSON is malformed, or a required request parameter is missing.
- `METHOD_NOT_ALLOWED`: HTTP method is not supported for the endpoint.
- `UNSUPPORTED_MEDIA_TYPE`: request content type is not supported for the endpoint.
- `NOT_FOUND`: endpoint was not found.
- `INTERNAL_ERROR`: unexpected server failure with a safe user message.

Validation-release money policy:

- Supported currencies are `EUR`, `GBP`, `TRY`, and `USD`.
- Supported currencies use two fractional digits.
- Unsupported currency values return `INVALID_CURRENCY`.
- Too many fractional digits return `INVALID_CURRENCY_PRECISION`.
- Imported booking, supplier-obligation, and financial-event amounts are exact decimals.

## 4. Authentication and User Context

### `GET /me`

Returns current user context.

Roles allowed: all authenticated users.

Response:

```json
{
  "userId": "uuid",
  "organisationId": "uuid",
  "displayName": "Finance User",
  "role": "FINANCE",
  "mfaSatisfied": true
}
```

## 5. Source Systems

### `POST /source-systems`

Creates a source system.

Roles: Administrator.

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

Errors:

- `DUPLICATE_SOURCE_CODE`
- `UNAUTHORISED_FINANCIAL_ACTION` when role is invalid for protected source management.

### `GET /source-systems`

Lists organisation source systems.

Roles: Administrator, Finance, Operations, Read-only Manager.

## 6. Import Batches

### `POST /import-batches`

Starts an import batch for a registered source system. CSV upload and parsing are added by the specific importer slices.

Roles: Administrator, Finance, Operations.

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

Response:

```json
{
  "id": "uuid",
  "organisationId": "uuid",
  "sourceSystemId": "uuid",
  "templateType": "BOOKING_CSV",
  "templateVersion": "v1",
  "status": "RECEIVED",
  "fileName": "bookings.csv",
  "fileChecksum": "sha256:abc",
  "receivedByUserId": "uuid",
  "receivedAt": "2026-07-14T06:00:00Z",
  "completedAt": null,
  "failureCode": null,
  "failureReason": null,
  "totalCount": 0,
  "acceptedCount": 0,
  "duplicateCount": 0,
  "rejectedCount": 0,
  "failedCount": 0
}
```

Errors:

- `SOURCE_SYSTEM_NOT_FOUND`
- `INACTIVE_SOURCE_SYSTEM`
- `INVALID_REQUEST`
- `UNAUTHORISED_FINANCIAL_ACTION`

### `GET /import-batches`

Lists import batches inside the actor organisation, newest first.

Roles: Administrator, Finance, Operations, Read-only Manager.

### `GET /import-batches/{importBatchId}`

Returns import batch status and counts.

Roles: Administrator, Finance, Operations, Read-only Manager.

Errors:

- `IMPORT_BATCH_NOT_FOUND`

### `POST /import-batches/{importBatchId}/row-results`

Records one row outcome for an open batch.

Roles: Administrator, Finance, Operations.

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
- `rowNumber` is unique per batch.
- `REJECTED` and `FAILED` rows require `errorCode` and `reason`.
- Terminal batches cannot be changed.

Errors:

- `IMPORT_BATCH_NOT_FOUND`
- `DUPLICATE_IMPORT_ROW_RESULT`
- `IMPORT_BATCH_TERMINAL`
- `INVALID_REQUEST`

### `GET /import-batches/{importBatchId}/row-results`

Returns row-level results ordered by row number.

### `POST /import-batches/{importBatchId}/complete`

Marks an open batch as `COMPLETED`.

### `POST /import-batches/{importBatchId}/fail`

Marks an open batch as `FAILED`.

Request:

```json
{
  "errorCode": "UNSUPPORTED_TEMPLATE_VERSION",
  "reason": "Template version v9 is not supported."
}
```

### `POST /booking-imports`

Imports booking CSV content through the import-batch lifecycle.

Roles: Administrator, Finance, Operations.

Request:

```json
{
  "sourceSystemId": "uuid",
  "fileName": "booking_valid_v1.csv",
  "fileChecksum": "sha256:abc",
  "csvContent": "template_type,template_version,..."
}
```

Response:

```json
{
  "importBatchId": "uuid",
  "status": "COMPLETED",
  "totalCount": 3,
  "acceptedCount": 3,
  "duplicateCount": 0,
  "rejectedCount": 0,
  "failedCount": 0,
  "completedAt": "2026-07-14T06:00:00Z"
}
```

Rules:

- Supported booking CSV rows use `template_type=BOOKING` and `template_version=1`.
- Valid rows create or update canonical booking and booking-item records.
- Duplicate unchanged source identities are counted as `DUPLICATE`.
- Older source versions are rejected as `STALE_SOURCE_VERSION`.
- Rows with invalid fields are rejected with row-level field, error code, and reason.
- Batches with accepted and rejected rows finish as `COMPLETED_WITH_ERRORS`.

Errors:

- `UNSUPPORTED_TEMPLATE_VERSION`
- `MISSING_REQUIRED_COLUMN`
- `MISSING_REQUIRED_FIELD`
- `INVALID_FIELD_TYPE`
- `INVALID_CURRENCY`
- `INVALID_CURRENCY_PRECISION`
- `INVALID_BOOKING_DATE`
- `STALE_SOURCE_VERSION`

## 7. Bookings

### `GET /bookings`

Searches bookings.

Query:

- `status`
- `reconciliationStatus`
- `sourceSystemId`
- `externalBookingId`
- `serviceFrom`
- `serviceTo`
- `page`
- `size`

Roles: Administrator, Finance, Operations, Read-only Manager.

### `GET /bookings/{bookingId}`

Returns canonical booking detail, booking items, and source provenance. Economics,
reconciliation status, and active discrepancies are added by later slices.

Roles: Administrator, Finance, Operations, Read-only Manager.

Response:

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
        "sourceSystemId": "uuid",
        "importBatchId": "uuid",
        "recordType": "BOOKING",
        "externalRecordId": "TL-BKG-1001",
        "sourceVersion": "1",
        "sourceRowNumber": 2,
        "contentChecksum": "sha256:abc",
        "payloadReference": null,
        "acceptedAt": "2026-07-14T06:00:00Z"
      }
    }
  ],
  "createdAt": "2026-07-14T06:00:00Z",
  "updatedAt": "2026-07-14T06:00:00Z"
}
```

Rules:

- Lookup is scoped to the actor organisation.
- Cross-organisation access returns `BOOKING_NOT_FOUND` without confirming target existence.
- Provenance includes source identity, import batch id, row number, source version, and content checksum.
- Raw source payload content is not returned.

Errors:

- `BOOKING_NOT_FOUND`
- `UNAUTHORISED_FINANCIAL_ACTION`

### `PATCH /bookings/{bookingId}/operational-fields`

Updates permitted operational fields.

Roles: Administrator, Finance, Operations.

Rules:

- Cannot edit imported financial events.
- Material change triggers recalculation and audit.

Errors:

- `INVALID_BOOKING_TRANSITION`
- `UNAUTHORISED_FINANCIAL_ACTION`

## 8. Supplier Obligations

### `POST /supplier-obligation-imports`

Imports supplier-obligation CSV content.

Roles: Administrator, Finance, Operations.

Request:

```json
{
  "sourceSystemId": "uuid",
  "fileName": "supplier_obligations.csv",
  "fileChecksum": "sha256:abc",
  "csvContent": "template_type,template_version,..."
}
```

Response:

```json
{
  "importBatchId": "uuid",
  "status": "COMPLETED",
  "totalCount": 2,
  "acceptedCount": 2,
  "duplicateCount": 0,
  "rejectedCount": 0,
  "failedCount": 0,
  "completedAt": "2026-07-14T06:00:00Z"
}
```

Rules:

- Supported template is `SUPPLIER_OBLIGATION` version `1`.
- Valid rows create supplier obligations with source-record provenance.
- Supplier references are upserted inside the actor organisation.
- Unknown booking or item references are accepted as unlinked obligations.
- Unlinked obligations are excluded from booking economics.
- Duplicate unchanged source identities produce row outcome `DUPLICATE` and no duplicate obligation.
- Amount must be positive and use at most two fractional digits.

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

### `GET /supplier-obligations`

Returns supplier obligations for review.

Query parameters:

- `unlinked`: optional boolean. When `true`, returns only obligations excluded from booking economics because no booking or booking item is linked.

Roles: Administrator, Finance, Operations, Read-only Manager.

Response:

```json
[
  {
    "id": "uuid",
    "organisationId": "uuid",
    "bookingId": null,
    "bookingItemId": null,
    "supplierId": "uuid",
    "supplierReference": "TOUR-NOVA",
    "supplierName": "Tour Nova",
    "amount": 75.00,
    "currency": "EUR",
    "dueDate": "2026-08-12",
    "status": "EXPECTED",
    "linkedToBookingEconomics": false,
    "contributesToActiveSupplierCost": false,
    "sourceRecord": {
      "id": "uuid",
      "recordType": "SUPPLIER_OBLIGATION",
      "externalRecordId": "SUP-OBL-UNLINKED-1",
      "sourceVersion": "1"
    },
    "createdAt": "2026-07-14T06:00:00Z"
  }
]
```

Rules:

- Lookup is scoped to the actor organisation.
- Source provenance is included; raw CSV payload content is not returned.
- `contributesToActiveSupplierCost` is false for unlinked obligations.

### `GET /bookings/{bookingId}/supplier-obligations`

Deferred beyond VR-010. Booking economics and booking-scoped supplier-cost views are added with later economics slices.

### `POST /supplier-obligations/{obligationId}/credits`

Deferred beyond VR-010. Creates a supplier credit or cancellation event.

Roles: Administrator, Finance; Operations only before finance control when allowed.

Errors:

- `SUPPLIER_OVERALLOCATION`
- `UNAUTHORISED_FINANCIAL_ACTION`

## 9. Financial Events

### `POST /financial-event-imports`

Imports financial-event CSV content.

Roles: Administrator or Finance with MFA.

Request:

```json
{
  "sourceSystemId": "uuid",
  "fileName": "financial_events.csv",
  "fileChecksum": "sha256:abc",
  "csvContent": "template_type,template_version,..."
}
```

Response:

```json
{
  "importBatchId": "uuid",
  "status": "COMPLETED",
  "totalCount": 6,
  "acceptedCount": 6,
  "duplicateCount": 0,
  "rejectedCount": 0,
  "failedCount": 0,
  "completedAt": "2026-07-14T06:00:00Z"
}
```

Rules:

- Supported template is `FINANCIAL_EVENT` version `1`.
- Accepted rows create immutable financial events with source-record provenance.
- Event direction is derived from event type.
- Unknown or missing booking references are accepted as unmatched events.
- Unmatched events do not contribute to booking reconciliation until allocated by later slices.
- Duplicate unchanged source identities produce row outcome `DUPLICATE` and no duplicate financial effect.
- Amount must be positive and use at most two fractional digits.
- Validation-release supported currencies are `EUR`, `GBP`, `TRY`, and `USD`.

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

### `GET /financial-events`

Searches financial events.

Roles: Administrator, Finance, Operations, Read-only Manager.

Query:

- `bookingId`
- `eventType`
- `unmatched`
- `sourceSystemId`
- `effectiveFrom`
- `effectiveTo`
- `page`
- `size`

Rules:

- Lookup is scoped to the actor organisation.
- `unmatched=true` returns only accepted events with no booking link.
- Source provenance is included; raw source payload content is not returned.

### `POST /financial-events/{eventId}/reversal`

Creates a reversal and optional replacement event.

Roles: Administrator or Finance with MFA.

Request:

```json
{
    "reason": "Corrected source file from gateway",
    "effectiveAt": "2026-07-15T09:00:00Z",
    "replacementEvent": {
        "eventType": "CUSTOMER_PAYMENT",
        "amount": "950.00",
    "currency": "EUR",
    "effectiveAt": "2026-07-13T10:00:00Z",
    "externalReference": "PAY-123-CORRECTED"
  }
}
```

Rules:

- The original accepted event is not updated or deleted.
- The reversal is a new `REVERSAL` event linked to the original event by `reversesEventId`.
- The reversal uses the original amount, currency, and booking link so the net effect can be audited.
- Only one reversal may exist for an original event.
- Replacement event is optional and is stored as a separate accepted event.
- Reason is required.

Errors:

- `FINANCIAL_EVENT_NOT_FOUND`
- `FINANCIAL_EVENT_ALREADY_REVERSED`
- `INVALID_FINANCIAL_REVERSAL`
- `INVALID_MANUAL_ADJUSTMENT`
- `INVALID_REQUEST`
- `INVALID_CURRENCY`
- `INVALID_CURRENCY_PRECISION`
- `MFA_REQUIRED`
- `UNAUTHORISED_FINANCIAL_ACTION`

## 10. Economics

### `POST /exchange-rate-evidence`

Creates explicit exchange-rate evidence for cross-currency workflows.

Roles: Administrator or Finance with MFA.

Request:

```json
{
  "financialEventId": "uuid",
  "sourceAmount": "3500.00",
  "sourceCurrency": "TRY",
  "targetCurrency": "EUR",
  "rate": "0.0285714286",
  "effectiveAt": "2026-07-10T09:00:00Z",
  "rateSource": "manual-finance-evidence",
  "roundingPolicyVersion": "HALF_UP-v1"
}
```

Response includes source amount/currency, target amount/currency, rate, effective time, rate source, rounding policy, actor, and creation timestamp.

Errors:

- `FINANCIAL_EVENT_NOT_FOUND`
- `INVALID_EXCHANGE_RATE`
- `INVALID_CURRENCY`
- `INVALID_CURRENCY_PRECISION`
- `MFA_REQUIRED`
- `UNAUTHORISED_FINANCIAL_ACTION`

### `GET /exchange-rate-evidence`

Lists exchange-rate evidence in the actor organisation. Optional `financialEventId` filters evidence for one event.

### `GET /bookings/{bookingId}/economics`

Returns current calculation snapshot.

Roles: Administrator, Finance, Operations, Read-only Manager.

Response:

```json
{
  "snapshotId": "uuid",
  "bookingId": "uuid",
  "ruleVersion": "economics-v1",
  "currency": "EUR",
  "contractedGrossSale": "1000.00",
  "expectedCustomerReceivable": "950.00",
  "expectedDeductions": "162.50",
  "activeSupplierCost": "500.00",
  "estimatedGrossMargin": "287.50",
  "status": "READY",
  "unknownComponents": [],
  "createdAt": "2026-07-16T06:00:00Z"
}
```

Rules:

- Discounts are represented by `APPROVED_DISCOUNT` financial events.
- Refunds reduce expected customer receivable while contracted gross sale remains unchanged.
- Missing supplier cost or exchange-rate evidence returns `status=NOT_READY`; incomplete values are null, not zero.

### `GET /bookings/{bookingId}/economics/explanation`

Returns formulas, component records, exchange rates, rounding, and rule version.

Roles: Administrator, Finance, Operations, Read-only Manager.

Response includes:

- formula entries with subtotal and business-rule reference;
- component entries with source table, source id, source record id, original amount/currency, converted amount/currency, and formula reference;
- exchange-rate entries when cross-currency financial evidence is used;
- rounding policy text.

Errors:

- `BOOKING_NOT_FOUND`
- `UNAUTHORISED_FINANCIAL_ACTION`

## 11. Matching and Reconciliation

### `POST /bookings/{bookingId}/reconciliation-runs`

Requests reconciliation for one booking.

Roles: Administrator, Finance.

Validation release may call this automatically after imports; explicit rerun can remain internal if not exposed in UI.

### `POST /matches`

Creates a manual match/allocation.

Roles: Administrator, Finance with MFA.

Request:

```json
{
  "bookingId": "uuid",
  "reason": "Settlement report allocates payout across two bookings.",
  "allocations": [
    {
      "financialEventId": "uuid",
      "amount": "500.00",
      "currency": "EUR"
    }
  ]
}
```

Errors:

- `MATCH_OVERALLOCATION`
- `AMBIGUOUS_MATCH`
- `ORG_REFERENCE_MISMATCH`

### `DELETE /matches/{matchId}`

Removes a match from active reconciliation without deleting history.

Roles: Administrator, Finance with MFA.

Request body:

```json
{
  "reason": "Wrong allocation selected during investigation."
}
```

## 12. Discrepancies

### `GET /discrepancies`

Filters discrepancy queue.

Query:

- `status`
- `type`
- `severity`
- `ownerUserId`
- `sourceSystemId`
- `currency`
- `ageMinDays`
- `amountMin`
- `page`
- `size`

Roles: Administrator, Finance, Operations for permitted operational cases, Read-only Manager.

### `GET /discrepancies/{discrepancyId}`

Returns evidence detail: related booking, expected component, actual records, matching attempts, source provenance, comments, and audit references.

### `PATCH /discrepancies/{discrepancyId}`

Updates assignment, classification, workflow status, or resolution.

Roles: Administrator, Finance. Operations may update permitted operational cases.

Errors:

- `UNACCEPTABLE_VARIANCE`
- `INVALID_MANUAL_ADJUSTMENT`

## 13. Timeline and Audit

### `GET /bookings/{bookingId}/timeline`

Returns chronological source, system-derived, and user-controlled events.

Roles: all authenticated roles with record access.

### `GET /audit-events`

Searches audit events.

Roles: Administrator, Finance, Read-only Manager. Operations has only permitted record-scoped visibility.

## 14. Export

### `POST /exports`

Creates a versioned CSV export package.

Roles: Administrator, Finance with recent authentication.

Request:

```json
{
  "exportType": "ACCOUNTING_REVIEW",
  "filters": {
    "serviceFrom": "2026-07-01",
    "serviceTo": "2026-07-31",
    "statuses": ["RECONCILED", "DISCREPANT"]
  }
}
```

Controls:

- scope checked against role and organisation;
- formula-leading cells neutralised;
- export audit event created;
- row count and checksum recorded.

Errors:

- `EXPORT_SCOPE_FORBIDDEN`

## 15. Operations

### `GET /health/live`

Unauthenticated liveness endpoint. Returns process health only.

### `GET /health/ready`

Readiness endpoint. May be restricted in production. Checks database and required dependencies.

### `GET /jobs/{jobId}`

Returns background job state, retry count, diagnostic category, and correlation id.

Roles: Administrator, Finance for financial jobs; Operations for own import jobs.

## 16. Validation Release API Boundary

Required for validation release:

- `/me`
- source systems
- imports
- booking detail
- supplier obligation visibility
- financial event import visibility
- economics and explanation
- reconciliation run/status
- basic discrepancy list/detail
- booking timeline
- health and job status

Can be deferred from validation release UI/API exposure:

- user invitation/deactivation UI;
- accepted variance workflow;
- management dashboard;
- accounting export;
- complex allocation endpoints beyond one-to-one exact match unless the slice is pulled forward.
