# TripLedger Security

This is the implementation-facing security guide. The full Stage 4 security design is in `SECURITY_DESIGN.md`.

## Security Priorities

1. Tenant isolation.
2. Server-side role enforcement.
3. Exact and immutable financial evidence.
4. Audit completeness.
5. Secret and restricted-data protection.
6. Safe import and export handling.
7. Recoverable deployment and backups.

## Data Classification

| Class | Examples | Handling |
|---|---|---|
| Public | documentation, sample templates | no special restriction |
| Internal | settings, source names, non-sensitive metrics | authenticated access |
| Confidential | booking amounts, supplier costs, margins | organisation isolation and role checks |
| Restricted | credentials, MFA material, secrets, backup keys | external secret management |
| Prohibited | raw card data, CVV, passport scans, medical records, bank credentials | reject or remove before processing |

## Secrets

- Do not commit `.env`.
- Do not commit passwords, tokens, certificates, private keys, or backup keys.
- `.env.example` may contain only safe placeholders.
- Production secrets must come from the deployment environment or secret manager.
- Logs and errors must not include secrets.

## Authentication and Authorization

The target architecture uses OIDC and MFA. Stage 7 starts protected API work with a local header-backed actor adapter for development and tests. Protected requests currently resolve:

- authenticated actor context;
- active user check;
- organisation id;
- role;
- MFA state where required;
- correlation id.

Until OIDC is integrated, protected local requests use:

- `X-TripLedger-Actor-Subject`;
- `X-TripLedger-Organisation-Id`;
- optional `X-TripLedger-Mfa-Satisfied`.

Every protected request must enforce role and organisation ownership on the server.

Role and MFA decisions are centralised through named permissions. Finance-only operations must require `FINANCIAL_ACTION` or `FINANCIAL_ACTION_WITH_MFA`; direct role checks in controllers should be avoided so future OIDC integration does not duplicate policy logic.

Source-system creation uses `SOURCE_SYSTEM_MANAGE` and is Administrator-only. Source-system listing uses `PROTECTED_READ` and is filtered by the actor organisation.

Import-batch creation, row-result recording, completion, and failure marking use `OPERATIONAL_WRITE`. Import-batch reads use `PROTECTED_READ`. Every import-batch and row-result query is filtered by the actor organisation, and inactive source systems cannot receive new import batches.

Booking CSV imports use the same `OPERATIONAL_WRITE` boundary. Accepted rows create organisation-scoped source records, bookings, and booking items. Rejected row responses include field names, stable error codes, and safe reasons only; raw CSV content must not be written to logs.

Booking detail reads use `PROTECTED_READ` and are filtered by the actor organisation. Missing bookings and cross-organisation booking ids return `BOOKING_NOT_FOUND` without confirming whether another organisation owns the id. Detail responses may include source identity, import batch id, row number, version, and checksum provenance, but must not return raw CSV payload content.

Supplier-obligation CSV imports use `OPERATIONAL_WRITE`. Accepted rows create organisation-scoped source records, suppliers, and supplier obligations. Unknown booking or item references are accepted as unlinked obligations rather than linked across tenants. Supplier-obligation reads use `PROTECTED_READ`, are filtered by organisation, and expose provenance and review state without raw CSV payload content.

Financial-event CSV imports require `FINANCIAL_ACTION_WITH_MFA`. Accepted rows create organisation-scoped immutable source records and financial events. Unknown booking references are accepted as unmatched events rather than linked across tenants. Financial-event reads use `PROTECTED_READ`, are filtered by organisation, and expose source provenance without raw CSV payload content or prohibited payment data.

Financial-event reversals require `FINANCIAL_ACTION_WITH_MFA`. The correction path creates new organisation-scoped events and never updates or deletes the original accepted event. A user-safe reason is required and returned as reversal evidence.

Money and currency validation is centralised in the application money policy and backed by database currency and minor-unit checks. Invalid precision and unsupported currencies are rejected before domain writes, and binary floating-point values are not used for persisted financial amounts.

Exchange-rate evidence creation requires `FINANCIAL_ACTION_WITH_MFA`. Evidence is organisation-scoped, uses exact decimal rates, stores the rounded converted result, and cannot be attached to a financial event outside the actor organisation. Reads use `PROTECTED_READ` and are filtered by organisation.

Booking economics reads use `PROTECTED_READ`, are filtered by organisation, and create calculation snapshots without exposing raw source payloads. Missing financial inputs are returned as `NOT_READY` evidence rather than zero-filled values.

Booking economics explanation reads use `PROTECTED_READ` and are filtered by organisation. They expose source identities, source record ids, formula references, currencies, and exchange-rate evidence, but not raw imported payload content.

Deterministic matching runs require `FINANCIAL_ACTION`, are scoped to the actor organisation, and never allocate an already allocated financial event. Ambiguous candidates create review evidence without allocating money.

Reconciliation runs require `FINANCIAL_ACTION`, are scoped to the actor organisation, and persist derived state without modifying accepted financial events. Prior reconciliation results are superseded rather than deleted.

Generated discrepancies are organisation-scoped and deduplicated by active cause identity. Reconciliation creates review evidence without exposing raw source payload content.

Discrepancy list and detail reads use `PROTECTED_READ` and are filtered by the actor organisation. Missing and cross-organisation discrepancy ids return `DISCREPANCY_NOT_FOUND`. Queue summaries are calculated from the same tenant-scoped filters as the listed rows, and detail responses expose generated evidence and booking references without raw source payloads.

## Logging

Logs include correlation id through MDC. Logs must not include:

- raw source rows;
- tokens;
- passwords;
- full customer contact data;
- payment account credentials;
- source payloads unless explicitly redacted.

Client-provided correlation ids are accepted only when they are bounded and safe for logs:

- maximum 128 characters;
- letters, numbers, `.`, `_`, `:`, and `-` only.

Blank, unsafe, or overlong correlation ids are replaced with a server-generated id before being written to logs or returned in API responses.

## Security Validation

Run:

```bash
make verify
```

Before real data, the project also needs the gates listed in `SECURITY_DESIGN.md`, including cross-organisation tests, MFA verification, import parser tests, audit completeness tests, backup restore rehearsal, and independent security review.
