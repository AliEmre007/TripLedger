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
