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

### Actuator

- `GET /actuator/health`
- `GET /actuator/metrics`

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

## Future API Rules

- Use `/api/v1`.
- Enforce server-side authorisation for every protected endpoint.
- Derive organisation from authenticated actor context.
- Do not reveal cross-organisation object existence.
- Return stable business-rule error codes from `BUSINESS_RULES.md`.
- Require MFA for Administrator and Finance financial functions.

See `API_SPECIFICATION.md` before adding or changing API contracts.
