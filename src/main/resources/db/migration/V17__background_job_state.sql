create table background_job (
    id uuid primary key,
    organisation_id uuid not null references organisation (id),
    job_type text not null,
    status text not null,
    target_type text,
    target_id uuid,
    idempotency_key text not null,
    requested_by_user_id uuid not null references app_user (id),
    max_attempts integer not null,
    attempt_count integer not null default 0,
    diagnostic_category text,
    diagnostic_message text,
    correlation_id text not null,
    requested_at timestamptz not null,
    last_attempt_at timestamptz,
    next_attempt_at timestamptz,
    completed_at timestamptz,
    constraint background_job_type_not_blank check (length(trim(job_type)) > 0),
    constraint background_job_status_supported check (status in ('PENDING', 'RUNNING', 'SUCCEEDED', 'FAILED')),
    constraint background_job_target_type_not_blank check (target_type is null or length(trim(target_type)) > 0),
    constraint background_job_idempotency_key_not_blank check (length(trim(idempotency_key)) > 0),
    constraint background_job_attempt_bounds check (max_attempts between 1 and 3 and attempt_count between 0 and max_attempts),
    constraint background_job_correlation_id_not_blank check (length(trim(correlation_id)) > 0),
    constraint background_job_failure_diagnostic check (
        status <> 'FAILED' or (diagnostic_category is not null and length(trim(diagnostic_category)) > 0)
    ),
    constraint background_job_terminal_completed check (
        (status in ('SUCCEEDED', 'FAILED') and completed_at is not null)
        or (status in ('PENDING', 'RUNNING') and completed_at is null)
    )
);

create unique index background_job_idempotency_idx
    on background_job (organisation_id, job_type, idempotency_key);

create index background_job_status_idx
    on background_job (organisation_id, status, requested_at desc);

create index background_job_target_idx
    on background_job (organisation_id, target_type, target_id, requested_at desc);
