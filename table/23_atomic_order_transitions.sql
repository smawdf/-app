-- Atomic order cancellation and buyer refund.
-- Run after table/22_secure_pairing.sql.

drop policy if exists "Users can update pair orders" on public.orders;

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
begin
    if caller_id is null then raise exception 'authentication required'; end if;
    if new_status not in ('confirmed', 'delivering', 'completed') then
        raise exception 'invalid target status';
    end if;

    select pair_id, selected_role into caller_pair_id, caller_role
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
    if caller_role <> 'caretaker' then raise exception 'caretaker role required'; end if;
    if not (
        (target_order.status = 'submitted' and new_status = 'confirmed')
        or (target_order.status = 'confirmed' and new_status = 'delivering')
        or (target_order.status = 'delivering' and new_status = 'completed')
    ) then
        raise exception 'invalid order transition';
    end if;

    update public.orders set status = new_status where id = target_order_id;
    return new_status;
end;
$$;

create or replace function public.cancel_order_and_refund(target_order_id uuid)
returns table(order_status text, refunded_coins integer)
language plpgsql
security definer
set search_path = public
as $$
declare
    caller_id uuid := auth.uid();
    target_order public.orders%rowtype;
    caller_pair_id text;
    new_balance integer;
begin
    if caller_id is null then raise exception 'authentication required'; end if;

    select * into target_order
      from public.orders
     where id = target_order_id
     for update;
    if not found then raise exception 'order not found'; end if;

    select pair_id into caller_pair_id
      from public.profiles
     where user_id = caller_id;
    if target_order.user_id <> caller_id and not (
        target_order.pair_id <> ''
        and target_order.pair_id <> '00000000-0000-0000-0000-000000000000'
        and target_order.pair_id = caller_pair_id
    ) then
        raise exception 'order access denied';
    end if;

    if target_order.status = 'cancelled' then
        return query select target_order.status, 0;
        return;
    end if;
    if target_order.status = 'completed' then
        raise exception 'completed order cannot be cancelled';
    end if;
    if target_order.user_id is null then
        raise exception 'order buyer is unavailable';
    end if;

    update public.orders
       set status = 'cancelled'
     where id = target_order_id;

    update public.profiles
       set candy_coins = least(9999, candy_coins + target_order.candy_coins_spent),
           updated_at = now()
     where user_id = target_order.user_id
     returning candy_coins into new_balance;
    if not found then raise exception 'buyer profile not found'; end if;

    insert into public.candy_coin_records (
        id, pair_id, user_id, type, amount, balance_after,
        actor_role, target_role, note, created_at
    ) values (
        'refund-order-' || target_order.id::text,
        target_order.pair_id,
        target_order.user_id,
        'refund',
        target_order.candy_coins_spent,
        new_balance,
        'system',
        'eater',
        'order cancellation refund',
        now()
    )
    on conflict (id) do nothing;

    return query select 'cancelled'::text, target_order.candy_coins_spent;
end;
$$;

revoke all on function public.cancel_order_and_refund(uuid) from public;
revoke all on function public.transition_order_status(uuid, text) from public;
grant execute on function public.cancel_order_and_refund(uuid) to authenticated;
grant execute on function public.transition_order_status(uuid, text) to authenticated;
