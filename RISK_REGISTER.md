# RISK_REGISTER.md

## TripLedger Risk Register

**Stage:** 3 — Risk, Feasibility, and Constraints  
**Owner:** Ali Emre GÜŞLÜ  
**Date:** 13 July 2026  
**Version:** 0.1

### Scoring

- Likelihood: 1 rare, 2 unlikely, 3 possible, 4 likely, 5 almost certain.
- Impact: 1 negligible, 2 minor, 3 material, 4 major, 5 project-threatening.
- Score: likelihood × impact.
- Priority treatment: score ≥15 or impact 5.

The score prioritises attention; it does not make a legal, privacy, or tenant-isolation risk acceptable merely because likelihood is low.

| ID | Category | Risk | L | I | Score | Consequence | Treatment | Owner | Trigger / evidence | Status |
|---|---|---|---:|---:|---:|---|---|---|---|---|
| R-001 | Product | Direct-user validation remains incomplete | 4 | 5 | 20 | Avoid solving a plausible but low-priority workflow | Complete five interviews, two real workflows, ten exceptions before production connectors | Product owner | Before Stage 4 integration commitments | OPEN |
| R-002 | Product | Existing hotel/tour platforms may already solve the target workflow adequately | 3 | 5 | 15 | Weak differentiation and no adoption | Perform side-by-side report walkthroughs with target operators; define migration-free gap | Product owner | User says current report already answers the 12 product questions | OPEN |
| R-003 | Product | Operators may reject an additional control layer | 4 | 4 | 16 | No adoption despite technical value | Prototype import-to-discrepancy workflow; test installation effort and buying objection | Product owner | User requires replacement rather than companion product | OPEN |
| R-004 | Product | Supplier-obligation reconciliation may not be a strong differentiator | 3 | 4 | 12 | MVP complexity without commercial value | Rank pain points in interviews; remove supplier workflow if low frequency/value | Product owner | Fewer than 2/5 users rank it in top three | OPEN |
| R-005 | Product | Saved time or recovered value may not justify subscription | 3 | 5 | 15 | No sustainable business case | Measure baseline hours, discrepancy value, close delay, and willingness-to-pay conditions | Product owner | Benefit is below indicative operating cost | OPEN |
| R-010 | Technical | Canonical model cannot represent provider diversity | 3 | 5 | 15 | Repeated schema changes and incorrect economics | Use source payload preservation, mapping layer, fixtures from multiple source categories, and extension fields with governance | Domain owner | Third sample requires destructive core-model change | MITIGATED |
| R-011 | Technical | External identifiers are missing, reused, or inconsistent | 4 | 4 | 16 | Low match coverage and duplicate risk | Require composite provenance; support unmatched records; never auto-match on name alone | Domain owner | More than 20% sample records lack reliable references | MITIGATED |
| R-012 | Technical | Versionless sources cannot prove stale versus current changed payload | 4 | 4 | 16 | Old data may replace newer business state | Use checksum for duplicate prevention, ingestion sequence, effective timestamp, and manual conflict review; PoC-001 records limitation | Domain owner | Changed checksum arrives with older effective date | ACCEPTED FOR MVP |
| R-013 | Technical | False-positive automatic matching | 3 | 5 | 15 | Incorrect financial closure | Unique deterministic rules only; ambiguity remains open; independently review auto-matched dataset | Finance/domain owner | Any material false positive in acceptance dataset | MITIGATED |
| R-014 | Technical | Concurrent allocations exceed available amount | 3 | 5 | 15 | Double use of payment or settlement | Transactional conservation check plus database locking/constraint strategy; repeat PoC-004 on selected DB | Technical lead | Concurrent acceptance test overallocates once | MITIGATED |
| R-015 | Technical | Rounding or currency conversion creates incorrect margins | 3 | 5 | 15 | Financial mistrust and reporting differences | Exact decimal, explicit rate evidence, versioned rounding policy, boundary tests; PoC-002 | Finance/domain owner | Same inputs produce different totals across paths | MITIGATED |
| R-016 | Technical | Reconciliation recalculation is non-deterministic | 2 | 5 | 10 | Results change without business input | Pure/versioned calculation core, deterministic ordering, replay tests, immutable inputs | Technical lead | Unchanged rerun changes status or totals | OPEN |
| R-017 | Security | CSV exports trigger spreadsheet formula execution | 3 | 4 | 12 | Code execution or data exfiltration on reviewer device | Neutralise formula-leading cells and document export contract; PoC-006 | Security owner | Export contains unescaped =,+,-,@,TAB,CR prefix | MITIGATED |
| R-018 | Technical | Import/reconciliation performance misses pilot targets | 2 | 3 | 6 | Slow workflow and failed demos | Benchmark representative dataset after architecture; current algorithmic baseline is not production proof | Technical lead | 10k import or reconciliation exceeds NFR | OPEN |
| R-020 | Security | Broken organisation isolation exposes another customer's data | 2 | 5 | 10 | Severe privacy and trust breach | Organisation-scoped keys, server authorisation, query tests, cache/export scoping, PoC-005 | Security owner | Any cross-org read/write in automated tests | MITIGATED |
| R-021 | Security | Broken object-level authorisation or privilege escalation | 3 | 5 | 15 | Unauthorised financial action | Deny-by-default policy, role matrix, endpoint tests, ASVS-aligned verification | Security owner | Operations can call Finance endpoint directly | OPEN |
| R-022 | Security | Malicious or oversized import file abuses parser/storage | 3 | 4 | 12 | DoS, injection, or malware persistence | CSV only, limits, content validation, streaming parser, non-executable storage, timeouts | Security owner | Parser memory/CPU exceeds controlled bound | OPEN |
| R-023 | Security | Audit records can be altered or omitted | 2 | 5 | 10 | No trustworthy investigation evidence | Append-only write path, restricted DB role, integrity monitoring, mandatory audit acceptance tests | Security owner | Financial action without audit event | OPEN |
| R-024 | Security | Secrets or restricted data leak through source files, logs, errors, backups, or exports | 3 | 5 | 15 | Credential compromise or privacy breach | Data minimisation, structured redaction, secret manager, log tests, encrypted backup, export scope | Security owner | Secret scanner or log test detects restricted value | OPEN |
| R-025 | Security | Identity-provider or MFA configuration is incorrect | 3 | 4 | 12 | Account takeover or inaccessible users | Use documented realm configuration, configuration-as-code, recovery procedure, integration tests | Security owner | Finance access succeeds without required MFA | OPEN |
| R-026 | Security | Dependency or build-chain compromise | 3 | 4 | 12 | Malicious release or vulnerable component | Locked dependencies, SBOM, signature/checksum verification where supported, SCA and image scans | Technical lead | Critical unresolved finding at release | OPEN |
| R-030 | Data | Source files include unnecessary customer or passport data | 4 | 4 | 16 | Expanded legal/security exposure | Publish minimal templates, reject prohibited columns where possible, anonymised fixtures, retention policy | Data owner | Pilot file includes restricted fields | MITIGATED |
| R-031 | Data | Raw source-file retention is undefined | 3 | 4 | 12 | Over-retention or loss of evidence | Define purpose-specific retention, encryption, deletion job, legal review, and manifest | Data owner | No approved retention period before pilot | OPEN |
| R-032 | Data | Data lineage is lost during normalisation or correction | 3 | 5 | 15 | Unexplainable financial result | Preserve batch, row, external ID, checksum, version, rule version, reversal links | Domain owner | Displayed amount cannot trace to accepted source | MITIGATED |
| R-033 | Operations | Backup exists but restore is not usable | 3 | 5 | 15 | Permanent data loss or long outage | Automated backup manifest and scheduled restore rehearsal; AC-072 | Operations owner | Restore rehearsal fails or exceeds RTO | OPEN |
| R-040 | Operations | Background jobs silently stall or repeatedly retry | 3 | 4 | 12 | Stale financial state | Bounded retry, terminal failure, metrics, alert, idempotency, operator runbook | Operations owner | Job age exceeds threshold without visible state | OPEN |
| R-041 | Operations | Insufficient observability prevents diagnosis | 3 | 4 | 12 | Slow incident resolution and unsafe manual fixes | Correlation IDs, structured logs, metrics, health/readiness, error catalogue | Operations owner | Failure requires direct DB modification to diagnose | OPEN |
| R-042 | Delivery | Project depends on one engineer's undocumented knowledge | 4 | 4 | 16 | Maintenance and delivery interruption | ADRs, runbooks, fixtures, small commits, automated tests, agent instructions | Project owner | Another engineer cannot reproduce environment | OPEN |
| R-050 | Integration | Production provider API access is unavailable or commercially restricted | 4 | 3 | 12 | Connector delays | CSV/simulator-first MVP; API access becomes separate go/no-go gate | Product owner | Provider denies sandbox or contract access | REMOVED FROM FIRST RELEASE |
| R-051 | Integration | Provider schema or semantics change | 4 | 4 | 16 | Broken imports or incorrect mappings | Versioned adapters, contract fixtures, unknown-field tolerance, mapping monitoring | Integration owner | New sample fails current contract | OPEN |
| R-052 | Integration | Time-zone and business-date semantics differ by source | 4 | 4 | 16 | False timing discrepancies and period mismatch | Retain source time zone and original timestamp; define event/effective/service/settlement dates separately | Domain owner | Same event moves period after normalisation | OPEN |
| R-053 | Integration | Generic accounting export does not fit accountant workflow | 3 | 4 | 12 | Manual rework remains | Validate export columns with accountant; keep versioned generic export; provider mapping later | Product owner | Accountant cannot tie export to evidence | OPEN |
| R-060 | Delivery | Stage 2 P0 backlog does not fit the 12-week capacity constraint | 5 | 5 | 25 | Incomplete or low-quality release | Split 12-week validation release from full MVP; full P0 estimate is 160–320h, likely 240h versus 120–144h capacity | Project owner | Any plan assumes all 32 slices in 12 weeks | MITIGATED BY RESCOPE |
| R-061 | Delivery | AI-generated changes pass superficial review but violate invariants | 4 | 5 | 20 | Hidden financial or security defects | Codex-ready specs, small change sets, mandatory tests, human review, no agent-owned approval | Technical lead | Generated code changes financial rule without linked test | OPEN |
| R-062 | Delivery | Acceptance dataset is too synthetic to expose real edge cases | 4 | 4 | 16 | Demo passes but pilot fails | Add anonymised real structures and exception patterns before pilot | Product owner | Real sample requires new core rule | OPEN |
| R-063 | Delivery | Too many quality controls are postponed until the end | 3 | 4 | 12 | Late security, recovery, and observability failures | Build vertical slices with audit, tests, errors, and metrics from first import | Technical lead | Feature marked done without operational evidence | OPEN |
| R-070 | Legal | Controller/processor roles and lawful basis are not established for a pilot | 3 | 5 | 15 | Unlawful processing or contractual exposure | Legal review, data-flow map, DPA terms, minimise personal data, synthetic data until approved | Data owner | Real personal data requested before approval | OPEN |
| R-071 | Legal | Cloud, identity, or support providers create cross-border transfer obligations | 3 | 5 | 15 | Regulatory or contractual non-compliance | Identify locations/subprocessors, legal transfer assessment, configurable hosting, no real data before review | Data owner | Chosen provider stores or accesses personal data abroad | OPEN |
| R-072 | Legal | Retention, deletion, and data-subject response duties are undefined | 3 | 4 | 12 | Over-retention and inability to fulfil requests | Retention schedule, deletion/anonymisation design, export/search capability, legal review | Data owner | Pilot starts without retention owner and schedule | OPEN |
| R-073 | Legal/Security | Future payment integration expands PCI DSS scope | 3 | 5 | 15 | Significant security and compliance burden | Do not store/process raw card data; use hosted/tokenised provider flows; perform scope assessment before connector | Security owner | System receives cardholder or sensitive authentication data | REMOVED FROM FIRST RELEASE |
| R-074 | Legal/Integration | Third-party reports or API data cannot be stored/repurposed under contract | 3 | 4 | 12 | Breach of provider terms | Review provider terms, customer authority, data retention, and redistribution restrictions before connector | Product owner | Connector design assumes unrestricted data rights | OPEN |

## Treatment Decision Summary

### Removed from the first release

- Production provider API integrations.
- Direct payment processing or raw card-data handling.
- Automated payouts.
- Statutory accounting behaviour.
- Probabilistic or AI-controlled financial matching.

### Tested through proof of concept

- Source identity and idempotency: POC-001.
- Exact money and FX rounding: POC-002.
- Unique deterministic matching and ambiguity: POC-003.
- Concurrent allocation conservation: POC-004.
- Tenant-scoped relational references: POC-005.
- Spreadsheet formula-injection defence: POC-006.

### Must be validated before a real-data pilot

- Direct-user workflow and commercial value.
- Legal role and lawful processing basis.
- Retention and deletion schedule.
- Hosting and cross-border data-flow assessment.
- Production-database concurrency test.
- Real source-file compatibility.
- Restore rehearsal.
- Independent authorisation and tenant-isolation test.

## Review Cadence

- Review at the start and end of every iteration.
- Review immediately after a new source format, security incident, failed acceptance test, or scope change.
- A risk may close only when evidence is linked; “implemented” alone is not evidence.
