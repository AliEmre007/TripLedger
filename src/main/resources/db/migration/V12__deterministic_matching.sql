create table booking_match (
    id uuid primary key,
    organisation_id uuid not null references organisation (id),
    booking_id uuid not null references booking (id),
    match_type text not null,
    rule_code text not null,
    status text not null,
    created_by_user_id uuid,
    created_at timestamptz not null,
    removed_at timestamptz,
    reason text,
    constraint booking_match_type_supported check (match_type in ('AUTOMATIC')),
    constraint booking_match_status_supported check (status in ('ACTIVE', 'REVIEW_REQUIRED')),
    constraint booking_match_rule_code_not_blank check (length(trim(rule_code)) > 0)
);

create table match_allocation (
    id uuid primary key,
    organisation_id uuid not null references organisation (id),
    match_id uuid not null references booking_match (id),
    financial_event_id uuid not null references financial_event (id),
    amount numeric(19, 4) not null,
    currency char(3) not null,
    active boolean not null default true,
    exchange_rate_id uuid references exchange_rate (id),
    original_amount numeric(19, 4),
    original_currency char(3),
    constraint match_allocation_amount_positive check (amount > 0),
    constraint match_allocation_amount_minor_unit check (amount = round(amount, 2)),
    constraint match_allocation_currency_supported check (currency in ('EUR', 'GBP', 'TRY', 'USD')),
    constraint match_allocation_original_currency_supported check (
        original_currency is null or original_currency in ('EUR', 'GBP', 'TRY', 'USD')
    )
);

create index booking_match_booking_idx
    on booking_match (organisation_id, booking_id, created_at desc);

create index match_allocation_event_idx
    on match_allocation (organisation_id, financial_event_id)
    where active = true;
