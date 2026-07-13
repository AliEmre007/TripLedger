# PROJECT_BRIEF.md

## Project Initiation Brief

**Working product name:** TripLedger  
**Project type:** B2B tourism operations and financial reconciliation platform  
**Document stage:** Stage 0 — Project Initiation  
**Status:** Draft baseline for discovery  
**Owner:** Ali Emre GÜŞLÜ  
**Date:** 13 July 2026  
**Version:** 0.1

---

## 1. Project Purpose

TripLedger is intended to help small and medium-sized tourism businesses understand the operational and financial state of every booking without manually combining data from booking portals, spreadsheets, payment providers, bank records, messages, and accounting tools.

The project will initially focus on one narrow and costly problem:

> Tourism operators cannot reliably reconcile what was booked, what the customer paid, what each sales channel deducted, what suppliers are owed, what was refunded, and what profit remains.

The purpose of the first version is not to replace every tourism system. It is to create a trusted booking-level financial and operational record that exposes discrepancies early and reduces manual reconciliation work.

---

## 2. Plain-Language Problem Statement

### Who experiences the problem?

The initial target users are:

- Owners and general managers of small tourism businesses.
- Finance and accounting employees responsible for booking settlements.
- Reservation and operations employees who coordinate bookings across multiple channels.
- Tour operators and destination-management companies that pay multiple suppliers for one customer booking.
- Boutique hotels that receive reservations from direct and third-party sales channels.

### What happens today?

A typical tourism business may receive bookings through online travel agencies, direct website forms, telephone calls, messaging applications, travel agencies, or spreadsheets.

The related financial information is then spread across:

- Reservation systems.
- OTA extranets and settlement reports.
- Payment gateways.
- Bank statements.
- Supplier invoices.
- Accounting software.
- Excel files.
- Email and WhatsApp conversations.

Employees manually compare these sources to determine:

- Whether the customer paid the correct amount.
- Whether the sales channel deducted the correct commission.
- Whether a payment settlement reached the company.
- Whether the correct supplier amount is due.
- Whether a cancellation or refund was processed correctly.
- Whether a booking is actually profitable.
- Whether duplicate, missing, or inconsistent records exist.

### Why is this costly or risky?

The current process creates:

- Significant manual work.
- Delayed detection of missing or incorrect settlements.
- Incorrect supplier payments.
- Inconsistent refund calculations.
- Poor visibility into booking-level profit.
- Duplicate or incomplete records.
- Dependence on individual employees and undocumented spreadsheets.
- Weak auditability.
- Higher operational risk during peak tourism periods.
- Difficulty scaling the business without adding administrative staff.

---

## 3. Product Hypothesis

We believe that a tourism operator using a unified booking-level reconciliation platform can reduce manual financial comparison work, detect settlement discrepancies earlier, and understand the real financial outcome of each booking.

This hypothesis will be tested before broadening the platform into a complete tourism operating system.

---

## 4. Target Market

### Initial market segment

The first version will target small and medium-sized tourism operators that:

- Process bookings through more than one sales channel.
- Receive customer payments in one or more currencies.
- Pay commissions, suppliers, guides, hotels, activity providers, or transfer companies.
- Use spreadsheets or manual processes for reconciliation.
- Do not have a dedicated internal software engineering team.

### Priority customer profiles

1. Tour and activity operators.
2. Destination-management companies.
3. Airport transfer companies selling through multiple channels.
4. Boutique hotels with approximately 10–100 rooms.
5. Travel agencies selling multi-supplier packages.
6. Medical-tourism coordinators.

### Initial geographic perspective

The project should be usable for tourism businesses operating in Türkiye while avoiding assumptions that prevent later adoption by international operators.

The first version may use Türkiye-based scenarios and currencies for testing, but the core domain must not depend on a single country, sales channel, or currency.

---

## 5. Primary Users

### 5.1 Finance or reconciliation specialist

**Goal:** Confirm that every booking, payment, commission, refund, and supplier obligation is financially consistent.

**Current pain:**

