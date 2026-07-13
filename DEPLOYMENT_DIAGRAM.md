# TripLedger Deployment Diagram

**Stage:** 4 - Domain Modeling and Architecture  
**Date:** 13 July 2026  
**Version:** 0.1  
**Status:** Deployment baseline for validation release

## 1. Deployment Principle

The validation release uses the simplest production-like topology that proves security, data integrity, recovery, and observability.

No Kubernetes, Kafka, service mesh, multi-region active-active deployment, or microservices are required.

## 2. Production-like Pilot Topology

```mermaid
flowchart TD
    User[Authenticated User Browser]
    IdP[OIDC Identity Provider]
    App[TripLedger Modular Monolith]
    DB[(Relational Database)]
    Obj[(Upload and Export Storage)]
    Logs[Structured Logs]
    Metrics[Metrics and Health Monitoring]
    Backup[(Encrypted Backup Storage)]

    User -->|HTTPS| App
    User -->|OIDC login| IdP
    IdP -->|Signed token claims| App
    App -->|SQL over private network| DB
    App -->|Store/read uploaded CSV and exports| Obj
    App -->|Structured events| Logs
    App -->|Metrics and readiness| Metrics
    DB -->|Scheduled backup + manifest| Backup
    Obj -->|Scheduled backup or retention copy| Backup
```

## 3. Runtime Components

| Component | Responsibility | First-release requirement |
|---|---|---|
| Browser client | User interface | No secrets, no authority decisions |
| TripLedger application | API, domain modules, background jobs, health checks | One deployable application |
| OIDC identity provider | Authentication, passwords, MFA | Mature provider; standard OIDC claims |
| Relational database | Canonical state, financial records, audit, jobs | Transactional, backed up, migration-controlled |
| Object/file storage | Uploaded files and generated exports | Non-executable path, retention controlled |
| Logs and metrics | Diagnosis and operational evidence | Correlation id, no restricted data |
| Backup storage | Recovery evidence | Encrypted, access-controlled, restore tested |

## 4. Local Development Topology

```text
Developer machine
    TripLedger application
    Local relational database container
    Local identity provider or test OIDC configuration
    Local file/object storage directory
    Synthetic acceptance fixtures
```

Goal: a new developer can start the documented environment within 30 minutes excluding initial software downloads.

## 5. Environments

| Environment | Data | Purpose |
|---|---|---|
| Local | synthetic fixtures | Development and automated tests |
| CI | synthetic fixtures | Tests, scans, migrations |
| Demo | synthetic/anonymised data only | Portfolio and validation demonstration |
| Pilot | real data only after gates | Controlled customer validation |

No real personal or confidential customer data enters local, CI, or public demo environments.

## 6. Deployment Flow

```text
Commit
 -> automated tests
 -> static checks
 -> dependency and secret scans
 -> build application artifact
 -> run database migration verification
 -> deploy to target environment
 -> run readiness and smoke checks
 -> record deployment evidence
```

## 7. Backup and Restore

Minimum pilot targets:

- Recovery point objective: backup no older than 24 hours.
- Recovery time objective: restore within 4 hours for reference pilot dataset.

Backup manifest includes:

- environment;
- timestamp;
- application version;
- schema version;
- critical table counts;
- checksum;
- storage object count where applicable.

Restore verification checks:

- application readiness;
- booking count;
- financial event count;
- match count;
- discrepancy count;
- audit event count;
- sample booking timeline reconstruction.

## 8. Operational Health

Liveness:

- process is running.

Readiness:

- database reachable;
- migrations current;
- object storage reachable;
- identity-provider configuration loaded;
- background job queue available.

Metrics:

- import counts and failures;
- reconciliation duration;
- match counts;
- discrepancy counts;
- export failures;
- authentication failures;
- job retries;
- request duration by endpoint;
- error counts by stable code.

## 9. Scaling Position

The first response to pilot growth is to scale the whole application and database vertically or with simple stateless application replicas if needed.

A module split may be reconsidered only when:

- one workload has materially different scale characteristics;
- independent release cadence is genuinely required;
- separate teams own separate domains;
- data ownership can tolerate distributed consistency;
- cross-service tracing and operational controls are mature.
