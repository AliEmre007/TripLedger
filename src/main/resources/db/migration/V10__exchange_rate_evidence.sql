create table exchange_rate (
    id uuid primary key,
    organisation_id uuid not null references organisation (id),
    financial_event_id uuid references financial_event (id),
    source_amount numeric(19, 4) not null,
    source_currency char(3) not null,
    target_amount numeric(19, 4) not null,
    target_currency char(3) not null,
    rate numeric(28, 12) not null,
    effective_at timestamptz not null,
    rate_source text not null,
    rounding_policy_version text not null,
    created_by_user_id uuid references app_user (id),
    created_at timestamptz not null,
    constraint exchange_rate_source_currency_supported check (
        source_currency in ('EUR', 'GBP', 'TRY', 'USD')
    ),
    constraint exchange_rate_target_currency_supported check (
        target_currency in ('EUR', 'GBP', 'TRY', 'USD')
    ),
    constraint exchange_rate_distinct_currencies check (source_currency <> target_currency),
    constraint exchange_rate_source_amount_positive check (source_amount > 0),
    constraint exchange_rate_target_amount_positive check (target_amount > 0),
    constraint exchange_rate_source_amount_minor_unit check (
        source_amount = round(source_amount, 2)
    ),
    constraint exchange_rate_target_amount_minor_unit check (
        target_amount = round(target_amount, 2)
    ),
    constraint exchange_rate_positive check (rate > 0),
    constraint exchange_rate_rate_scale check (rate = round(rate, 12)),
    constraint exchange_rate_source_currency_uppercase check (source_currency = upper(source_currency)),
    constraint exchange_rate_target_currency_uppercase check (target_currency = upper(target_currency)),
    constraint exchange_rate_rate_source_present check (length(trim(rate_source)) > 0),
    constraint exchange_rate_rounding_policy_present check (length(trim(rounding_policy_version)) > 0)
);

create index exchange_rate_event_idx
    on exchange_rate (organisation_id, financial_event_id, effective_at desc)
    where financial_event_id is not null;

create index exchange_rate_list_idx
    on exchange_rate (organisation_id, effective_at desc, created_at desc);
