# PRODUCT_REQUIREMENTS.md

## TripLedger — Product Requirements Document

**Stage:** 2 — Scope, Requirements, and Business Rules  
**Product:** TripLedger  
**Owner:** Ali Emre GÜŞLÜ  
**Date:** 13 July 2026  
**Version:** 0.1  
**Status:** MVP requirements baseline  
**Inputs:** `PROJECT_BRIEF.md`, `STAGE_1_DISCOVERY_AND_VALIDATION.md`

---

## 1. Purpose

This document converts the discovery hypothesis into a small, testable MVP.

The MVP is not a full tourism operating system. It is a provider-neutral financial control application that imports representative booking and financial records, calculates expected booking economics, performs deterministic reconciliation, and gives users an auditable discrepancy workflow.

The release must answer one question:

> Can TripLedger reduce the effort and uncertainty required to determine the financial state of a multi-supplier tourism booking?

---

## 2. Product Outcome

For every eligible booking, an authorised user must be able to determine:

1. The booking source and stable source identifier.
2. The contracted selling amount and currency.
3. The customer payments or channel settlements received.
4. Channel commissions and payment fees.
5. Refunds and financial adjustments.
6. Supplier obligations and credits.
7. Expected net financial result.
8. Actual matched financial result.
9. Unmatched or inconsistent amounts.
10. Current reconciliation status.
11. The source and calculation behind every displayed amount.
12. Who changed, reviewed, or resolved a material record.

---

## 3. Target Users

| Role | Primary outcome |
|---|---|
| Finance user | Reconcile bookings and resolve financial discrepancies |
| Operations user | Maintain booking and supplier information without changing controlled finance decisions |
| Manager | Review financial status, margin, unresolved exposure, and audit evidence |
| Administrator | Manage organisation settings, users, source definitions, and reconciliation tolerances |
| External accountant | Consume a traceable export; direct system access is not required for the MVP |

---

## 4. Product Principles

1. **Explain before automate.** Every calculated result must expose its source records and rules.
2. **Preserve provenance.** Imported values retain source system, source record, import batch, and original payload reference.
3. **Financial history is append-only.** Corrections use reversal or adjustment events rather than destructive edits.
4. **Do not infer when evidence is ambiguous.** Automatic matching is allowed only when deterministic rules produce one valid candidate.
5. **Separate statuses.** Booking, payment, supplier, reconciliation, discrepancy, and export statuses are distinct.
6. **Fail visibly.** Invalid rows and failed jobs are visible and actionable; silent data loss is forbidden.
7. **Minimise sensitive data.** The MVP does not require passport data, raw card data, or unnecessary customer details.
8. **Integrate before replace.** The MVP imports from existing systems rather than rebuilding booking, payment, or accounting platforms.
9. **Keep the delivery small.** A feature that cannot be completed in one short iteration must be split before implementation.
10. **Architecture follows requirements.** No infrastructure technology is justified merely by anticipated scale.

---

## 5. MVP Boundary

### 5.1 Included capabilities

The MVP includes:

- One organisation with logical tenant boundaries designed into the model.
- Four application roles: Administrator, Finance, Operations, and Read-only Manager.
- Source-system registration.
- Versioned CSV import templates for:
  - Bookings and booking items.
  - Financial events.
  - Supplier obligations.
- Controlled manual entry for exceptional records.
- Canonical booking-level financial model.
- Multiple booking items and suppliers.
- Multiple currencies with recorded exchange rates where base-currency reporting is required.
- Expected booking-economics calculation.
- Deterministic automatic matching.
- Controlled manual matching and unmatching.
- Reconciliation status calculation.
- Explainable discrepancy creation and queue.
- Assignment, classification, ageing, resolution, and accepted-variance workflow.
- Controlled adjustments with reason and audit evidence.
- Booking-level financial timeline.
- Management summary and discrepancy dashboard.
- CSV export for accounting or external review.
- Security, audit, observability, backup, restore, and deployment evidence.

### 5.2 MVP integration strategy

