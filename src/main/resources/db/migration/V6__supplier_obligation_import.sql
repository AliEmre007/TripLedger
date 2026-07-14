alter table source_record
    drop constraint source_record_type_supported;

alter table source_record
    add constraint source_record_type_supported check (
        record_type in ('BOOKING', 'SUPPLIER_OBLIGATION')
    );

create table supplier (
    id uuid primary key,
    organisation_id uuid not null references organisation (id),
    name text not null,
    external_reference text,
    status text not null,
    created_at timestamptz not null,
    constraint supplier_name_not_blank check (length(trim(name)) > 0),
    constraint supplier_external_reference_not_blank check (
        external_reference is null or length(trim(external_reference)) > 0
    ),
    constraint supplier_status_supported check (status in ('ACTIVE'))
);

create unique index supplier_external_reference_idx
    on supplier (organisation_id, external_reference)
    where external_reference is not null;

create table supplier_obligation (
    id uuid primary key,
    organisation_id uuid not null references organisation (id),
    booking_id uuid references booking (id),
    booking_item_id uuid references booking_item (id),
    supplier_id uuid references supplier (id),
    source_record_id uuid references source_record (id),
    amount numeric(19, 4) not null,
    currency char(3) not null,
    due_date date,
    status text not null,
    created_at timestamptz not null,
    constraint supplier_obligation_amount_positive check (amount > 0),
    constraint supplier_obligation_currency_uppercase check (currency = upper(currency)),
    constraint supplier_obligation_status_supported check (
        status in ('EXPECTED', 'CONFIRMED', 'INVOICED', 'CANCELLED', 'PAID')
    )
);

create unique index supplier_obligation_source_record_idx
    on supplier_obligation (organisation_id, source_record_id)
    where source_record_id is not null;

create index supplier_obligation_booking_idx
    on supplier_obligation (organisation_id, booking_id);

create index supplier_obligation_unlinked_idx
    on supplier_obligation (organisation_id, created_at)
    where booking_id is null and booking_item_id is null;
