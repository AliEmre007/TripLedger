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

## Logging

Logs include correlation id through MDC. Logs must not include:

- raw source rows;
- tokens;
- passwords;
- full customer contact data;
- payment account credentials;
- source payloads unless explicitly redacted.

## Security Validation

Run:

```bash
make verify
```

Before real data, the project also needs the gates listed in `SECURITY_DESIGN.md`, including cross-organisation tests, MFA verification, import parser tests, audit completeness tests, backup restore rehearsal, and independent security review.