The MVP uses **versioned file imports and realistic source simulators** instead of production OTA, bank, gateway, or accounting APIs.

Why:

- It tests the domain and workflow without depending on commercial API access.
- It keeps the iteration size compatible with a solo engineer.
- It allows repeated deterministic acceptance tests.
- It avoids spending most of the MVP on one provider-specific connector.

Production API connectors are later enhancements and must reuse the same canonical import contracts.

---

## 6. Conceptual Domain Model

This is a requirements-level model, not a database design.

| Concept | Purpose |
|---|---|
| Organisation | Owns users, settings, source systems, imports, and financial records |
| User | Authenticated actor with an organisation role |
| Source system | Identifies an external booking, payment, channel, bank, supplier, or manual source |
| Import batch | Records one uploaded file, processing result, counts, errors, and actor |
| Source record | Preserves the original external identity and payload reference |
| Booking | Commercial agreement with the customer or reseller |
| Booking item | Individual hotel, transfer, guide, activity, or other service sold in a booking |
| Supplier obligation | Expected or invoiced cost payable to a supplier |
| Financial event | Customer payment, settlement, fee, commission, refund, credit, reversal, or adjustment |
| Exchange-rate record | Rate and effective date used for a specific conversion |
| Match | Explicit relationship between expected and actual financial records |
| Reconciliation result | Calculated booking-level status and variance |
| Discrepancy | Explainable exception requiring review or accepted resolution |
| Adjustment | Controlled correction or accepted business variance |
| Audit event | Append-only evidence of material actions and changes |
| Export batch | Traceable output generated for accounting or external review |

---

## 7. Status Model

### 7.1 Booking lifecycle status

- `DRAFT`
- `CONFIRMED`
- `IN_SERVICE`
- `COMPLETED`
- `CANCELLED`

### 7.2 Reconciliation status

- `NOT_READY` — required booking economics are incomplete.
- `PENDING` — eligible for reconciliation but not fully processed.
- `PARTIALLY_RECONCILED` — some required components match and some remain open.
- `RECONCILED` — all required components match within approved tolerances and no material discrepancy is open.
- `DISCREPANT` — at least one material mismatch or invalid condition is open.
- `CLOSED_WITH_VARIANCE` — an authorised user accepted and documented the remaining difference.

### 7.3 Discrepancy workflow status

- `OPEN`
- `IN_REVIEW`
- `WAITING_EXTERNAL`
- `RESOLVED`
- `ACCEPTED_VARIANCE`

### 7.4 Import status

- `RECEIVED`
- `PROCESSING`
- `COMPLETED`
- `COMPLETED_WITH_ERRORS`
- `FAILED`

Statuses are not interchangeable. For example, a `CANCELLED` booking can still be `DISCREPANT` because its refund or cancellation fee has not reconciled.

---

## 8. Functional Requirements

Priority definitions:

- **P0:** Required for the MVP exit gate.
- **P1:** Valuable after P0 is stable; not required to prove the central hypothesis.
- **P2:** Later enhancement.

### 8.1 Organisation, identity, and access

| ID | Priority | Requirement |
|---|---|---|
| FR-001 | P0 | The system shall require authentication for every application function except health checks and the login flow. |
| FR-002 | P0 | The system shall enforce the role permissions defined in the access-control matrix. |
| FR-003 | P0 | The system shall prevent a user from reading or modifying another organisation's records. |
| FR-004 | P0 | An Administrator shall be able to invite, deactivate, and assign one supported role to a user. |
| FR-005 | P0 | Deactivating a user shall prevent new sessions while preserving the user's historical audit identity. |
| FR-006 | P0 | Administrator and Finance roles shall require multi-factor authentication before accessing financial functions. |
| FR-007 | P1 | An Administrator shall be able to configure base currency, amount tolerances, date windows, and materiality threshold. |

### 8.2 Source-system management

| ID | Priority | Requirement |
|---|---|---|
| FR-010 | P0 | An Administrator shall be able to register a source system with name, category, external code, time zone, and active status. |
| FR-011 | P0 | Source-system external code shall be unique within an organisation. |
| FR-012 | P0 | Deactivating a source system shall prevent new imports from it without deleting historical records. |
| FR-013 | P1 | The system shall expose the latest successful and failed import for each source. |

