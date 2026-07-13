# THREAT_ASSUMPTIONS.md

## TripLedger Threat Assumptions and Initial Threat Model

**Version:** 0.1  
**Date:** 13 July 2026  
**Scope:** File-import-first MVP and its production-like pilot

---

## 1. Security Objective

TripLedger must prevent an unauthorised actor, compromised account, malformed source, or software defect from:

- seeing another organisation's data;
- changing booking economics without evidence;
- applying the same financial value more than once;
- hiding or altering an audit trail;
- exporting restricted data;
- or making the system silently accept an incorrect reconciliation.

Security verification should use OWASP ASVS 5.0.0 as the application-control baseline and consider the OWASP Top 10 2025 during design and testing.[^asvs][^top10]

---

## 2. Data Classification

| Class | Examples | MVP handling |
|---|---|---|
| Public | Product documentation, template examples | No special restriction |
| Internal | Rule configuration, non-sensitive metrics, source names | Authenticated access |
| Confidential | Booking amounts, supplier obligations, commissions, margins, reconciliation results | Organisation isolation, role control, encrypted transport, protected exports |
| Restricted | Credentials, MFA material, API secrets, encryption keys, backup keys | External secret management, never in payloads/logs/source control |
| Prohibited in MVP | Raw card number, card security code, passport scan, medical record, unrestricted bank credential | Reject, avoid collecting, or remove before processing |

An external customer reference may still be personal data when it can be linked back to a person. Data minimisation remains necessary even when a field does not contain a name.

---

## 3. Protected Assets

1. Organisation boundary.
2. Authentication session and role claims.
3. Imported source files and row provenance.
4. Canonical bookings and supplier obligations.
5. Financial events and reversal chains.
6. Exchange rates and calculation versions.
7. Matches and allocation balances.
8. Reconciliation results and discrepancies.
9. Manual adjustments and accepted variances.
10. Audit events.
11. Exports.
12. Backups, logs, metrics, and deployment secrets.

---

## 4. Trust Boundaries

```text
User browser
    │ untrusted input / authenticated session
    ▼
Identity provider ── signed identity claims ──► TripLedger application
                                                  │
                    uploaded CSV / export         │ controlled DB access
External source ─────────────────────────────────►│
                                                  ▼
                                           Relational database
                                                  │
                                                  ├── object/file storage
                                                  ├── logs and metrics
                                                  └── backup storage
```

Every boundary requires validation. Identity-provider claims do not replace application authorisation; database constraints do not replace organisation-scoped queries.

---

## 5. Threat Actors

- External unauthenticated attacker.
- Authenticated user attempting cross-organisation access.
- Operations user attempting a Finance-only action.
- Compromised Finance or Administrator account.
- Malicious or careless insider.
- Malicious source-file producer.
- Compromised third-party dependency or build component.
- Accidental operator causing duplicate import, stale update, or incorrect adjustment.
- Future connector returning malformed, replayed, or adversarial data.

---

## 6. Primary Threats and Required Controls

