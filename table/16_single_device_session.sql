alter table public.profiles
    add column if not exists session_id text not null default '';

alter table public.profiles
    add column if not exists session_updated_at text not null default '';

create index if not exists idx_profiles_session_id
    on public.profiles(session_id)
    where session_id <> '';