### 8.3 File imports and validation

| ID | Priority | Requirement |
|---|---|---|
| FR-020 | P0 | A permitted user shall be able to upload a versioned CSV file for bookings, financial events, or supplier obligations. |
| FR-021 | P0 | The system shall validate file type, template version, required columns, data types, supported enumerations, currency codes, dates, and source identity before creating domain records. |
| FR-022 | P0 | The system shall process valid rows and reject invalid rows without silently discarding either. |
| FR-023 | P0 | Each rejected row shall expose row number, field, machine-readable error code, and user-readable reason. |
| FR-024 | P0 | Re-importing the same source record shall not create a duplicate domain record or duplicate financial effect. |
| FR-025 | P0 | An import summary shall show total, accepted, duplicate, rejected, and warning counts. |
| FR-026 | P0 | The system shall preserve the uploaded file checksum, template version, source system, actor, timestamp, and source-row identity. |
| FR-027 | P0 | A user shall be able to download rejected rows with their errors. |
| FR-028 | P0 | A failed import shall be retryable after correction without manually deleting partial data. |
| FR-029 | P1 | A user shall be able to validate a file and preview the first 20 interpreted rows before committing the import. |

### 8.4 Booking records

| ID | Priority | Requirement |
|---|---|---|
| FR-030 | P0 | The system shall create or update a canonical booking from an accepted source record according to the source-version rules. |
| FR-031 | P0 | A booking shall retain organisation, source system, external booking identifier, source version, booking date, service dates, lifecycle status, selling currency, and contracted selling amount. |
| FR-032 | P0 | A booking shall contain one or more booking items. |
| FR-033 | P0 | Each booking item shall identify service type, service dates, selling amount, selling currency, supplier reference when known, and active/cancelled state. |
| FR-034 | P0 | The system shall display the current booking record and its source provenance. |
| FR-035 | P0 | An Operations user may edit allowed operational fields, but may not directly edit imported financial events or approved adjustments. |
| FR-036 | P0 | A material booking change shall trigger recalculation and shall be recorded in the audit timeline. |
| FR-037 | P1 | A user shall be able to add an internal note that is distinct from source data and financial evidence. |

### 8.5 Supplier obligations

| ID | Priority | Requirement |
|---|---|---|
| FR-040 | P0 | A permitted user shall be able to import or create a supplier obligation linked to a booking item or booking. |
| FR-041 | P0 | A supplier obligation shall contain supplier reference, amount, currency, due date when known, obligation status, and source provenance. |
| FR-042 | P0 | The system shall support expected, confirmed, invoiced, credited, cancelled, and paid obligation states without overwriting history. |
| FR-043 | P0 | A supplier credit or cancellation shall reduce the active supplier cost through a separate event or version. |
| FR-044 | P0 | An Operations user may propose or update an unapproved expected supplier cost; Finance approval shall be required for a controlled manual adjustment after reconciliation has started. |
| FR-045 | P1 | The system shall identify supplier obligations that have no linked booking item. |

### 8.6 Financial events

| ID | Priority | Requirement |
|---|---|---|
| FR-050 | P0 | The system shall accept customer payment, channel settlement, channel commission, payment fee, refund, supplier payment, supplier credit, reversal, and manual adjustment event types. |
| FR-051 | P0 | A financial event shall contain source, external identifier, event type, amount, currency, effective timestamp, booking reference when known, and provenance. |
| FR-052 | P0 | Financial event amounts shall be represented with exact decimal semantics and currency-specific precision; binary floating-point shall not determine financial results. |
| FR-053 | P0 | An imported financial event shall be immutable after acceptance. |
| FR-054 | P0 | A correction to an accepted financial event shall use a linked reversal and replacement event or an authorised adjustment. |
| FR-055 | P0 | The system shall reject an unsupported currency or an amount with invalid precision. |
| FR-056 | P0 | The system shall preserve both original-currency amount and base-currency amount when conversion is required. |
| FR-057 | P0 | A base-currency conversion shall reference a recorded exchange rate and effective date. |
| FR-058 | P1 | A Finance user shall be able to enter a controlled manual financial event when external import is unavailable. |

