alter table source_record
    drop constraint source_record_type_supported;

alter table source_record
    add constraint source_record_type_supported check (
        record_type in ('BOOKING', 'SUPPLIER_OBLIGATION', 'FINANCIAL_EVENT')
    );

create table financial_event (
    id uuid primary key,
    organisation_id uuid not null references organisation (id),
    source_record_id uuid references source_record (id),
    booking_id uuid references booking (id),
    event_type text not null,
    direction text not null,
    amount numeric(19, 4) not null,
    currency char(3) not null,
    effective_at timestamptz not null,
    external_reference text,
    reverses_event_id uuid references financial_event (id),
    adjustment_reason text,
    created_by_user_id uuid references app_user (id),
    created_at timestamptz not null,
    constraint financial_event_type_supported check (
        event_type in (
            'CUSTOMER_PAYMENT',
            'CHANNEL_SETTLEMENT',
            'CHANNEL_COMMISSION',
            'PAYMENT_FEE',
            'REFUND',
            'PAYMENT_REVERSAL',
            'SUPPLIER_PAYMENT',
            'SUPPLIER_CREDIT',
            'MANUAL_ADJUSTMENT'
        )
    ),
    constraint financial_event_direction_supported check (
        direction in (
            'INCREASE_RECEIVED',
            'DECREASE_RECEIVED',
            'INCREASE_DEDUCTION',
            'INCREASE_SUPPLIER_SETTLEMENT',
            'DECREASE_SUPPLIER_COST',
            'ADJUSTMENT'
        )
    ),
    constraint financial_event_amount_positive check (amount > 0),
    constraint financial_event_currency_uppercase check (currency = upper(currency)),
    constraint financial_event_manual_adjustment_reason check (
        event_type <> 'MANUAL_ADJUSTMENT'
        or adjustment_reason is not null
    )
);

create unique index financial_event_source_record_idx
    on financial_event (organisation_id, source_record_id)
    where source_record_id is not null;

create index financial_event_booking_idx
    on financial_event (organisation_id, booking_id, event_type, currency, effective_at);

create index financial_event_unmatched_idx
    on financial_event (organisation_id, created_at)
    where booking_id is null;

create function reject_financial_event_mutation()
returns trigger
language plpgsql
as $$
begin
    raise exception 'accepted financial events are immutable';
end;
$$;

create trigger financial_event_no_update
    before update on financial_event
    for each row
    execute function reject_financial_event_mutation();

create trigger financial_event_no_delete
    before delete on financial_event
    for each row
    execute function reject_financial_event_mutation();
