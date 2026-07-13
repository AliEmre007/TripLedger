# OUT_OF_SCOPE.md

## TripLedger MVP Explicit Exclusions

**Version:** 0.1  
**Date:** 13 July 2026

This list protects the Stage 2 boundary. An excluded capability requires a documented scope-change decision before implementation.

---

## 1. Consumer and Marketplace Features

The MVP does not include:

- Consumer travel search.
- Hotel, flight, transfer, or activity marketplace.
- Consumer itinerary planning.
- Public supplier catalogue.
- Traveller reviews.
- Loyalty programme.
- Consumer mobile application.
- Public booking checkout.
- Marketing campaign management.

**Reason:** These features do not test booking-level financial reconciliation.

---

## 2. Booking and Tourism Operations Replacement

The MVP is not:

- A property-management system.
- A central reservation system.
- A channel manager.
- A tour booking engine.
- A complete DMC itinerary builder.
- A resource or vehicle scheduler.
- A guide-management platform.
- A staff scheduling or payroll system.
- A customer-support platform.
- A supplier extranet.

It imports representative records from those systems.

---

## 3. Payment and Banking Scope

The MVP does not:

- Act as a payment gateway.
- Store raw payment-card information.
- Hold or transmit customer funds.
- Initiate bank transfers or supplier payouts.
- Provide wallets or stored value.
- Execute foreign-exchange trades.
- Provide credit, lending, or insurance.
- Perform chargeback representation.
- Connect to production open-banking APIs.
- Claim payment-industry certification.

It records financial evidence and expected outcomes only.

---

## 4. Accounting and Compliance Scope

The MVP does not:

- Replace statutory accounting software.
- Post directly to a government tax system.
- Produce certified tax returns.
- Determine VAT or other tax liability.
- Provide legal, accounting, or regulatory advice.
- Claim GDPR, KVKK, PCI, or financial-regulatory certification.
- Implement country-specific chart-of-accounts logic.
- Perform formal revenue recognition.
- Produce audited financial statements.

The estimated gross margin is operational and explicitly non-statutory.

---

## 5. Integration Scope

The MVP excludes production connectors for:

- Booking.com.
- Expedia.
- Airbnb.
- GetYourGuide.
- Viator.
- Bókun.
- FareHarbor.
- Rezdy.
- Mews.
- Cloudbeds.
- SiteMinder.
- Stripe.
- Adyen.
- iyzico.
- Turkish or international banks.
- Xero.
- QuickBooks.
- Logo.
- Paraşüt.
- Other accounting products.

Versioned CSV templates and simulators represent these categories.

---

## 6. AI and Analytics Scope

The MVP excludes:

- AI chatbot as the main interface.
- AI-controlled refunds, cancellation, matching, or financial adjustment.
- Generative financial explanations without deterministic source evidence.
- Probabilistic automatic matching.
- Demand forecasting.
- Dynamic pricing.
- Recommendation engine.
- Fraud scoring.
- Predictive supplier or cancellation risk.
- Large data warehouse.
- Real-time streaming analytics.

Rule-based explanation and deterministic matching are included.

---

## 7. Infrastructure Scope

The MVP does not require:

- Microservices.
- Kafka.
- Kubernetes.
- Multi-region active-active deployment.
- Internet-scale throughput.
- Event sourcing as a universal architecture.
- Separate database per module.
- Service mesh.
- Complex data lake.
- Dedicated mobile backend.
- Continuous production traffic migration.

These may be considered only when measurable requirements justify them.

---

## 8. User and Organisation Scope

The MVP does not include:

- Self-service public registration.
- Subscription billing.
- Automated tenant provisioning.
- Reseller administration.
- White labelling.
- Complex custom roles.
- Attribute-based access policy editor.
- Supplier or customer direct login.
- Enterprise single sign-on.
- More than one role per user.

The model must not prevent later multi-organisation SaaS operation, but commercial onboarding is excluded.

---

## 9. Data Scope

The MVP does not require:

- Passport details.
- Identity-document scans.
- Full traveller profiles.
- Medical-tourism health records.
- Raw card numbers.
- Bank credentials.
- Message or email inbox ingestion.
- Unstructured OCR of invoices.
- Permanent storage of source files beyond the documented pilot retention policy.
- Historical migration of an entire company's records.

Synthetic and anonymised data are used for development and demonstration.

---

## 10. Scope-Change Gate

An excluded item may enter scope only when:

1. Direct user evidence identifies it as necessary for the target outcome.
2. The existing P0 workflow cannot meet its acceptance criteria without it.
3. Security, privacy, regulatory, and operational effects are documented.
4. The feature is decomposed to one-iteration slices.
5. A lower-priority item is removed or the delivery constraint is explicitly changed.
6. Product requirements, business rules, user stories, acceptance criteria, and backlog are updated together.