### 8.7 Expected booking economics

| ID | Priority | Requirement |
|---|---|---|
| FR-060 | P0 | The system shall calculate contracted gross sale from active booking items. |
| FR-061 | P0 | The system shall calculate expected customer receivable after approved discounts, cancellations, retained cancellation fees, and expected refunds. |
| FR-062 | P0 | The system shall calculate expected channel and payment deductions from recorded fee or commission events and approved expectations. |
| FR-063 | P0 | The system shall calculate active supplier cost from supplier obligations net of credits and cancellations. |
| FR-064 | P0 | The system shall calculate estimated gross margin as expected customer receivable minus expected channel fees, payment fees, and active supplier cost. |
| FR-065 | P0 | Every displayed subtotal shall expose its component records and calculation formula. |
| FR-066 | P0 | Missing required economic inputs shall produce `NOT_READY` rather than an assumed zero. |
| FR-067 | P1 | A Finance user shall be able to compare original-currency and base-currency economics. |

### 8.8 Matching and reconciliation

| ID | Priority | Requirement |
|---|---|---|
| FR-070 | P0 | The system shall attempt deterministic matching using explicit references, organisation, source, amount, currency, event type, and configured date window. |
| FR-071 | P0 | The system shall automatically create a match only when exactly one valid candidate satisfies an approved matching rule. |
| FR-072 | P0 | Ambiguous candidates shall remain unmatched and create a reviewable discrepancy or warning. |
| FR-073 | P0 | A Finance user shall be able to create and remove a manual match with a reason. |
| FR-074 | P0 | A match shall preserve the matching rule, actor or system identity, timestamp, and participating records. |
| FR-075 | P0 | The system shall support one-to-one, one-to-many, and many-to-one allocations when the allocated totals remain valid. |
| FR-076 | P0 | The system shall prevent the same financial amount from being allocated beyond its available amount. |
| FR-077 | P0 | Reconciliation shall be repeatable and produce the same result for unchanged inputs and settings. |
| FR-078 | P0 | A material source or rule change shall recalculate affected bookings and preserve the previous result in the audit history. |
| FR-079 | P1 | A Finance user shall be able to re-run reconciliation for one booking or one import batch. |

### 8.9 Discrepancy management

| ID | Priority | Requirement |
|---|---|---|
| FR-080 | P0 | The system shall create a discrepancy when a material amount, missing record, duplicate, invalid state, or ambiguous match prevents reconciliation. |
| FR-081 | P0 | A discrepancy shall contain type, severity, amount when applicable, currency, explanation, evidence links, creation time, age, status, and owner. |
| FR-082 | P0 | Supported discrepancy types shall include missing payment, short settlement, unmatched payment, incorrect fee, supplier variance, refund mismatch, duplicate source record, exchange-rate variance, timing difference, and invalid state. |
| FR-083 | P0 | A Finance user shall be able to assign, classify, comment on, and change the workflow status of a discrepancy. |
| FR-084 | P0 | Resolving a discrepancy shall require a resolution type and explanatory note. |
| FR-085 | P0 | Accepting a material variance shall require Finance or Administrator authority and a reason. |
| FR-086 | P0 | A resolved discrepancy shall reopen automatically when a later source record invalidates its resolution. |
| FR-087 | P0 | The queue shall support filtering by status, type, severity, owner, source, currency, age, and amount. |
| FR-088 | P1 | The system shall notify an assigned user when a material discrepancy exceeds its ageing threshold. |

### 8.10 Timeline, audit, dashboard, and export

