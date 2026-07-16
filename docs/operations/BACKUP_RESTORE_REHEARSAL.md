# Backup and Restore Rehearsal

## Purpose

This document records the validation-release backup and restore rehearsal procedure for AC-072.

The local rehearsal uses synthetic/local data only. Pilot rehearsal must use encrypted backup storage and restricted restore access before real data is approved.

## Local Procedure

Start from a running local stack:

```bash
cp .env.example .env
docker compose up --build
```

Confirm readiness:

```bash
make smoke
```

Create a backup and manifest:

```bash
make backup-local
```

The command writes to `/tmp/tripledger-backups/<backup-id>` by default:

- `tripledger.sql`
- `manifest.json`

Restore and validate counts:

```bash
BACKUP_DIR=/tmp/tripledger-backups/<backup-id> RESTORE_CONFIRM=restore-local make restore-local
make smoke
```

`RESTORE_CONFIRM=restore-local` is required because local restore drops and recreates the database `public` schema before loading the dump.

## Manifest

The manifest records:

- backup id;
- environment;
- creation timestamp;
- commit sha;
- schema version;
- dump filename;
- dump SHA-256 checksum;
- critical table counts:
  - `booking`;
  - `financial_event`;
  - `booking_match`;
  - `discrepancy`;
  - `audit_event`.

## Restore Evidence

Successful restore writes `restore-evidence.json` next to the manifest with:

- restore timestamp;
- backup id;
- restore status;
- schema version;
- restored critical table counts.

The restored counts must match the backup manifest before the rehearsal is accepted.

## Acceptance Mapping

AC-072 requires:

- backup no older than 24 hours;
- documented restore procedure;
- service returns to ready state;
- booking, financial, match, discrepancy, and audit counts match the manifest.

Local evidence satisfies the procedure and count-matching path. Production-like pilot evidence still requires encrypted backup storage, access logging, and environment-specific restore timing.
