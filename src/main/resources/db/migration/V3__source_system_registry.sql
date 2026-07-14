create table source_system (
    id uuid primary key,
    organisation_id uuid not null references organisation (id),
    name text not null,
    category text not null,
    external_code text not null,
    time_zone text not null,
    active boolean not null,
    created_at timestamptz not null default now(),
    constraint source_system_category_supported check (
        category in ('BOOKING_CHANNEL', 'PAYMENT_PROVIDER', 'SUPPLIER', 'MANUAL', 'OTHER')
    ),
    constraint source_system_external_code_uppercase check (external_code = upper(external_code)),
    constraint source_system_name_not_blank check (length(trim(name)) > 0),
    constraint source_system_external_code_not_blank check (length(trim(external_code)) > 0),
    constraint source_system_time_zone_not_blank check (length(trim(time_zone)) > 0)
);

create unique index source_system_organisation_external_code_idx
    on source_system (organisation_id, external_code);

create index source_system_organisation_idx
    on source_system (organisation_id);