- Compares reports manually.
- Does not know which source is authoritative.
- Detects discrepancies late.
- Cannot easily explain how a final settlement was calculated.

### 5.2 Operations or reservations employee

**Goal:** Know the current operational and payment status of a booking.

**Current pain:**

- Switches between multiple portals.
- Relies on messages and spreadsheets.
- Cannot easily distinguish a payment issue from an operational issue.
- May act on stale booking information.

### 5.3 Owner or general manager

**Goal:** Understand revenue, obligations, discrepancies, and actual booking profitability.

**Current pain:**

- Receives delayed or incomplete reports.
- Cannot inspect booking-level financial details.
- Depends heavily on individual employees.
- Has limited visibility into leakage and process inefficiency.

### 5.4 Accountant or external financial adviser

**Goal:** Receive consistent records and supporting evidence.

**Current pain:**

- Receives manually prepared data.
- Must resolve inconsistent terminology and identifiers.
- Cannot easily trace adjustments back to the original booking.

---

## 6. Business Value

The intended business value is to:

- Reduce time spent on manual reconciliation.
- Detect missing, incorrect, or duplicate settlements.
- Improve booking-level profit visibility.
- Reduce incorrect supplier payouts.
- Improve cancellation and refund accuracy.
- Establish an auditable record of financial changes.
- Reduce dependence on spreadsheets and employee memory.
- Enable a tourism company to grow without increasing administrative work at the same rate.
- Create reliable data that can later support forecasting, automation, and AI-assisted operations.

---

## 7. First-Version Outcome

At the end of the first version, an authorized employee should be able to open a booking and answer:

1. Where did this booking originate?
2. What was the original selling price?
3. Which currency was used?
4. What did the customer pay?
5. What fees or commissions were deducted?
6. What amount was settled to the business?
7. Which suppliers are owed money?
8. Has a cancellation, refund, or adjustment occurred?
9. Are any amounts missing, duplicated, or inconsistent?
10. What is the current estimated gross margin?
11. Which source records and user actions produced the current result?

The user must be able to answer these questions without reconstructing the booking manually across several systems.

---

## 8. Initial Scope

### 8.1 Included in the first version

The first version will include:

- Import of reservation data from:
  - One structured external booking source.
  - One CSV-based or simulated secondary source.
  - Manual entry for exceptional bookings.
- A normalized internal booking record.
- Customer, booking, channel, currency, and supplier references.
- Import of payment or settlement data from:
  - One payment provider, bank-export format, or realistic simulator.
- Booking-level calculation of:
  - Gross selling amount.
  - Sales-channel commission.
  - Payment-processing fee.
  - Refunds and adjustments.
  - Supplier obligations.
  - Net settlement.
  - Estimated gross margin.
- Matching between bookings, payments, settlements, refunds, and supplier obligations.
- Detection and classification of discrepancies.
- Support for:
  - Full cancellation.
  - Partial cancellation.
  - Full refund.
  - Partial refund.
  - Settlement adjustment.
- A reconciliation dashboard.
- A booking-level financial timeline.
- Daily or on-demand discrepancy reporting.
- Role-based access for at least:
  - Administrator.
  - Finance user.
  - Operations user.
  - Read-only manager.
- Audit history for material financial and operational changes.
- An acceptance dataset containing valid and intentionally inconsistent booking scenarios.
- Professional project evidence:
  - Requirements.
  - Architecture decisions.
  - Domain model.
  - API contract.
  - Tests.
  - Deployment documentation.
  - Operational runbook.

### 8.2 Conditional scope

The following may be included only when required to prove the primary workflow and when they do not delay the reconciliation capability:

- Email notification for unresolved discrepancies.
- Simple multi-currency conversion using recorded exchange rates.
- Supplier payment status tracking.
- Export of reconciled records for accounting review.

---

## 9. Explicit Exclusions

The first version will not attempt to provide:

- A consumer travel marketplace.
- Flight, hotel, or activity search for travellers.
- A complete property-management system.
- A complete channel manager.
- Automated room or seat inventory distribution across every OTA.
- A full accounting or enterprise-resource-planning system.
- Official tax filing or jurisdiction-specific accounting compliance.
- Direct custody of customer funds.
- Banking services or regulated money transmission.
- Card-data storage.
- A payment gateway built from scratch.
- Automated supplier fund transfers.
- A dynamic-pricing engine.
- An AI travel concierge.
- A generic chatbot as the primary product.
- Native Android or iOS applications.
- Payroll, human resources, or staff scheduling.
- IoT room or hotel-device management.
- Advanced forecasting or machine-learning models.
- Support for every OTA or tourism provider.
- Microservices, streaming platforms, or distributed infrastructure unless later requirements prove they are necessary.

These exclusions protect the first version from becoming a broad tourism-management suite before the core reconciliation problem has been validated.

---

## 10. Success Metrics

Success will be evaluated with product, operational, quality, and portfolio metrics.

### 10.1 Product and user-value metrics

| Metric | First-version target | Reason |
|---|---:|---|
| Reconciliation coverage | At least 90% of eligible records in the acceptance dataset are automatically matched | The system must remove substantial manual comparison work |
| Discrepancy detection | 100% of intentionally inserted material discrepancies are identified in acceptance tests | Missing financial issues would undermine trust |
| Manual reconciliation time | At least 60% reduction compared with the documented baseline workflow | Time saving is the primary user benefit |
| Booking-level explainability | A user can trace every displayed amount to its source or adjustment | Financial output must be defensible |
| Financial status retrieval | A trained user can determine a booking's financial state in under 60 seconds | The platform must simplify investigation |
| Pilot usability | At least 4 out of 5 average rating from at least three representative reviewers or pilot users | Technical correctness alone is insufficient |
| Unresolved discrepancy visibility | All unresolved material discrepancies appear in a dedicated review queue | Problems must not remain hidden |

### 10.2 Data and correctness metrics

| Metric | First-version target | Reason |
|---|---:|---|
| Duplicate import protection | Re-importing the same source record produces no duplicate booking or financial event | External systems commonly retry or resend data |
| Financial consistency | Every completed acceptance scenario satisfies defined balance and reconciliation rules | Financial records require invariant-based validation |
| Audit coverage | 100% of material manual changes record actor, time, old value, new value, and reason where applicable | The system must be auditable |
| Import traceability | 100% of imported records retain source-system and source-record identifiers | Reconciliation requires provenance |
| Failed-import visibility | 100% of rejected records are visible with an actionable reason | Silent data loss is unacceptable |

### 10.3 Delivery and engineering evidence metrics

| Metric | First-version target | Reason |
|---|---:|---|
| Automated testing | Critical reconciliation, refund, duplicate-import, and authorization scenarios are covered | Core financial behaviour must be repeatable |
| Reproducible environment | A new developer can run the documented system from a clean environment | This demonstrates professional delivery |
| Deployment evidence | At least one production-like deployed environment is documented | The project must prove operational capability |
| Observability | Core imports, reconciliation runs, failures, and user-visible errors can be traced | Production failures must be diagnosable |
| Documentation | Project brief, requirements, architecture decisions, API contract, runbook, and test evidence are current | The repository must communicate engineering intent |

---

## 11. Stakeholders

| Stakeholder | Interest | Influence | Initial involvement |
|---|---|---:|---|
| Project owner and lead engineer — Ali Emre GÜŞLÜ | Product definition, engineering quality, portfolio and commercial value | High | Owns scope, decisions, implementation review, and delivery evidence |
| Tourism business owner or general manager | Cost reduction, visibility, business control | High | Problem interviews, workflow validation, acceptance review |
| Finance or reconciliation employee | Accurate matching and discrepancy investigation | High | Primary workflow source and usability reviewer |
| Operations or reservations employee | Booking status and operational clarity | Medium | Validates booking lifecycle and terminology |
| Accountant or financial adviser | Export quality, traceability, financial interpretation | Medium | Reviews record structure and reporting assumptions |
| External booking-channel provider | Reservation and settlement data source | Medium | Integration constraints and test data |
| Payment provider or bank-export source | Payment and settlement data source | Medium | Integration constraints and test data |
| Supplier, hotel, guide, or transfer provider | Payable amount and booking fulfilment | Low in version one | Represented through domain scenarios; no direct portal initially |
| Recruiter or engineering reviewer | Evidence of backend, financial-domain, security, testing, and DevOps capability | Medium | Reviews repository and deployed demonstration |
| AI coding assistant | Implementation acceleration under written instructions | Low decision authority | May generate code, tests, or documentation but does not own requirements or final approval |

