create table reconciliation_result (
    id uuid primary key,
    organisation_id uuid not null references organisation (id),
    booking_id uuid not null references booking (id),
    calculation_snapshot_id uuid not null references calculation_snapshot (id),
    rule_version text not null,
    status text not null,
    variance_amount numeric(19, 4),
    variance_currency char(3),
    created_at timestamptz not null,
    superseded_at timestamptz,
    constraint reconciliation_result_rule_version_not_blank check (length(trim(rule_version)) > 0),
    constraint reconciliation_result_status_supported check (
        status in ('NOT_READY', 'PARTIALLY_RECONCILED', 'RECONCILED', 'DISCREPANT')
    ),
    constraint reconciliation_result_variance_currency_supported check (
        variance_currency is null or variance_currency in ('EUR', 'GBP', 'TRY', 'USD')
    ),
    constraint reconciliation_result_variance_minor_unit check (
        variance_amount is null or variance_amount = round(variance_amount, 2)
    )
);

create index reconciliation_result_current_idx
    on reconciliation_result (organisation_id, booking_id, superseded_at, created_at desc);