| Threat | Example | Required control and evidence |
|---|---|---|
| Broken object-level authorisation | User guesses another booking ID | Organisation-scoped server checks and negative API tests |
| Privilege escalation | Operations calls adjustment endpoint | Deny-by-default role policy and audit |
| Tenant reference corruption | ORG-A payment links to ORG-B booking | Composite organisation-scoped FK pattern plus domain check; POC-005 |
| Replay or duplicate import | Same settlement imported twice | Stable source identity, unique constraint, idempotency; POC-001 |
| Stale update | Older booking version overwrites current state | Version ordering; versionless conflict policy |
| Financial event tampering | Accepted payment edited | Immutable event; reversal and replacement |
| Allocation race | Two users allocate the same payment | Transactional conservation and production-DB concurrency test; POC-004 |
| False positive match | One payment automatically closes wrong booking | Unique deterministic rules; ambiguity remains open; POC-003 |
| Currency manipulation | Rate changed without evidence | Immutable rate reference, source/effective date, role control; POC-002 |
| CSV/parser abuse | Oversized row, malformed quoting, formula payload | Streaming parser, limits, validation, non-executable storage, export escaping |
| Spreadsheet injection | Exported cell begins with `=` | Formula neutralisation; POC-006 |
| Stored script injection | Source memo rendered as HTML | Contextual output encoding and sanitisation |
| SQL/command injection | Imported field reaches query or command | Parameterised access; no shell interpolation |
| Audit suppression | Financial action succeeds without audit | Same transaction/outbox control and acceptance test |
| Audit tampering | User edits history | Append-only application path and restricted DB privilege |
| Sensitive logging | Source row or token appears in log | Structured allow-list logging and redaction tests |
| Export leakage | User exports wider scope than authorised | Purpose/filter enforcement, recent authentication, export audit |
| Backup compromise | Unencrypted backup copied | Encrypted storage, restricted key, restore manifest, access logs |
| Denial of service | Huge file or repeated reconciliation | File/rate limits, bounded work, asynchronous job control, quotas |
| Supply-chain compromise | Malicious library or image | Locked dependencies, SBOM, scans, trusted registries, review |
| Insecure future connector | SSRF or secret misuse | Egress allow-list, connector isolation, scoped secret, timeout, no first-release connector |

---

## 7. Security Assumptions

The MVP assumes:

- The identity provider signs and validates tokens correctly.
- Production-like environments use encrypted transport.
- Database and object-storage credentials are not shared with application users.
- The host and container runtime receive security updates.
- Backup storage is separate from the primary database account.
- Uploaded CSV does not need active-content execution.
- Raw payment-card data is never required.
- No public anonymous upload endpoint exists.
- Administrators are trusted to configure policy but their actions remain auditable.

Each assumption must become either verified deployment evidence or an explicit accepted risk.

---

## 8. Privacy and Payment Boundaries

Türkiye's data-protection authority describes personal-data processing principles including purpose limitation, proportionality, data minimisation, and retention only for the required period.[^kvkk] GDPR Article 5 similarly includes purpose limitation, data minimisation, accuracy, storage limitation, integrity/confidentiality, and accountability.[^gdpr]

The current PCI DSS version listed by PCI SSC is v4.0.1, and PCI DSS applies to environments that store, process, transmit, or can impact payment account data.[^pci-current][^pci-scope] TripLedger therefore deliberately excludes raw cardholder and sensitive authentication data. A future payment connector requires a formal PCI scope assessment; tokenisation or hosted payment pages do not automatically eliminate every security responsibility.

This document is an engineering risk assessment, not legal advice.

---

## 9. Security Verification Gate Before Real Data

- [ ] ASVS-based control checklist selected for the implemented architecture.
- [ ] Cross-role and cross-organisation API tests pass.
- [ ] Finance and Administrator MFA verified.
- [ ] Import parser limits and malformed-file tests pass.
- [ ] No restricted data in logs, errors, fixtures, or repository.
- [ ] Dependency, secret, and image scans pass release policy.
- [ ] Allocation concurrency test passes against the selected database.
- [ ] Audit completeness test passes for every material financial action.
- [ ] Backup encryption and restore rehearsal verified.
- [ ] Data-flow, retention, controller/processor, and hosting review completed.
- [ ] Independent security review of the first pilot release.

---

## References

[^asvs]: OWASP, Application Security Verification Standard; latest stable version shown as 5.0.0. https://owasp.org/www-project-application-security-verification-standard/
[^top10]: OWASP, Top Ten Web Application Security Risks; current released edition shown as 2025. https://owasp.org/www-project-top-ten/
[^kvkk]: Kişisel Verileri Koruma Kurumu, summary of general principles including purpose limitation, proportionality and retention. https://www.kvkk.gov.tr/Icerik/6721/
[^gdpr]: Regulation (EU) 2016/679, Article 5. https://eur-lex.europa.eu/eli/reg/2016/679/2016-05-04/eng
[^pci-current]: PCI Security Standards Council, Document Library, listing PCI DSS v4.0.1. https://www.pcisecuritystandards.org/document_library/
[^pci-scope]: PCI Security Standards Council, PCI DSS overview and scope. https://www.pcisecuritystandards.org/standards/pci-dss/
