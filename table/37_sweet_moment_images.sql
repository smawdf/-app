-- Custom images for completed-order sweet moments.
-- Run after table/36_private_storage_scope.sql.

alter table public.orders
    add column if not exists moment_image_url text not null default '';

create or replace function public.update_order_moment_image(
    target_order_id uuid,
    new_image_url text
)
returns text
language plpgsql
security definer
set search_path = public
as $$
declare
    caller_id uuid := auth.uid();
    target_order public.orders%rowtype;
    caller_pair_id text;
    normalized_image_url text := trim(coalesce(new_image_url, ''));
begin
    if caller_id is null then raise exception 'authentication required'; end if;
    if normalized_image_url <> '' and normalized_image_url !~ '^https?://' then
        raise exception 'invalid image url';
    end if;

    select pair_id into caller_pair_id
      from public.profiles
     where user_id = caller_id;

    select * into target_order
      from public.orders
     where id = target_order_id
     for update;
    if not found then raise exception 'order not found'; end if;

    if target_order.user_id <> caller_id and not (
        target_order.pair_id <> ''
        and target_order.pair_id <> '00000000-0000-0000-0000-000000000000'
        and target_order.pair_id = caller_pair_id
    ) then
        raise exception 'order access denied';
    end if;

    update public.orders
       set moment_image_url = normalized_image_url
     where id = target_order_id;

    return normalized_image_url;
end;
$$;

revoke all on function public.update_order_moment_image(uuid, text) from public;
grant execute on function public.update_order_moment_image(uuid, text) to authenticated;
