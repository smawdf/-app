-- Client-side cloud error logs.
-- Safe to run multiple times in Supabase SQL Editor.

create table if not exists public.client_error_logs (
    id text primary key,
    user_id uuid references auth.users(id) on delete cascade,
    pair_id text not null default '',
    session_id text not null default '',
    area text not null default '',
    action text not null default '',
    message text not null default '',
    detail text not null default '',
    app_version text not null default '',
    device text not null default '',
    os_version text not null default '',
    created_at timestamptz not null default now()
);

alter table public.client_error_logs enable row level security;

drop policy if exists "Users can insert own client error logs" on public.client_error_logs;
create policy "Users can insert own client error logs"
    on public.client_error_logs for insert
    with check (user_id = auth.uid());

drop policy if exists "Users can read own client error logs" on public.client_error_logs;
create policy "Users can read own client error logs"
    on public.client_error_logs for select
    using (user_id = auth.uid());

create index if not exists idx_client_error_logs_user_created
    on public.client_error_logs(user_id, created_at desc);

create index if not exists idx_client_error_logs_area_created
    on public.client_error_logs(area, created_at desc);
