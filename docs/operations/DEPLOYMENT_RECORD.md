# Deployment Record

Copy this template for each deployment or attach the completed fields to the release ticket.

## Deployment Summary

| Field | Value |
| --- | --- |
| Release version |  |
| Commit SHA |  |
| Environment |  |
| Data classification | Synthetic / anonymised / real |
| Operator |  |
| Started at |  |
| Completed at |  |
| Deployment result | Successful / rolled back / failed |
| Previous known-good version |  |

## Pre-Deployment Checks

| Check | Result | Notes |
| --- | --- | --- |
| Worktree or build source is clean |  |  |
| `make verify` passed |  |  |
| Migration plan reviewed |  |  |
| Secrets provided by approved source |  |  |
| No real data in validation/demo environment |  |  |
| Rollback version identified |  |  |
| Backup available before deployment |  |  |

## Deployment Steps

| Step | Result | Evidence |
| --- | --- | --- |
| Application image built or artifact selected |  |  |
| Database reachable |  |  |
| Flyway migrations applied or validated |  |  |
| Application started |  |  |
| `make smoke` passed |  |  |
| Logs checked for startup errors |  |  |
| Metrics endpoint checked |  |  |

## Post-Deployment Checks

| Check | Result | Notes |
| --- | --- | --- |
| `/api/v1/health/live` healthy |  |  |
| `/api/v1/health/ready` healthy |  |  |
| `/actuator/health/liveness` healthy |  |  |
| `/actuator/health/readiness` healthy |  |  |
| Critical workflow checked |  |  |
| Backup created after deployment |  |  |
| Restore rehearsal completed in safe environment |  |  |

## Rollback Decision

| Field | Value |
| --- | --- |
| Rollback required? | Yes / No |
| Rollback reason |  |
| Rollback version |  |
| Data restore required? | Yes / No |
| Restore backup id |  |
| Final service state |  |

## Known Issues And Follow-Up

| Issue | Severity | Owner | Due date |
| --- | --- | --- | --- |
|  |  |  |  |
