-- Repair installations that already ran table/29 with a partial unique index.
-- Also creates a base profile for authenticated users whose profile is missing.

drop index if exists public.ux_profiles_user_id;

create unique index ux_profiles_user_id
    on public.profiles(user_id);

insert into public.profiles(user_id, pair_id, nickname, updated_at)
select
    users.id,
    '00000000-0000-0000-0000-000000000000',
    coalesce(
        nullif(users.raw_user_meta_data ->> 'nickname', ''),
        nullif(users.raw_user_meta_data ->> 'name', ''),
        split_part(coalesce(users.email, ''), '@', 1),
        '高糖用户'
    ),
    now()
from auth.users users
where not exists (
    select 1 from public.profiles profiles where profiles.user_id = users.id
)
on conflict (user_id) do nothing;

select users.id, users.email
from auth.users users
left join public.profiles profiles on profiles.user_id = users.id
where profiles.user_id is null;
