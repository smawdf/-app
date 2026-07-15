-- Secure pair invitation flow.
-- Run after table/21_atomic_candy_coins.sql.

create or replace function public.current_pair_id()
returns text
language sql
stable
security definer
set search_path = public
as $$
    select pair_id
      from public.profiles
     where user_id = auth.uid()
     limit 1;
$$;

revoke all on function public.current_pair_id() from public;
grant execute on function public.current_pair_id() to authenticated;

drop policy if exists "Authenticated users can preview pair invites" on public.profiles;
drop policy if exists "Pair members can view pair profiles" on public.profiles;

create policy "Pair members can view pair profiles"
    on public.profiles for select
    using (
        user_id = auth.uid()
        or (
            pair_id = public.current_pair_id()
            and pair_id <> ''
            and pair_id <> '00000000-0000-0000-0000-000000000000'
        )
    );

create or replace function public.create_pair_invite(inviter_role text)
returns table(pair_code text, selected_role text)
language plpgsql
security definer
set search_path = public
as $$
declare
    caller_id uuid := auth.uid();
    current_pair text;
    generated_code text;
    member_count integer;
    attempt integer;
begin
    if caller_id is null then raise exception 'authentication required'; end if;
    if inviter_role not in ('caretaker', 'eater') then raise exception 'invalid inviter role'; end if;

    select pair_id into current_pair
      from public.profiles
     where user_id = caller_id
     for update;
    if not found then raise exception 'profile not found'; end if;

    if current_pair <> '' and current_pair <> '00000000-0000-0000-0000-000000000000' then
        select count(*) into member_count from public.profiles where pair_id = current_pair;
        if member_count > 1 then raise exception 'already paired'; end if;
    end if;

    for attempt in 1..20 loop
        generated_code := upper(substr(md5(random()::text || clock_timestamp()::text || caller_id::text), 1, 6));
        exit when not exists (select 1 from public.profiles where pair_id = generated_code);
    end loop;
    if exists (select 1 from public.profiles where pair_id = generated_code) then
        raise exception 'unable to allocate pair code';
    end if;

    update public.profiles
       set pair_id = generated_code,
           selected_role = inviter_role,
           updated_at = now()
     where user_id = caller_id;

    return query select generated_code, inviter_role;
end;
$$;

create or replace function public.preview_pair_invite(invite_code text)
returns table(pair_code text, inviter_name text, inviter_role text)
language plpgsql
security definer
set search_path = public
as $$
declare
    normalized_code text := upper(trim(invite_code));
begin
    if auth.uid() is null then raise exception 'authentication required'; end if;
    if length(normalized_code) <> 6 then return; end if;

    return query
    select p.pair_id, p.nickname, p.selected_role
      from public.profiles p
     where p.pair_id = normalized_code
       and p.user_id <> auth.uid()
       and p.selected_role in ('caretaker', 'eater')
       and (select count(*) from public.profiles members where members.pair_id = normalized_code) = 1
     limit 1;
end;
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
    inviter_role text;
    invitee_role text;
    member_count integer;
    current_pair text;
begin
    if caller_id is null then raise exception 'authentication required'; end if;
    if length(normalized_code) <> 6 then raise exception 'invalid pair code'; end if;

    select pair_id into current_pair
      from public.profiles
     where user_id = caller_id
     for update;
    if not found then raise exception 'profile not found'; end if;

    if current_pair <> ''
       and current_pair <> '00000000-0000-0000-0000-000000000000'
       and current_pair <> normalized_code
       and exists (select 1 from public.profiles where pair_id = current_pair and user_id <> caller_id) then
        raise exception 'already paired';
    end if;

    perform 1 from public.profiles where pair_id = normalized_code for update;
    select count(*), max(selected_role)
      into member_count, inviter_role
      from public.profiles
     where pair_id = normalized_code
       and user_id <> caller_id;

    if member_count <> 1 or inviter_role not in ('caretaker', 'eater') then
        raise exception 'pair invite unavailable';
    end if;

    invitee_role := case when inviter_role = 'caretaker' then 'eater' else 'caretaker' end;
    update public.profiles
       set pair_id = normalized_code,
           selected_role = invitee_role,
           updated_at = now()
     where user_id = caller_id;

    return query select normalized_code, invitee_role;
end;
$$;

revoke all on function public.create_pair_invite(text) from public;
revoke all on function public.preview_pair_invite(text) from public;
revoke all on function public.join_pair_invite(text) from public;
grant execute on function public.create_pair_invite(text) to authenticated;
grant execute on function public.preview_pair_invite(text) to authenticated;
grant execute on function public.join_pair_invite(text) to authenticated;