| ID | Priority | Requirement |
|---|---|---|
| FR-090 | P0 | The booking detail shall display a chronological financial and operational timeline. |
| FR-091 | P0 | The timeline shall distinguish imported source events, system calculations, user actions, matches, discrepancies, and adjustments. |
| FR-092 | P0 | The system shall maintain append-only audit evidence for authentication-sensitive and financial actions. |
| FR-093 | P0 | A Manager shall see counts and values for reconciled, partially reconciled, discrepant, not-ready, and closed-with-variance bookings. |
| FR-094 | P0 | The dashboard shall show unresolved discrepancy value, age distribution, top discrepancy types, and latest failed imports. |
| FR-095 | P0 | Dashboard values shall link to the underlying filtered records. |
| FR-096 | P0 | A Finance user shall be able to export booking economics, matched records, unresolved discrepancies, and audit references as a versioned CSV package. |
| FR-097 | P0 | An export shall record filters, actor, generation time, format version, row count, and checksum. |
| FR-098 | P1 | A Manager shall be able to save a dashboard filter. |

---

## 9. Access-Control Matrix

Legend: `R` read, `C` create/import, `U` update, `A` approve/accept, `M` manage.

| Capability | Administrator | Finance | Operations | Read-only Manager |
|---|---:|---:|---:|---:|
| Organisation settings | M | R | — | R |
| User and role management | M | — | — | — |
| Source-system management | M | R | R | R |
| Booking import | C/R | C/R | C/R | R |
| Booking operational fields | U/R | U/R | U/R | R |
| Supplier obligations | M | C/U/R | C/U/R before finance control | R |
| Financial-event import | C/R | C/R | — | R |
| Manual financial event | A/C/R | C/R | — | R |
| Matching and reconciliation | M | C/U/R | R | R |
| Discrepancy assignment | M | C/U/R | C/U/R for operational cases | R |
| Accept material variance | A | A | — | R |
| Audit timeline | R | R | R for permitted records | R |
| Dashboard | R | R | R with limited financial detail | R |
| Accounting export | C/R | C/R | — | R of generated exports |

Every denial must occur on the server side; hiding a user-interface control is not sufficient enforcement.

---

## 10. Non-Functional Requirements

### 10.1 Security

| ID | Requirement |
|---|---|
| NFR-SEC-001 | All network traffic carrying authentication or application data shall use encrypted transport in production-like environments. |
| NFR-SEC-002 | Passwords and MFA secrets shall be managed by an identity component; TripLedger shall not store plaintext passwords. |
| NFR-SEC-003 | Administrator and Finance access shall require MFA. |
| NFR-SEC-004 | Authorisation shall be enforced for every server-side request using organisation and role context. |
| NFR-SEC-005 | A cross-organisation access test shall return no data and a non-success response without revealing whether the target record exists. |
| NFR-SEC-006 | Security-sensitive actions shall be audited, including login failure, role change, user deactivation, import, manual match, unmatch, adjustment, variance acceptance, export, and settings change. |
| NFR-SEC-007 | Secrets shall not be committed to source control, returned in APIs, or written to logs. |
| NFR-SEC-008 | Uploaded files shall be size-limited, content-validated, stored outside executable paths, and rejected when the declared and detected formats conflict. |
| NFR-SEC-009 | The application shall validate and encode untrusted input to prevent injection and stored-script execution. |
| NFR-SEC-010 | A user session shall expire after 30 minutes of inactivity and no later than 12 hours after authentication unless the identity policy is stricter. |
| NFR-SEC-011 | Financial exports shall require re-authentication or a recently authenticated session no older than 15 minutes. |
| NFR-SEC-012 | Dependency, secret, and container-image scans shall run in the delivery pipeline and block release on unresolved critical findings. |

### 10.2 Privacy

| ID | Requirement |
|---|---|
| NFR-PRI-001 | The MVP shall not store raw card numbers, card security codes, passport scans, or identity documents. |
| NFR-PRI-002 | A booking shall require only an external customer reference; customer name and contact data are optional and shall be minimised. |
| NFR-PRI-003 | Logs and error messages shall not expose full customer contact data, source payloads, tokens, or financial-account credentials. |
| NFR-PRI-004 | Organisation data shall be logically isolated in storage, queries, exports, caches, and background jobs. |
| NFR-PRI-005 | Test and demonstration environments shall use synthetic or anonymised data. |
| NFR-PRI-006 | Exported files shall include only fields required by the selected export purpose. |

