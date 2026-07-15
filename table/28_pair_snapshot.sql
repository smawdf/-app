-- Authoritative pair state for both devices.
-- Run after table/27_pair_events_and_atomic_unpair.sql.

create or replace function public.current_pair_snapshot()
returns table(
    pair_code text,
    is_paired boolean,
    partner_name text,
    partner_avatar_url text,
    partner_updated_at text,
    partner_candy_coins integer,
    notice_id text,
    notice_message text
)
language plpgsql
security definer
set search_path = public
as $$
declare
    caller_id uuid := auth.uid();
    caller_pair text;
begin
    if caller_id is null then raise exception 'authentication required'; end if;
    select pair_id into caller_pair from public.profiles where user_id = caller_id;
    if not found then raise exception 'profile not found'; end if;

    return query
    select
        coalesce(caller_pair, ''),
        partner.user_id is not null,
        coalesce(partner.nickname, ''),
        coalesce(partner.avatar_url, ''),
        coalesce(partner.updated_at::text, ''),
        partner.candy_coins,
        coalesce(event.id::text, ''),
        coalesce(event.message, '')
    from (select 1) seed
    left join lateral (
        select p.user_id, p.nickname, p.avatar_url, p.updated_at, p.candy_coins
          from public.profiles p
         where p.pair_id = caller_pair
           and p.user_id <> caller_id
           and caller_pair <> ''
           and caller_pair <> '00000000-0000-0000-0000-000000000000'
         limit 1
    ) partner on true
    left join lateral (
        select e.id, e.message
          from public.pair_events e
         where e.recipient_id = caller_id
           and e.read_at is null
         order by e.created_at desc
         limit 1
    ) event on true;
end;
$$;

revoke all on function public.current_pair_snapshot() from public;
grant execute on function public.current_pair_snapshot() to authenticated;

