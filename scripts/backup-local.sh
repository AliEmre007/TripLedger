#!/usr/bin/env sh
set -eu

POSTGRES_DB="${POSTGRES_DB:-tripledger}"
POSTGRES_USER="${POSTGRES_USER:-tripledger}"
BACKUP_ROOT="${BACKUP_ROOT:-/tmp/tripledger-backups}"
BACKUP_ID="${BACKUP_ID:-$(date -u +%Y%m%dT%H%M%SZ)}"
BACKUP_DIR="${BACKUP_DIR:-$BACKUP_ROOT/$BACKUP_ID}"
DUMP_FILE="$BACKUP_DIR/tripledger.sql"
MANIFEST_FILE="$BACKUP_DIR/manifest.json"

mkdir -p "$BACKUP_DIR"

query() {
    docker compose exec -T db psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Atc "$1"
}

count_table() {
    query "select count(*) from $1;"
}

latest_migration_version() {
    ls src/main/resources/db/migration/V*.sql \
        | sed 's|.*/V||; s|__.*||' \
        | sort -V \
        | tail -n 1
}

created_at="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
commit_sha="$(git rev-parse HEAD 2>/dev/null || printf 'unknown')"
schema_version="$(query "select version from flyway_schema_history where success = true order by installed_rank desc limit 1;" 2>/dev/null || latest_migration_version)"

docker compose exec -T db pg_dump -U "$POSTGRES_USER" "$POSTGRES_DB" > "$DUMP_FILE"
dump_sha256="$(sha256sum "$DUMP_FILE" | awk '{print $1}')"

booking_count="$(count_table booking)"
financial_event_count="$(count_table financial_event)"
match_count="$(count_table booking_match)"
discrepancy_count="$(count_table discrepancy)"
audit_event_count="$(count_table audit_event)"

cat > "$MANIFEST_FILE" <<EOF
{
  "backupId": "$BACKUP_ID",
  "environment": "local",
  "createdAt": "$created_at",
  "commitSha": "$commit_sha",
  "schemaVersion": "$schema_version",
  "dumpFile": "tripledger.sql",
  "dumpSha256": "$dump_sha256",
  "criticalTableCounts": {
    "booking": $booking_count,
    "financial_event": $financial_event_count,
    "booking_match": $match_count,
    "discrepancy": $discrepancy_count,
    "audit_event": $audit_event_count
  }
}
EOF

printf 'Backup created: %s\n' "$BACKUP_DIR"
printf 'Manifest: %s\n' "$MANIFEST_FILE"
