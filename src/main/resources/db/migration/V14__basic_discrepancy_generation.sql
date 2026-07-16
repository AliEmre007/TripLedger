create table discrepancy (
    id uuid primary key,
    organisation_id uuid not null references organisation (id),
    booking_id uuid references booking (id),
    type text not null,
    severity text not null,
    component text,
    cause_identity text not null,
    amount numeric(19, 4),
    currency char(3),
    status text not null,
    owner_user_id uuid references app_user (id),
    explanation text not null,
    created_at timestamptz not null,
    resolved_at timestamptz,
    constraint discrepancy_type_supported check (
        type in ('SHORT_SETTLEMENT', 'AMBIGUOUS_MATCH')
    ),
    constraint discrepancy_severity_supported check (
        severity in ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')
    ),
    constraint discrepancy_status_supported check (status in ('ACTIVE', 'RESOLVED')),
    constraint discrepancy_cause_identity_not_blank check (length(trim(cause_identity)) > 0),
    constraint discrepancy_explanation_not_blank check (length(trim(explanation)) > 0),
    constraint discrepancy_amount_minor_unit check (
        amount is null or amount = round(amount, 2)
    ),
    constraint discrepancy_currency_supported check (
        currency is null or currency in ('EUR', 'GBP', 'TRY', 'USD')
    )
);

create unique index discrepancy_active_identity_idx
    on discrepancy (organisation_id, booking_id, type, component, cause_identity)
    where status = 'ACTIVE';

create index discrepancy_booking_idx
    on discrepancy (organisation_id, booking_id, status, created_at desc);