### 10.3 Reliability and data integrity

| ID | Requirement |
|---|---|
| NFR-REL-001 | Reprocessing an unchanged import shall be idempotent and create no additional financial effect. |
| NFR-REL-002 | A logical financial operation shall either complete fully or leave no partially committed financial state. |
| NFR-REL-003 | Background processing shall retry transient failures no more than three times and shall expose a final failed state with diagnostic context. |
| NFR-REL-004 | The system shall preserve source records, financial events, matches, adjustments, and audit events after user deactivation or source deactivation. |
| NFR-REL-005 | Reconciliation calculations shall be deterministic for identical records, rules, exchange rates, and settings. |
| NFR-REL-006 | The MVP shall target 99.5% monthly availability in a production-like pilot, excluding announced maintenance. |
| NFR-REL-007 | Backup evidence shall demonstrate a recovery point no older than 24 hours. |
| NFR-REL-008 | Restore evidence shall demonstrate service recovery within four hours for the pilot dataset. |
| NFR-REL-009 | A failed import or calculation shall not silently change the last accepted reconciliation result. |

### 10.4 Performance and capacity

The acceptance dataset represents a small-business pilot, not internet-scale traffic.

| ID | Requirement |
|---|---|
| NFR-PERF-001 | For up to 100,000 bookings and 1,000,000 financial records in one organisation, 95% of booking-detail requests shall complete within 2 seconds under the documented test load. |
| NFR-PERF-002 | 95% of filtered discrepancy-list requests shall complete within 2 seconds for up to 100,000 open and historical discrepancies. |
| NFR-PERF-003 | A 10,000-row import shall complete validation and persistence within 5 minutes on the reference pilot environment. |
| NFR-PERF-004 | Reconciliation of 10,000 affected bookings shall complete within 10 minutes on the reference pilot environment. |
| NFR-PERF-005 | Dashboard summary requests shall complete within 3 seconds for the reference dataset. |
| NFR-PERF-006 | Export generation for 100,000 rows shall complete within 10 minutes or continue asynchronously with visible status. |

### 10.5 Observability and supportability

| ID | Requirement |
|---|---|
| NFR-OBS-001 | Every request and background job shall have a correlation identifier visible in structured logs. |
| NFR-OBS-002 | Logs shall identify organisation, operation, outcome, duration, and error category without exposing restricted data. |
| NFR-OBS-003 | Metrics shall include import counts and failures, reconciliation duration, match counts, discrepancy counts, export failures, authentication failures, and job retries. |
| NFR-OBS-004 | Health checks shall distinguish process health from dependency readiness. |
| NFR-OBS-005 | An operator shall be able to identify the reason for a failed import or reconciliation run from documented logs and application status without database modification. |
| NFR-OBS-006 | A runbook shall document startup, shutdown, backup, restore, failed-job recovery, import investigation, and rollback. |
| NFR-OBS-007 | User-facing errors shall include a stable error code and correlation identifier. |

### 10.6 Deployment and maintainability

| ID | Requirement |
|---|---|
| NFR-DEP-001 | A new developer shall be able to start the complete development environment from a clean documented machine in no more than 30 minutes, excluding initial software downloads. |
| NFR-DEP-002 | Database or schema changes shall be versioned, repeatable, and reversible through a documented recovery procedure. |
| NFR-DEP-003 | The release pipeline shall run automated tests, static checks, security scans, packaging, and deployment verification. |
| NFR-DEP-004 | A deployment shall include an automated health verification and a documented rollback procedure. |
| NFR-DEP-005 | Configuration shall be externalised by environment; production secrets shall not be embedded in build artifacts. |
| NFR-DEP-006 | Critical business rules shall have automated tests at the domain level and at least one end-to-end acceptance scenario. |
| NFR-DEP-007 | Public or integration-facing contracts shall be versioned and backward-incompatible changes shall require a new version. |

