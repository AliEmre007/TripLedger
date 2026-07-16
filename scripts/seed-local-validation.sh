#!/usr/bin/env sh
set -eu

POSTGRES_DB="${POSTGRES_DB:-tripledger}"
POSTGRES_USER="${POSTGRES_USER:-tripledger}"

docker compose exec -T db psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB" <<'SQL'
insert into organisation (
    id,
    name,
    base_currency,
    materiality_threshold,
    default_amount_tolerance,
    default_date_window_before_days,
    default_date_window_after_days,
    rounding_policy_version,
    status
) values (
    '11111111-1111-1111-1111-111111111111',
    'TripLedger Validation Organisation',
    'EUR',
    10.0000,
    0.0100,
    2,
    2,
    'validation-v1',
    'ACTIVE'
) on conflict (id) do update set
    name = excluded.name,
    status = excluded.status;

insert into app_user (
    id,
    organisation_id,
    identity_subject,
    display_name,
    role,
    status
) values
    (
        '22222222-2222-2222-2222-222222222222',
        '11111111-1111-1111-1111-111111111111',
        'local-admin',
        'Local Administrator',
        'ADMINISTRATOR',
        'ACTIVE'
    ),
    (
        '33333333-3333-3333-3333-333333333333',
        '11111111-1111-1111-1111-111111111111',
        'local-finance',
        'Local Finance',
        'FINANCE',
        'ACTIVE'
    ),
    (
        '44444444-4444-4444-4444-444444444444',
        '11111111-1111-1111-1111-111111111111',
        'local-ops',
        'Local Operations',
        'OPERATIONS',
        'ACTIVE'
    ),
    (
        '55555555-5555-5555-5555-555555555555',
        '11111111-1111-1111-1111-111111111111',
        'local-readonly',
        'Local Read Only',
        'READ_ONLY_MANAGER',
        'ACTIVE'
    )
on conflict (organisation_id, identity_subject) do update set
    display_name = excluded.display_name,
    role = excluded.role,
    status = excluded.status,
    deactivated_at = null;
SQL

printf 'Seeded local validation organisation and users.\n'
printf 'Default console actor: local-admin / 11111111-1111-1111-1111-111111111111\n'
