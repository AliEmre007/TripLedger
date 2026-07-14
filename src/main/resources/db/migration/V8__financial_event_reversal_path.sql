alter table financial_event
    drop constraint financial_event_type_supported;

alter table financial_event
    add constraint financial_event_type_supported check (
        event_type in (
            'CUSTOMER_PAYMENT',
            'CHANNEL_SETTLEMENT',
            'CHANNEL_COMMISSION',
            'PAYMENT_FEE',
            'REFUND',
            'PAYMENT_REVERSAL',
            'SUPPLIER_PAYMENT',
            'SUPPLIER_CREDIT',
            'REVERSAL',
            'MANUAL_ADJUSTMENT'
        )
    );

alter table financial_event
    drop constraint financial_event_direction_supported;

alter table financial_event
    add constraint financial_event_direction_supported check (
        direction in (
            'INCREASE_RECEIVED',
            'DECREASE_RECEIVED',
            'INCREASE_DEDUCTION',
            'INCREASE_SUPPLIER_SETTLEMENT',
            'DECREASE_SUPPLIER_COST',
            'REVERSAL',
            'ADJUSTMENT'
        )
    );

create unique index financial_event_one_reversal_idx
    on financial_event (organisation_id, reverses_event_id)
    where reverses_event_id is not null;