### 10.7 Usability and accessibility

| ID | Requirement |
|---|---|
| NFR-USE-001 | A trained Finance user shall be able to determine why a booking is discrepant in no more than 60 seconds in a timed acceptance test. |
| NFR-USE-002 | Every status, amount, and discrepancy type shall have a visible plain-language explanation. |
| NFR-USE-003 | Destructive or irreversible actions shall require confirmation and shall state the financial effect. |
| NFR-USE-004 | Critical finance workflows shall be operable by keyboard and shall expose programmatic labels for form controls and status indicators. |
| NFR-USE-005 | Colour shall not be the only means used to communicate reconciliation state or severity. |
| NFR-USE-006 | Date, time, amount, and currency formatting shall be consistent and shall show the source time zone where ambiguity could affect reconciliation. |

---

## 11. Business-Rule Dependency

The authoritative catalogue is `BUSINESS_RULES.md`.

No API, database write path, background job, or user-interface action may bypass a rule merely because the same rule is checked elsewhere. Critical invariants require server-side enforcement and automated tests.

---

## 12. Acceptance-Criteria Dependency

Detailed scenarios are in `ACCEPTANCE_CRITERIA.md`.

A P0 feature is not complete until:

1. Its referenced acceptance scenarios pass.
2. Its business-rule tests pass.
3. Authorisation tests pass.
4. Audit evidence is produced where required.
5. Relevant failure behavior is demonstrated.
6. Documentation is updated.

---

## 13. MVP Release Exit Criteria

The MVP may be called complete only when:

- All P0 backlog items are accepted.
- All P0 user stories have passing acceptance criteria.
- The controlled dataset contains:
  - At least 100 bookings.
  - At least 250 booking items.
  - At least 400 financial events.
  - At least 150 supplier obligations.
  - At least 25 intentional discrepancies covering every P0 discrepancy type.
- Automatic reconciliation coverage is at least 80% for eligible, unambiguous acceptance records.
- No material false-positive reconciliation occurs in the independently reviewed dataset.
- All intentional material discrepancies are detected.
- Duplicate re-import produces no additional financial effect.
- Cross-role and cross-organisation authorisation tests pass.
- Backup and restore evidence meets the declared RPO and RTO.
- A new environment can be started using the documented procedure.
- The booking financial state can be explained in under 60 seconds by a trained reviewer.
- Known limitations and unvalidated market assumptions remain documented.

---

## 14. Requirements Assumptions

The following remain provisional until direct-user validation:

- CSV import is acceptable for an initial pilot.
- Supplier obligations are sufficiently important to differentiate the product.
- A separate control layer is adoptable beside existing systems.
- Default matching windows and tolerances reflect actual workflows.
- The chosen discrepancy categories cover most high-value exceptions.
- The base-currency margin calculation is useful to target users.
- The target volume is representative of the first customer segment.

Requirements affected by these assumptions must be easy to adjust without rewriting unrelated financial invariants.

---

## 15. Traceability Summary

| Product outcome | Main requirements |
|---|---|
| Import fragmented records safely | FR-010–FR-029 |
| Create one booking-level financial view | FR-030–FR-067 |
| Match and reconcile deterministically | FR-070–FR-079 |
| Manage exceptions explicitly | FR-080–FR-088 |
| Explain every result | FR-065, FR-074, FR-081, FR-090–FR-092 |
| Support management and accounting review | FR-093–FR-098 |
| Protect financial and tenant data | NFR-SEC, NFR-PRI, access matrix |
| Operate reliably | NFR-REL, NFR-PERF, NFR-OBS, NFR-DEP |

---

## 16. Stage 2 Exit Assessment

This requirements baseline satisfies the written Stage 2 exit gate when every P0 backlog item:

- Has a named user outcome.
- References one or more user stories.
- References acceptance criteria.
- Identifies relevant business rules and edge cases.
- Has a size no larger than one short iteration.
- Has explicit exclusions where scope could expand.

Direct-user validation can still change priorities and terminology. It must not silently weaken financial integrity, auditability, security, or deterministic matching rules.