---

## 12. Constraints

### 12.1 Engineering capacity

- The project is initially led by one engineer.
- The realistic capacity is approximately 10–12 focused hours per week while the owner is working weekdays from 08:00 to 17:00.
- AI coding assistants may accelerate implementation, but the project owner remains responsible for:
  - Requirements.
  - Domain modelling.
  - Security decisions.
  - Architecture decisions.
  - Acceptance criteria.
  - Code review.
  - Testing.
  - Final approval.
- Scope must remain achievable by a solo engineer.

### 12.2 Time constraint

- There is no external customer deadline at project initiation.
- The initial planning window for the first credible version is 12 weeks.
- The scope must be reduced before extending the timeline.
- Features that do not prove the central reconciliation hypothesis will not receive priority.

### 12.3 Budget constraint

- The project should use low-cost or free development resources where practical.
- Paid external APIs must be replaceable with simulators, sandbox environments, or test files during development.
- Infrastructure should remain suitable for a portfolio and early pilot rather than premature large-scale operation.

### 12.4 Skills constraint

The project should build on the owner's existing strengths in:

- Backend development.
- Java and Spring-based systems.
- PostgreSQL and transactional data.
- Docker and Linux.
- Authentication and authorization concepts.
- CI/CD and deployment.
- Financial ledger concepts.
- AI-assisted engineering workflows.

New technologies must be introduced only when they solve a documented requirement or create necessary professional evidence.

### 12.5 Data constraint

- Real customer, passport, payment-card, or confidential business data will not be required for development.
- Development and demonstration will use synthetic or properly anonymized data.
- Imported data must preserve provenance.
- Sensitive values must not be committed to the repository.

### 12.6 Regulatory and compliance constraint

The first version:

- Is not a bank.
- Does not hold or transmit customer money.
- Does not store raw payment-card data.
- Does not provide official accounting or tax advice.
- Does not claim certification for payment-card, banking, or country-specific tourism regulation.
- Must still follow privacy-by-design, least privilege, secure secrets management, and auditable-change principles.

Potential obligations such as GDPR, Türkiye's KVKK, PCI DSS boundaries, tax rules, and payment regulations must be assessed before processing real production data.

### 12.7 Integration constraint

- The first version will prove the model using a small number of integrations.
- It must tolerate unavailable, duplicated, delayed, or malformed external records.
- The domain model must not be tightly coupled to one OTA, payment provider, or file format.
- Full production access to third-party APIs may not be available; realistic adapters and simulators are acceptable for initial validation.

### 12.8 Infrastructure constraint

- The first version must be deployable on modest infrastructure.
- The design must not require Kubernetes, Kafka, multiple regions, or a microservice architecture to function.
- Reliability, data integrity, backups, and observability are required, but infrastructure complexity must remain proportional to the first-version workload.

---

## 13. Assumptions to Validate

The following statements are assumptions, not confirmed facts:

1. Small tourism operators perform substantial reconciliation work manually.
2. Booking-level profitability is not clearly visible in their current systems.
3. Commission, settlement, refund, and supplier records can be matched through common identifiers and business rules.
4. Users will trust automated matching when every result is explainable.
5. A focused reconciliation product is more valuable than another generic booking or chatbot product.
6. Tourism businesses will provide sample reports or anonymized workflow examples during discovery.
7. At least one target user can be reached for workflow interviews and acceptance feedback.
8. The first customer segment can be served without building a complete PMS or channel manager.
9. A user will pay for the product when recovered revenue or saved staff time exceeds the subscription cost.

