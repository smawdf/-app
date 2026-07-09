-- Cloud user preferences for device switching.
-- Safe to run multiple times in Supabase SQL Editor.

create table if not exists public.user_preferences (
    user_id uuid primary key references auth.users(id) on delete cascade,
    order_notifications_enabled boolean not null default false,
    updated_at timestamptz not null default now()
);

alter table public.user_preferences enable row level security;

drop policy if exists "Users can manage own preferences" on public.user_preferences;
create policy "Users can manage own preferences"
    on public.user_preferences for all
    using (user_id = auth.uid())
    with check (user_id = auth.uid());
