create table import_batch (
    id uuid primary key,
    organisation_id uuid not null references organisation (id),
    source_system_id uuid not null references source_system (id),
    template_type text not null,
    template_version text not null,
    status text not null,
    file_name text not null,
    file_checksum text not null,
    received_by_user_id uuid not null references app_user (id),
    received_at timestamptz not null,
    completed_at timestamptz,
    failure_code text,
    failure_reason text,
    total_count integer not null default 0,
    accepted_count integer not null default 0,
    duplicate_count integer not null default 0,
    rejected_count integer not null default 0,
    failed_count integer not null default 0,
    constraint import_batch_status_supported check (status in ('RECEIVED', 'COMPLETED', 'FAILED')),
    constraint import_batch_template_type_not_blank check (length(trim(template_type)) > 0),
    constraint import_batch_template_version_not_blank check (length(trim(template_version)) > 0),
    constraint import_batch_file_name_not_blank check (length(trim(file_name)) > 0),
    constraint import_batch_file_checksum_not_blank check (length(trim(file_checksum)) > 0),
    constraint import_batch_counts_non_negative check (
        total_count >= 0
        and accepted_count >= 0
        and duplicate_count >= 0
        and rejected_count >= 0
        and failed_count >= 0
    ),
    constraint import_batch_counts_sum_to_total check (
        total_count = accepted_count + duplicate_count + rejected_count + failed_count
    ),
    constraint import_batch_terminal_completion_time check (
        status = 'RECEIVED'
        or completed_at is not null
    ),
    constraint import_batch_failure_reason_when_failed check (
        status <> 'FAILED'
        or (
            failure_code is not null
            and failure_reason is not null
            and length(trim(failure_code)) > 0
            and length(trim(failure_reason)) > 0
        )
    )
);

create index import_batch_organisation_received_at_idx
    on import_batch (organisation_id, received_at desc);

create index import_batch_organisation_source_system_idx
    on import_batch (organisation_id, source_system_id);

create table import_row_result (
    id uuid primary key,
    organisation_id uuid not null references organisation (id),
    import_batch_id uuid not null references import_batch (id),
    row_number integer not null,
    outcome text not null,
    field_name text,
    error_code text,
    reason text,
    source_record_id uuid,
    recorded_at timestamptz not null,
    constraint import_row_result_outcome_supported check (
        outcome in ('ACCEPTED', 'DUPLICATE', 'REJECTED', 'FAILED')
    ),
    constraint import_row_result_row_number_positive check (row_number > 0),
    constraint import_row_result_error_for_rejected_or_failed check (
        outcome not in ('REJECTED', 'FAILED')
        or (
            error_code is not null
            and reason is not null
            and length(trim(error_code)) > 0
            and length(trim(reason)) > 0
        )
    )
);

create unique index import_row_result_batch_row_idx
    on import_row_result (organisation_id, import_batch_id, row_number);

create index import_row_result_batch_idx
    on import_row_result (organisation_id, import_batch_id);
