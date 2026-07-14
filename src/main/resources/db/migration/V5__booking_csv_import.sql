alter table import_batch
    drop constraint import_batch_status_supported;

alter table import_batch
    add constraint import_batch_status_supported check (
        status in ('RECEIVED', 'COMPLETED', 'COMPLETED_WITH_ERRORS', 'FAILED')
    );

create table source_record (
    id uuid primary key,
    organisation_id uuid not null references organisation (id),
    source_system_id uuid not null references source_system (id),
    import_batch_id uuid not null references import_batch (id),
    record_type text not null,
    external_record_id text not null,
    source_version text not null,
    source_row_number integer not null,
    content_checksum text not null,
    payload_reference text,
    accepted_at timestamptz not null,
    constraint source_record_type_supported check (record_type in ('BOOKING')),
    constraint source_record_external_record_id_not_blank check (length(trim(external_record_id)) > 0),
    constraint source_record_source_version_not_blank check (length(trim(source_version)) > 0),
    constraint source_record_row_number_positive check (source_row_number > 0),
    constraint source_record_content_checksum_not_blank check (length(trim(content_checksum)) > 0)
);

create unique index source_record_identity_idx
    on source_record (organisation_id, source_system_id, record_type, external_record_id, source_version);

create index source_record_import_batch_idx
    on source_record (organisation_id, import_batch_id);

create table booking (
    id uuid primary key,
    organisation_id uuid not null references organisation (id),
    source_system_id uuid not null references source_system (id),
    external_booking_id text not null,
    current_source_record_id uuid references source_record (id),
    booking_date date not null,
    service_start_date date,
    service_end_date date,
    lifecycle_status text not null,
    selling_currency char(3) not null,
    contracted_selling_amount numeric(19, 4) not null,
    customer_reference text,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    constraint booking_lifecycle_status_supported check (
        lifecycle_status in ('DRAFT', 'CONFIRMED', 'IN_SERVICE', 'COMPLETED', 'CANCELLED')
    ),
    constraint booking_external_booking_id_not_blank check (length(trim(external_booking_id)) > 0),
    constraint booking_service_date_order check (
        service_start_date is null
        or service_end_date is null
        or service_end_date >= service_start_date
    ),
    constraint booking_selling_currency_uppercase check (selling_currency = upper(selling_currency)),
    constraint booking_contracted_selling_amount_non_negative check (contracted_selling_amount >= 0)
);

create unique index booking_source_identity_idx
    on booking (organisation_id, source_system_id, external_booking_id);

create index booking_organisation_idx
    on booking (organisation_id);

create table booking_item (
    id uuid primary key,
    organisation_id uuid not null references organisation (id),
    booking_id uuid not null references booking (id),
    source_record_id uuid references source_record (id),
    item_external_id text not null,
    service_type text not null,
    service_start_date date not null,
    service_end_date date not null,
    selling_amount numeric(19, 4) not null,
    selling_currency char(3) not null,
    state text not null,
    constraint booking_item_service_type_supported check (
        service_type in ('HOTEL', 'TOUR', 'TRANSFER', 'OTHER')
    ),
    constraint booking_item_state_supported check (state in ('ACTIVE')),
    constraint booking_item_external_id_not_blank check (length(trim(item_external_id)) > 0),
    constraint booking_item_service_date_order check (service_end_date >= service_start_date),
    constraint booking_item_selling_currency_uppercase check (selling_currency = upper(selling_currency)),
    constraint booking_item_selling_amount_non_negative check (selling_amount >= 0)
);

create unique index booking_item_source_identity_idx
    on booking_item (organisation_id, booking_id, item_external_id);

create index booking_item_booking_idx
    on booking_item (organisation_id, booking_id);
