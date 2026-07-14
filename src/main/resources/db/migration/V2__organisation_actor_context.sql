create table organisation (
    id uuid primary key,
    name text not null,
    base_currency char(3) not null,
    materiality_threshold numeric(19, 4) not null,
    default_amount_tolerance numeric(19, 4) not null,
    default_date_window_before_days integer not null,
    default_date_window_after_days integer not null,
    rounding_policy_version text not null,
    status text not null,
    created_at timestamptz not null default now(),
    constraint organisation_base_currency_uppercase check (base_currency = upper(base_currency)),
    constraint organisation_status_supported check (status in ('ACTIVE', 'INACTIVE')),
    constraint organisation_materiality_non_negative check (materiality_threshold >= 0),
    constraint organisation_amount_tolerance_non_negative check (default_amount_tolerance >= 0),
    constraint organisation_date_window_non_negative check (
        default_date_window_before_days >= 0
        and default_date_window_after_days >= 0
    )
);

create table app_user (
    id uuid primary key,
    organisation_id uuid not null references organisation (id),
    identity_subject text not null,
    display_name text not null,
    role text not null,
    status text not null,
    created_at timestamptz not null default now(),
    deactivated_at timestamptz,
    constraint app_user_role_supported check (
        role in ('ADMINISTRATOR', 'FINANCE', 'OPERATIONS', 'READ_ONLY_MANAGER')
    ),
    constraint app_user_status_supported check (status in ('ACTIVE', 'INACTIVE')),
    constraint app_user_deactivated_when_inactive check (
        status = 'ACTIVE'
        or deactivated_at is not null
    )
);

create unique index app_user_organisation_subject_idx
    on app_user (organisation_id, identity_subject);
