-- Only the caretaker may accept an order and move it into preparation.
-- Run after table/34_eater_only_ordering.sql.

alter table public.orders
    drop constraint if exists orders_status_check;

alter table public.orders
    add constraint orders_status_check
    check (status in ('submitted', 'confirmed', 'preparing', 'delivering', 'completed', 'cancelled'));

create or replace function public.transition_order_status(target_order_id uuid, new_status text)
returns text
language plpgsql
security definer
set search_path = public
as $$
declare
    caller_id uuid := auth.uid();
    target_order public.orders%rowtype;
    caller_pair_id text;
    caller_role text;
    normalized_status text;
begin
    if caller_id is null then raise exception 'authentication required'; end if;

    normalized_status := case new_status
        when 'confirmed' then 'preparing'
        when 'delivering' then 'completed'
        else new_status
    end;
    if normalized_status not in ('preparing', 'completed') then
        raise exception 'invalid target status';
    end if;

    select pair_id, selected_role
      into caller_pair_id, caller_role
      from public.profiles
     where user_id = caller_id;
    if not found then raise exception 'caller profile not found'; end if;
    if caller_role <> 'caretaker' then raise exception 'caretaker role required'; end if;

    select * into target_order
      from public.orders
     where id = target_order_id
     for update;
    if not found then raise exception 'order not found'; end if;
    if not (
        target_order.user_id = caller_id
        or (
            target_order.pair_id <> ''
            and target_order.pair_id <> '00000000-0000-0000-0000-000000000000'
            and target_order.pair_id = caller_pair_id
        )
    ) then
        raise exception 'order access denied';
    end if;

    if not (
        (target_order.status in ('submitted', 'confirmed') and normalized_status = 'preparing')
        or (target_order.status in ('preparing', 'delivering') and normalized_status = 'completed')
    ) then
        raise exception 'invalid order transition';
    end if;

    update public.orders
       set status = normalized_status
     where id = target_order_id;

    return normalized_status;
end;
$$;

revoke all on function public.transition_order_status(uuid, text) from public;
grant execute on function public.transition_order_status(uuid, text) to authenticated;
