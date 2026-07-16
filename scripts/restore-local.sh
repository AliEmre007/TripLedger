#!/usr/bin/env sh
set -eu

POSTGRES_DB="${POSTGRES_DB:-tripledger}"
POSTGRES_USER="${POSTGRES_USER:-tripledger}"

if [ "${RESTORE_CONFIRM:-}" != "restore-local" ]; then
    printf 'Refusing to restore. Set RESTORE_CONFIRM=restore-local to continue.\n' >&2
    exit 2
fi

if [ -z "${BACKUP_DIR:-}" ]; then
    printf 'BACKUP_DIR is required.\n' >&2
    exit 2
fi

DUMP_FILE="$BACKUP_DIR/tripledger.sql"
MANIFEST_FILE="$BACKUP_DIR/manifest.json"
EVIDENCE_FILE="$BACKUP_DIR/restore-evidence.json"

if [ ! -f "$DUMP_FILE" ]; then
    printf 'Backup dump not found: %s\n' "$DUMP_FILE" >&2
    exit 2
fi

if [ ! -f "$MANIFEST_FILE" ]; then
    printf 'Backup manifest not found: %s\n' "$MANIFEST_FILE" >&2
    exit 2
fi

manifest_value() {
    sed -n "s/.*\"$1\": \"\\([^\"]*\\)\".*/\\1/p" "$MANIFEST_FILE" | head -n 1
}

manifest_count() {
    sed -n "s/.*\"$1\": \\([0-9][0-9]*\\).*/\\1/p" "$MANIFEST_FILE" | head -n 1
}

query() {
    docker compose exec -T db psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Atc "$1"
}

latest_migration_version() {
    ls src/main/resources/db/migration/V*.sql \
        | sed 's|.*/V||; s|__.*||' \
        | sort -V \
        | tail -n 1
}

expected_sha256="$(manifest_value dumpSha256)"
actual_sha256="$(sha256sum "$DUMP_FILE" | awk '{print $1}')"

if [ "$actual_sha256" != "$expected_sha256" ]; then
    printf 'Backup checksum mismatch.\nExpected: %s\nActual:   %s\n' "$expected_sha256" "$actual_sha256" >&2
    exit 1
fi

docker compose exec -T db psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -v ON_ERROR_STOP=1 \
    -c "drop schema public cascade; create schema public;"
docker compose exec -T db psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -v ON_ERROR_STOP=1 < "$DUMP_FILE"

compare_count() {
    table="$1"
    expected="$(manifest_count "$table")"
    actual="$(query "select count(*) from $table;")"
    if [ "$actual" != "$expected" ]; then
        printf 'Count mismatch for %s. Expected %s, got %s.\n' "$table" "$expected" "$actual" >&2
        exit 1
    fi
    printf '%s' "$actual"
}

booking_count="$(compare_count booking)"
financial_event_count="$(compare_count financial_event)"
match_count="$(compare_count booking_match)"
discrepancy_count="$(compare_count discrepancy)"
audit_event_count="$(compare_count audit_event)"
schema_version="$(query "select version from flyway_schema_history where success = true order by installed_rank desc limit 1;" 2>/dev/null || latest_migration_version)"
restored_at="$(date -u +%Y-%m-%dT%H:%M:%SZ)"

cat > "$EVIDENCE_FILE" <<EOF
{
  "restoredAt": "$restored_at",
  "backupId": "$(manifest_value backupId)",
  "status": "RESTORED",
  "schemaVersion": "$schema_version",
  "criticalTableCounts": {
    "booking": $booking_count,
    "financial_event": $financial_event_count,
    "booking_match": $match_count,
    "discrepancy": $discrepancy_count,
    "audit_event": $audit_event_count
  }
}
EOF

printf 'Restore rehearsal passed for backup: %s\n' "$BACKUP_DIR"
printf 'Evidence: %s\n' "$EVIDENCE_FILE"
