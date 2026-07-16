# Observability Operations

This document defines the validation-release logs, metrics, dashboard expectations, and alert policies for TripLedger operations.

## Signal Model

| Signal | Question answered | Validation-release source |
| --- | --- | --- |
| Logs | What happened? | Application structured logs with correlation id and stable error codes. |
| Metrics | How often or how much? | Spring Actuator metrics and TripLedger request/job counters. |
| Traces | Where did time or failure occur? | Deferred until external integrations or multi-service deployment exist. |

## Required Log Fields

Request logs must remain safe for operational use and must include:

- timestamp;
- correlation id;
- HTTP method;
- route pattern;
- status;
- outcome;
- duration;
- stable error code when present.

Logs must not include:

- passwords, tokens, or certificates;
- raw CSV/source rows;
- payment account credentials;
- unrestricted customer contact data;
- backup keys or secret values.

## Dashboard Expectations

A validation dashboard should show:

- application liveness and readiness;
- request rate;
- error rate by stable error code;
- p95 request duration;
- database readiness state;
- failed background jobs;
- job retry count;
- backup age and latest restore result when available.

## Alert Policies

| Alert | Condition | Initial response |
| --- | --- | --- |
| Service not live | Liveness check fails for 2 consecutive checks | Check app container/process logs, restart only after preserving logs. |
| Service not ready | Readiness fails for 2 consecutive checks | Inspect readiness detail, database status, and migration logs. |
| Elevated API errors | 5xx errors exceed validation baseline for 5 minutes | Capture correlation ids, check recent deployment and logs. |
| Authentication or authorization spike | `AUTHENTICATION_REQUIRED`, `MFA_REQUIRED`, or authorisation denials spike unexpectedly | Confirm actor headers/OIDC config and investigate suspicious access. |
| Failed background jobs | A job reaches final failed state | Inspect job id, diagnostic category, correlation id, and retry count. |
| Backup stale | Latest backup age exceeds declared RPO | Create backup or investigate scheduled backup failure. |
| Restore rehearsal overdue | No restore evidence exists for current release or backup policy window | Run restore rehearsal in safe environment before relying on backup. |

## Validation-Release Limits

- Alert routing is documented but not wired to an external paging system in this repository.
- Dashboards are described as operational requirements; the concrete dashboard depends on the selected hosting/monitoring provider.
- Distributed tracing is deferred because the validation release is one deployable application without production external connectors.

## Before Real Data

Before any real customer or financial data is accepted:

- alerts must route to a named owner;
- dashboards must exist in the selected monitoring system;
- backup age and restore evidence must be visible to operators;
- logs must be reviewed for restricted-data leakage;
- dependency, secret, and image scans must pass release policy.
