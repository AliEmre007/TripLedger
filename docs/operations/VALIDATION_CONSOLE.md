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
- Demo: guided local workflow for creating demo source systems, importing bundled CSV fixtures, listing imported bookings, and running booking controls.
- Booking: booking detail lookup by id.
- Discrepancies: discrepancy queue and detail lookup.
- Timeline: booking timeline lookup by id.

## Demo Workflow

The Demo view uses the same API contracts as external callers. It does not bypass the application services.

Use it after `make seed-local`:

1. Prepare sources: creates local source systems for the demo OTA, supplier, and payment provider when they do not already exist.
2. Import demo data: posts the bundled booking, supplier obligation, and financial event CSV files through the import APIs.
3. Review bookings: loads `GET /api/v1/bookings` and shows imported booking summaries.
4. Run controls: opens booking detail/timeline views or calls economics explanation, matching, and reconciliation endpoints for a selected booking.

The bundled fixture files are served from:

- `/demo/bookings.csv`
- `/demo/supplier_obligations.csv`
- `/demo/financial_events.csv`

## Limits

- No production login flow exists yet.
- The console imports only the bundled validation CSV fixtures. It is not a general CSV upload screen yet.
- Booking, discrepancy, and timeline views require data created through API calls or the Demo workflow.
- Real customer financial data is not approved for this validation release.
