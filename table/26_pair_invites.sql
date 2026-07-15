-- Durable pair invitations independent from profiles.pair_id.
-- Run after table/25_disable_email_confirmation_cleanup.sql.

create table if not exists public.pair_invites (
    code text primary key check (length(code) = 6),
    inviter_id uuid not null references auth.users(id) on delete cascade,
    inviter_role text not null check (inviter_role in ('caretaker', 'eater')),
    created_at timestamptz not null default now(),
    expires_at timestamptz not null default (now() + interval '7 days'),
    used_at timestamptz,
    used_by uuid references auth.users(id) on delete set null
);

alter table public.pair_invites enable row level security;
drop policy if exists "Users can view own pair invites" on public.pair_invites;
create policy "Users can view own pair invites"
    on public.pair_invites for select
    using (inviter_id = auth.uid() or used_by = auth.uid());

create index if not exists idx_pair_invites_inviter_active
    on public.pair_invites(inviter_id, expires_at desc);

create or replace function public.create_pair_invite(inviter_role text)
returns table(pair_code text, selected_role text)
language plpgsql
security definer
set search_path = public
as $$
declare
    caller_id uuid := auth.uid();
    generated_code text;
    current_pair text;
    attempt integer;
begin
    if caller_id is null then raise exception 'authentication required'; end if;
    if inviter_role not in ('caretaker', 'eater') then raise exception 'invalid inviter role'; end if;

    select pair_id into current_pair from public.profiles where user_id = caller_id for update;
    if not found then raise exception 'profile not found'; end if;
    if current_pair <> '' and current_pair <> '00000000-0000-0000-0000-000000000000'
       and exists (select 1 from public.profiles where pair_id = current_pair and user_id <> caller_id) then
        raise exception 'already paired';
    end if;

    delete from public.pair_invites where inviter_id = caller_id and used_at is null;
    for attempt in 1..20 loop
        generated_code := upper(substr(md5(random()::text || clock_timestamp()::text || caller_id::text), 1, 6));
        exit when not exists (select 1 from public.pair_invites where code = generated_code);
    end loop;
    if exists (select 1 from public.pair_invites where code = generated_code) then
        raise exception 'unable to allocate pair code';
    end if;

    insert into public.pair_invites(code, inviter_id, inviter_role)
    values (generated_code, caller_id, inviter_role);
    update public.profiles
       set pair_id = '00000000-0000-0000-0000-000000000000',
           selected_role = inviter_role,
           updated_at = now()
     where user_id = caller_id;
    return query select generated_code, inviter_role;
end;
$$;

create or replace function public.preview_pair_invite(invite_code text)
returns table(pair_code text, inviter_name text, inviter_role text)
language sql
stable
security definer
set search_path = public
as $$
    select i.code, p.nickname, i.inviter_role
      from public.pair_invites i
      join public.profiles p on p.user_id = i.inviter_id
     where i.code = upper(trim(invite_code))
       and i.inviter_id <> auth.uid()
       and i.used_at is null
       and i.expires_at > now()
     limit 1;
$$;

create or replace function public.join_pair_invite(invite_code text)
returns table(pair_code text, selected_role text)
language plpgsql
security definer
set search_path = public
as $$
declare
    caller_id uuid := auth.uid();
    normalized_code text := upper(trim(invite_code));
    invite public.pair_invites%rowtype;
    invitee_role text;
begin
    if caller_id is null then raise exception 'authentication required'; end if;
    select * into invite
      from public.pair_invites
     where code = normalized_code
     for update;
    if not found or invite.used_at is not null or invite.expires_at <= now() or invite.inviter_id = caller_id then
        raise exception 'pair invite unavailable';
    end if;

    invitee_role := case when invite.inviter_role = 'caretaker' then 'eater' else 'caretaker' end;
    update public.profiles
       set pair_id = normalized_code, selected_role = invite.inviter_role, updated_at = now()
     where user_id = invite.inviter_id;
    update public.profiles
       set pair_id = normalized_code, selected_role = invitee_role, updated_at = now()
     where user_id = caller_id;
    if not found then raise exception 'profile not found'; end if;

    update public.pair_invites
       set used_at = now(), used_by = caller_id
     where code = normalized_code;
    return query select normalized_code, invitee_role;
end;
$$;

revoke all on function public.create_pair_invite(text) from public;
revoke all on function public.preview_pair_invite(text) from public;
revoke all on function public.join_pair_invite(text) from public;
grant execute on function public.create_pair_invite(text) to authenticated;
grant execute on function public.preview_pair_invite(text) to authenticated;
grant execute on function public.join_pair_invite(text) to authenticated;
