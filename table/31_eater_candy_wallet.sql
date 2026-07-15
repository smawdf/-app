-- Unify candy coin display, spending, and refunds around the eater wallet.
-- Run after table/30_missing_profiles_and_upsert_fix.sql.

create or replace function public.spend_eater_candy_coins(amount integer, record_id uuid)
returns integer
language plpgsql
security definer
set search_path = public
as $$
declare
    caller_id uuid := auth.uid();
    caller_pair_id text;
    caller_role text;
    wallet_user_id uuid;
    new_balance integer;
    existing_balance integer;
    ledger_id text := 'spend-order-' || record_id::text;
begin
    if caller_id is null then raise exception 'authentication required'; end if;
    if amount is null or amount <= 0 then raise exception 'amount must be positive'; end if;
    if record_id is null then raise exception 'record id is required'; end if;

    perform pg_advisory_xact_lock(hashtextextended(ledger_id, 0));

    select balance_after into existing_balance
      from public.candy_coin_records
     where id = ledger_id
       and type = 'spend'
       and public.candy_coin_records.amount = -spend_eater_candy_coins.amount;
    if found then return existing_balance; end if;

    select pair_id, selected_role
      into caller_pair_id, caller_role
      from public.profiles
     where user_id = caller_id;
    if not found then raise exception 'caller profile not found'; end if;

    wallet_user_id := caller_id;
    if caller_pair_id is not null
       and caller_pair_id <> ''
       and caller_pair_id <> '00000000-0000-0000-0000-000000000000' then
        select user_id into wallet_user_id
          from public.profiles
         where pair_id = caller_pair_id
           and selected_role = 'eater'
         order by case when user_id = caller_id then 0 else 1 end
         limit 1
         for update;
        if wallet_user_id is null then raise exception 'eater wallet not found'; end if;
    end if;

    update public.profiles
       set candy_coins = candy_coins - amount,
           updated_at = now()
     where user_id = wallet_user_id
       and candy_coins >= amount
     returning candy_coins into new_balance;
    if new_balance is null then raise exception 'insufficient candy coins'; end if;

    insert into public.candy_coin_records (
        id, pair_id, user_id, type, amount, balance_after,
        actor_role, target_role, note, created_at
    ) values (
        ledger_id, coalesce(caller_pair_id, ''), wallet_user_id, 'spend', -amount, new_balance,
        coalesce(caller_role, ''), 'eater', 'order spend', now()
    );

    return new_balance;
end;
$$;

create or replace function public.refund_eater_candy_coins(amount integer, record_id uuid)
returns integer
language plpgsql
security definer
set search_path = public
as $$
declare
    caller_id uuid := auth.uid();
    caller_pair_id text;
    caller_role text;
    wallet_user_id uuid;
    new_balance integer;
    existing_balance integer;
    spend_id text := 'spend-order-' || record_id::text;
    refund_id text := 'rollback-order-' || record_id::text;
begin
    if caller_id is null then raise exception 'authentication required'; end if;
    if amount is null or amount <= 0 then raise exception 'amount must be positive'; end if;
    if record_id is null then raise exception 'record id is required'; end if;

    perform pg_advisory_xact_lock(hashtextextended(spend_id, 0));

    select balance_after into existing_balance
      from public.candy_coin_records
     where id = refund_id
       and type = 'refund';
    if found then return existing_balance; end if;

    if exists (select 1 from public.orders where id = record_id) then
        raise exception 'order already exists; use order cancellation';
    end if;

    select pair_id, selected_role
      into caller_pair_id, caller_role
      from public.profiles
     where user_id = caller_id;
    if not found then raise exception 'caller profile not found'; end if;

    select user_id into wallet_user_id
      from public.candy_coin_records
     where id = spend_id
       and type = 'spend'
       and public.candy_coin_records.amount = -refund_eater_candy_coins.amount;
    if wallet_user_id is null then raise exception 'matching spend record not found'; end if;

    if wallet_user_id <> caller_id and not exists (
        select 1 from public.profiles
         where user_id = wallet_user_id
           and pair_id = caller_pair_id
    ) then
        raise exception 'wallet access denied';
    end if;

    update public.profiles
       set candy_coins = least(9999, candy_coins + amount),
           updated_at = now()
     where user_id = wallet_user_id
     returning candy_coins into new_balance;

    insert into public.candy_coin_records (
        id, pair_id, user_id, type, amount, balance_after,
        actor_role, target_role, note, created_at
    ) values (
        refund_id, coalesce(caller_pair_id, ''), wallet_user_id, 'refund', amount, new_balance,
        'system', 'eater', 'local order rollback', now()
    );

    return new_balance;
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
    wallet_user_id uuid;
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

    select user_id into wallet_user_id
      from public.candy_coin_records
     where id = 'spend-order-' || target_order.id::text
       and type = 'spend';
    wallet_user_id := coalesce(wallet_user_id, target_order.user_id);
    if wallet_user_id is null then raise exception 'order wallet is unavailable'; end if;

    update public.orders
       set status = 'cancelled'
     where id = target_order_id;

    update public.profiles
       set candy_coins = least(9999, candy_coins + target_order.candy_coins_spent),
           updated_at = now()
     where user_id = wallet_user_id
     returning candy_coins into new_balance;
    if not found then raise exception 'wallet profile not found'; end if;

    insert into public.candy_coin_records (
        id, pair_id, user_id, type, amount, balance_after,
        actor_role, target_role, note, created_at
    ) values (
        'refund-order-' || target_order.id::text,
        target_order.pair_id,
        wallet_user_id,
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

revoke all on function public.spend_eater_candy_coins(integer, uuid) from public;
revoke all on function public.refund_eater_candy_coins(integer, uuid) from public;
revoke all on function public.cancel_order_and_refund(uuid) from public;
grant execute on function public.spend_eater_candy_coins(integer, uuid) to authenticated;
grant execute on function public.refund_eater_candy_coins(integer, uuid) to authenticated;
grant execute on function public.cancel_order_and_refund(uuid) to authenticated;
