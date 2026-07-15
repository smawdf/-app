-- Repair profile integrity required by pairing and cloud profile sync.
-- Run after table/28_pair_snapshot.sql.

alter table public.profiles
    add column if not exists candy_coins integer not null default 66;

with profile_duplicate_map as (
    select id as duplicate_id, keeper_id
    from (
        select id,
            first_value(id) over (partition by user_id order by updated_at desc nulls last, created_at desc nulls last, id) as keeper_id,
            row_number() over (partition by user_id order by updated_at desc nulls last, created_at desc nulls last, id) as row_number
        from public.profiles where user_id is not null
    ) ranked where row_number > 1
)
update public.dishes target
set created_by = duplicates.keeper_id
from profile_duplicate_map duplicates
where target.created_by = duplicates.duplicate_id;

with profile_duplicate_map as (
    select id as duplicate_id, keeper_id
    from (
        select id,
            first_value(id) over (partition by user_id order by updated_at desc nulls last, created_at desc nulls last, id) as keeper_id,
            row_number() over (partition by user_id order by updated_at desc nulls last, created_at desc nulls last, id) as row_number
        from public.profiles where user_id is not null
    ) ranked where row_number > 1
)
update public.meals target
set created_by = duplicates.keeper_id
from profile_duplicate_map duplicates
where target.created_by = duplicates.duplicate_id;

with profile_duplicate_map as (
    select id as duplicate_id, keeper_id
    from (
        select id,
            first_value(id) over (partition by user_id order by updated_at desc nulls last, created_at desc nulls last, id) as keeper_id,
            row_number() over (partition by user_id order by updated_at desc nulls last, created_at desc nulls last, id) as row_number
        from public.profiles where user_id is not null
    ) ranked where row_number > 1
)
update public.meal_items target
set chosen_by = duplicates.keeper_id
from profile_duplicate_map duplicates
where target.chosen_by = duplicates.duplicate_id;

with profile_duplicate_map as (
    select id as duplicate_id, keeper_id
    from (
        select id,
            first_value(id) over (partition by user_id order by updated_at desc nulls last, created_at desc nulls last, id) as keeper_id,
            row_number() over (partition by user_id order by updated_at desc nulls last, created_at desc nulls last, id) as row_number
        from public.profiles where user_id is not null
    ) ranked where row_number > 1
)
update public.wishlists target
set added_by = duplicates.keeper_id
from profile_duplicate_map duplicates
where target.added_by = duplicates.duplicate_id;

with profile_duplicate_map as (
    select id as duplicate_id
    from (
        select id,
            row_number() over (partition by user_id order by updated_at desc nulls last, created_at desc nulls last, id) as row_number
        from public.profiles where user_id is not null
    ) ranked where row_number > 1
)
delete from public.profiles target
using profile_duplicate_map duplicates
where target.id = duplicates.duplicate_id;

create unique index if not exists ux_profiles_user_id
    on public.profiles(user_id);

-- Reinstall the snapshot after the schema repair so PostgREST refreshes it.
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
stable
security definer
set search_path = public
as $$
declare
    caller_id uuid := auth.uid();
    caller_pair text;
begin
    if caller_id is null then raise exception 'authentication required'; end if;
    select p.pair_id into caller_pair
      from public.profiles p
     where p.user_id = caller_id;
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
         where e.recipient_id = caller_id and e.read_at is null
         order by e.created_at desc
         limit 1
    ) event on true;
end;
$$;

revoke all on function public.current_pair_snapshot() from public;
grant execute on function public.current_pair_snapshot() to authenticated;

select user_id, count(*) as profile_count
from public.profiles
where user_id is not null
group by user_id
having count(*) > 1;
