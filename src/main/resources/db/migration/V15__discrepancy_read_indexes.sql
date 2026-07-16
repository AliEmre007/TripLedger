create index discrepancy_queue_read_idx
    on discrepancy (organisation_id, status, type, severity, created_at desc);

create index discrepancy_owner_read_idx
    on discrepancy (organisation_id, owner_user_id, status, created_at desc)
    where owner_user_id is not null;