Each assumption must later be validated through interviews, sample documents, workflow observation, or prototype testing.

---

## 14. Initial Risks

| Risk | Consequence | Initial response |
|---|---|---|
| The project becomes a complete tourism platform | Scope becomes unmanageable | Enforce explicit exclusions and the reconciliation-first boundary |
| Real provider APIs are inaccessible | Integration cannot be demonstrated | Use documented sandbox APIs, CSV imports, and realistic simulators |
| Tourism accounting rules differ significantly by company | Domain model becomes too generic or incorrect | Interview representative users and separate configurable rules from invariants |
| Matching quality is poor because identifiers are inconsistent | Users cannot trust automation | Preserve source provenance and support explainable confidence and manual review |
| Financial calculations are incorrect | Severe loss of trust | Use invariant-based tests, realistic scenarios, and independent review |
| AI-generated code introduces hidden defects | Incorrect or insecure behaviour | Use small changes, code review, automated tests, and written acceptance criteria |
| Compliance scope expands unexpectedly | Production use becomes risky | Avoid regulated activity and perform legal/compliance assessment before pilots with real data |
| No access to real users | Product becomes speculative | Treat user interviews and sample workflow evidence as a required discovery gate |
| Too many technologies are introduced | Delivery slows and reliability decreases | Require a documented problem before adding a major technology |

---

## 15. Initial Evidence Required Before Implementation Expansion

The project should gather the following evidence during discovery:

- At least three interviews with representative tourism users, preferably including:
  - One owner or manager.
  - One finance or reconciliation employee.
  - One operations or reservation employee.
- At least two anonymized or synthetic examples of:
  - Booking reports.
  - Settlement reports.
  - Commission calculations.
  - Cancellation or refund cases.
- A documented current-state reconciliation workflow.
- A baseline estimate of:
  - Time spent per day or week.
  - Number of records handled.
  - Common discrepancy types.
  - Average delay before discrepancies are found.
- A glossary of tourism and finance terms used by target users.
- Confirmation that the first-version workflow solves a problem users consider important.

---

## 16. Version-One Boundary Statement

Version one is:

> A secure B2B application that imports a limited set of booking and payment records, normalizes them, calculates booking-level financial obligations, matches related records, exposes discrepancies, and provides an auditable explanation of the current financial state of each booking.

Version one is not:

> A complete reservation marketplace, property-management platform, accounting system, payment institution, channel manager, AI concierge, or enterprise tourism suite.

---

## 17. Exit Gate Checklist

Stage 0 is complete when the following statements can be answered clearly without naming a technology stack:

- [x] We can state who experiences the problem.
- [x] We can describe the current manual workflow.
- [x] We can explain why the problem is costly or risky.
- [x] We can identify the primary and secondary users.
- [x] We can describe the expected business outcome.
- [x] We have measurable first-version success criteria.
- [x] We have explicit first-version scope.
- [x] We have explicit exclusions.
- [x] We have recorded engineering, time, budget, data, regulatory, integration, and infrastructure constraints.
- [x] We have identified the principal stakeholders.
- [x] We can state the version-one boundary without relying on a framework, database, message broker, cloud provider, or architectural style.
- [ ] At least one representative tourism professional has reviewed the problem statement.
- [ ] At least one real or anonymized reconciliation workflow has been examined.
- [ ] The main assumptions have been ranked for discovery.

The written initiation brief is complete. The project should not be considered fully validated for implementation expansion until the final three discovery items are completed.

---

## 18. Initiation Decision

**Decision:** Proceed to problem discovery and requirements elicitation with a reconciliation-first scope.

**Do not decide yet:**

- Programming language.
- Framework.
- Database product.
- Microservices versus modular monolith.
- Message broker.
- Cloud provider.
- AI model.
- Mobile application strategy.

Those decisions must follow validated requirements and explicit quality attributes.

---

## 19. One-Sentence Project Definition

> TripLedger helps small tourism operators reconcile bookings, payments, commissions, refunds, and supplier obligations so that they can detect discrepancies early and understand the real financial outcome of every booking.
