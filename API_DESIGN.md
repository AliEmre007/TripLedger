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

## Future API Rules

- Use `/api/v1`.
- Enforce server-side authorisation for every protected endpoint.
- Derive organisation from authenticated actor context.
- Do not reveal cross-organisation object existence.
- Return stable business-rule error codes from `BUSINESS_RULES.md`.
- Require MFA for Administrator and Finance financial functions.

See `API_SPECIFICATION.md` before adding or changing API contracts.
