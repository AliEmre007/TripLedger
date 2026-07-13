create table app_metadata (
    key text primary key,
    value text not null,
    created_at timestamptz not null default now()
);

insert into app_metadata (key, value)
values ('schema_version', 'stage-6-foundation');
