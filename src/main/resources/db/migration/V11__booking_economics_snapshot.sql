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
            'APPROVED_DISCOUNT',
            'REVERSAL',
            'MANUAL_ADJUSTMENT'
        )
    );

create table calculation_snapshot (
    id uuid primary key,
    organisation_id uuid not null references organisation (id),
    booking_id uuid not null references booking (id),
    rule_version text not null,
    contracted_gross_sale numeric(19, 4) not null,
    expected_customer_receivable numeric(19, 4),
    expected_deductions numeric(19, 4),
    active_supplier_cost numeric(19, 4),
    estimated_gross_margin numeric(19, 4),
    currency char(3) not null,
    status text not null,
    unknown_components text not null,
    created_at timestamptz not null,
    constraint calculation_snapshot_rule_version_not_blank check (length(trim(rule_version)) > 0),
    constraint calculation_snapshot_currency_supported check (currency in ('EUR', 'GBP', 'TRY', 'USD')),
    constraint calculation_snapshot_status_supported check (status in ('READY', 'NOT_READY')),
    constraint calculation_snapshot_contracted_amount_minor_unit check (
        contracted_gross_sale = round(contracted_gross_sale, 2)
    ),
    constraint calculation_snapshot_receivable_minor_unit check (
        expected_customer_receivable is null
        or expected_customer_receivable = round(expected_customer_receivable, 2)
    ),
    constraint calculation_snapshot_deductions_minor_unit check (
        expected_deductions is null
        or expected_deductions = round(expected_deductions, 2)
    ),
    constraint calculation_snapshot_supplier_cost_minor_unit check (
        active_supplier_cost is null
        or active_supplier_cost = round(active_supplier_cost, 2)
    ),
    constraint calculation_snapshot_margin_minor_unit check (
        estimated_gross_margin is null
        or estimated_gross_margin = round(estimated_gross_margin, 2)
    )
);

create index calculation_snapshot_booking_idx
    on calculation_snapshot (organisation_id, booking_id, created_at desc);
