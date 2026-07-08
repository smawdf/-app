-- Pair invite role metadata.
-- Safe to run multiple times in Supabase SQL Editor.
-- Purpose: invite preview needs to know whether the inviter is caretaker or eater.

alter table public.profiles
    add column if not exists selected_role text not null default '';

alter table public.profiles
    drop constraint if exists profiles_selected_role_check;

alter table public.profiles
    add constraint profiles_selected_role_check
    check (selected_role in ('', 'caretaker', 'eater'));

drop policy if exists "Authenticated users can preview pair invites" on public.profiles;

create policy "Authenticated users can preview pair invites"
    on public.profiles for select
    using (
        auth.role() = 'authenticated'
        and pair_id <> '00000000-0000-0000-0000-000000000000'
        and length(pair_id) = 6
        and selected_role in ('caretaker', 'eater')
    );

create index if not exists idx_profiles_pair_invite_preview
    on public.profiles(pair_id, selected_role);
