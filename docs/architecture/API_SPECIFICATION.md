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
| `X-Correlation-Id` | request | optional | Client-provided correlation id; server creates one if absent |
| `Idempotency-Key` | request | write endpoints where specified | Prevent duplicate client retries |
| `X-Correlation-Id` | response | always | Correlate user errors to logs |

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

## 6. Imports

### `POST /imports`

Creates an import batch and uploads a CSV file.

Roles:

- Booking import: Administrator, Finance, Operations.
- Supplier obligation import: Administrator, Finance, Operations.
- Financial event import: Administrator, Finance.

Content type: `multipart/form-data`

Fields:

- `sourceSystemId`
- `templateType`: `BOOKINGS`, `SUPPLIER_OBLIGATIONS`, `FINANCIAL_EVENTS`
- `templateVersion`
- `file`

Response:

```json
{
  "importBatchId": "uuid",
  "status": "PROCESSING",
  "correlationId": "01JZ..."
}
```

Errors:

- `UNSUPPORTED_TEMPLATE_VERSION`
- `IMPORT_LIMIT_EXCEEDED`
- `MISSING_REQUIRED_COLUMN`
- `EXPORT_SCOPE_FORBIDDEN` for unauthorised financial import scope.

### `GET /imports/{importBatchId}`

Returns import status and counts.

Roles: users allowed to view the import type.

Response:

```json
{
  "id": "uuid",
  "sourceSystemId": "uuid",
  "templateType": "BOOKINGS",
  "templateVersion": "bookings-v1",
  "status": "COMPLETED_WITH_ERRORS",
  "counts": {
    "accepted": 8,
    "duplicate": 0,
    "rejected": 2,
    "warning": 0
  }
}
```

### `GET /imports/{importBatchId}/rows`

Returns row-level results with pagination.

Query:

- `result`
- `page`
- `size`

### `GET /imports/{importBatchId}/rejected-rows.csv`

Downloads rejected rows and error metadata.

Roles: users allowed to view the import type.

Controls:

- No secrets or hidden system values.
- Spreadsheet formula-leading cells are neutralised.

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

Returns canonical booking detail, items, provenance, current economics, reconciliation status, and active discrepancies.

Roles: Administrator, Finance, Operations, Read-only Manager.

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

### `GET /bookings/{bookingId}/supplier-obligations`

Returns obligations linked to the booking.

Roles: all authenticated roles, with Operations limited by financial-detail policy.

### `POST /supplier-obligations/{obligationId}/credits`

Creates a supplier credit or cancellation event.

Roles: Administrator, Finance; Operations only before finance control when allowed.

Errors:

- `SUPPLIER_OVERALLOCATION`
- `UNAUTHORISED_FINANCIAL_ACTION`

## 9. Financial Events

### `GET /financial-events`

Searches financial events.

Roles: Administrator, Finance, Read-only Manager. Operations receives only permitted booking-scoped visibility.

Query:

- `bookingId`
- `eventType`
- `unmatched`
- `sourceSystemId`
- `effectiveFrom`
- `effectiveTo`
- `page`
- `size`

### `POST /financial-events/{eventId}/reversal`

Creates a reversal and optional replacement event.

Roles: Administrator, Finance with MFA.

Request:

```json
{
  "reason": "Corrected source file from gateway",
  "replacementEvent": {
    "eventType": "CUSTOMER_PAYMENT",
    "amount": "950.00",
    "currency": "EUR",
    "effectiveAt": "2026-07-13T10:00:00Z",
    "externalReference": "PAY-123-CORRECTED"
  }
}
```

Errors:

- `FINANCIAL_EVENT_IMMUTABLE`
- `INVALID_MANUAL_ADJUSTMENT`

## 10. Economics

### `GET /bookings/{bookingId}/economics`

Returns current calculation snapshot.

Roles: Administrator, Finance, Operations, Read-only Manager.

Response:

```json
{
  "bookingId": "uuid",
  "ruleVersion": "economics-v1",
  "currency": "EUR",
  "contractedGrossSale": "1000.00",
  "expectedCustomerReceivable": "950.00",
  "expectedDeductions": "162.50",
  "activeSupplierCost": "500.00",
  "estimatedGrossMargin": "287.50",
  "unknownComponents": []
}
```

### `GET /bookings/{bookingId}/economics/explanation`

Returns formulas, component records, exchange rates, rounding, and rule version.

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
