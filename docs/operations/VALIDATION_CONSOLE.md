# Validation Console

TripLedger includes a small static validation console served by the Spring Boot application.

It is not a production user interface. It exists to make the validation backend easier to inspect during demo, staging review, and local operations checks.

## Open The Console

Start the local stack:

```bash
cp .env.example .env
docker compose up --build
```

In another terminal, seed local validation actors:

```bash
make seed-local
```

Open:

```text
http://localhost:18080/
```

## Default Local Actor

The console defaults to:

| Header | Value |
| --- | --- |
| `X-TripLedger-Actor-Subject` | `local-admin` |
| `X-TripLedger-Organisation-Id` | `11111111-1111-1111-1111-111111111111` |
| `X-TripLedger-Mfa-Satisfied` | `true` |

`make seed-local` creates:

- `local-admin`
- `local-finance`
- `local-ops`
- `local-readonly`

These users are local validation users only. They are not production authentication.

## Current Views

- Status: application health, readiness checks, and Actuator links.
- Actor: current local/header-backed actor context.
- Booking: booking detail lookup by id.
- Discrepancies: discrepancy queue and detail lookup.
- Timeline: booking timeline lookup by id.

## Limits

- No production login flow exists yet.
- No CSV upload workflow exists in the console yet.
- Booking, discrepancy, and timeline views require data created through API calls or future demo seeding.
- Real customer financial data is not approved for this validation release.
