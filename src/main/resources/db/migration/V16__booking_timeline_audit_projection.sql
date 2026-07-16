create table audit_event (
    id uuid primary key,
    organisation_id uuid references organisation (id),
    actor_user_id uuid references app_user (id),
    system_actor text,
    action text not null,
    target_type text not null,
    target_id uuid,
    outcome text not null,
    before_reference text,
    after_reference text,
    reason text,
    correlation_id text not null,
    created_at timestamptz not null,
    constraint audit_event_action_not_blank check (length(trim(action)) > 0),
    constraint audit_event_target_type_not_blank check (length(trim(target_type)) > 0),
    constraint audit_event_outcome_supported check (outcome in ('SUCCESS', 'DENIED', 'FAILED')),
    constraint audit_event_actor_present check (actor_user_id is not null or system_actor is not null),
    constraint audit_event_correlation_id_not_blank check (length(trim(correlation_id)) > 0)
);

create index audit_event_target_idx
    on audit_event (organisation_id, target_type, target_id, created_at desc);

create index audit_event_created_idx
    on audit_event (organisation_id, created_at desc);

create function reject_audit_event_mutation()
returns trigger
language plpgsql
as $$
begin
    raise exception 'audit_event records are append-only';
end;
$$;

create trigger audit_event_no_update
    before update on audit_event
    for each row
    execute function reject_audit_event_mutation();

create trigger audit_event_no_delete
    before delete on audit_event
    for each row
    execute function reject_audit_event_mutation();
