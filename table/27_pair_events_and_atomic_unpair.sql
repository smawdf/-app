-- Pair lifecycle notifications and atomic unpairing.
-- Run after table/26_pair_invites.sql.

create table if not exists public.pair_events (
    id uuid primary key default gen_random_uuid(),
    recipient_id uuid not null references auth.users(id) on delete cascade,
    actor_id uuid references auth.users(id) on delete set null,
    event_type text not null check (event_type in ('paired', 'unpaired')),
    message text not null default '',
    created_at timestamptz not null default now(),
    read_at timestamptz
);

alter table public.pair_events enable row level security;
drop policy if exists "Users can read own pair events" on public.pair_events;
create policy "Users can read own pair events"
    on public.pair_events for select using (recipient_id = auth.uid());
drop policy if exists "Users can mark own pair events read" on public.pair_events;
create policy "Users can mark own pair events read"
    on public.pair_events for update
    using (recipient_id = auth.uid())
    with check (recipient_id = auth.uid());

create index if not exists idx_pair_events_recipient_unread
    on public.pair_events(recipient_id, created_at desc) where read_at is null;

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
    caller_name text;
    inviter_name text;
begin
    if caller_id is null then raise exception 'authentication required'; end if;
    select * into invite from public.pair_invites where code = normalized_code for update;
    if not found or invite.used_at is not null or invite.expires_at <= now() or invite.inviter_id = caller_id then
        raise exception 'pair invite unavailable';
    end if;

    invitee_role := case when invite.inviter_role = 'caretaker' then 'eater' else 'caretaker' end;
    select nickname into inviter_name from public.profiles where user_id = invite.inviter_id;
    select nickname into caller_name from public.profiles where user_id = caller_id;
    update public.profiles set pair_id = normalized_code, selected_role = invite.inviter_role, updated_at = now()
     where user_id = invite.inviter_id;
    update public.profiles set pair_id = normalized_code, selected_role = invitee_role, updated_at = now()
     where user_id = caller_id;
    if not found then raise exception 'profile not found'; end if;
    update public.pair_invites set used_at = now(), used_by = caller_id where code = normalized_code;

    insert into public.pair_events(recipient_id, actor_id, event_type, message) values
        (invite.inviter_id, caller_id, 'paired', coalesce(nullif(caller_name, ''), '对方') || ' 已接受邀请，绑定成功'),
        (caller_id, invite.inviter_id, 'paired', '已和 ' || coalesce(nullif(inviter_name, ''), '对方') || ' 绑定成功');
    return query select normalized_code, invitee_role;
end;
$$;

create or replace function public.unpair_current_pair()
returns boolean
language plpgsql
security definer
set search_path = public
as $$
declare
    caller_id uuid := auth.uid();
    caller_pair text;
    caller_name text;
    partner_id uuid;
begin
    if caller_id is null then raise exception 'authentication required'; end if;
    select pair_id, nickname into caller_pair, caller_name
      from public.profiles where user_id = caller_id for update;
    if caller_pair is null or caller_pair = '' or caller_pair = '00000000-0000-0000-0000-000000000000' then
        return false;
    end if;
    select user_id into partner_id from public.profiles
     where pair_id = caller_pair and user_id <> caller_id for update;

    update public.profiles
       set pair_id = '00000000-0000-0000-0000-000000000000', selected_role = '', updated_at = now()
     where pair_id = caller_pair;
    if partner_id is not null then
        insert into public.pair_events(recipient_id, actor_id, event_type, message)
        values (partner_id, caller_id, 'unpaired', coalesce(nullif(caller_name, ''), '对方') || ' 已解除与你的绑定');
    end if;
    return true;
end;
$$;

revoke all on function public.unpair_current_pair() from public;
grant execute on function public.unpair_current_pair() to authenticated;

