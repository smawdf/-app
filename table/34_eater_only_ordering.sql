-- Only the eater role may spend candy coins to submit an order.
-- Run after table/33_pair_sync_integrity.sql.

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
    new_balance integer;
    existing_balance integer;
    ledger_id text := 'spend-order-' || record_id::text;
begin
    if caller_id is null then raise exception 'authentication required'; end if;
    if amount is null or amount <= 0 then raise exception 'amount must be positive'; end if;
    if record_id is null then raise exception 'record id is required'; end if;

    select pair_id, selected_role
      into caller_pair_id, caller_role
      from public.profiles
     where user_id = caller_id;
    if not found then raise exception 'caller profile not found'; end if;
    if caller_role <> 'eater' then raise exception 'eater role required'; end if;

    perform pg_advisory_xact_lock(hashtextextended(ledger_id, 0));

    select balance_after into existing_balance
      from public.candy_coin_records
     where id = ledger_id
       and user_id = caller_id
       and type = 'spend'
       and public.candy_coin_records.amount = -spend_eater_candy_coins.amount;
    if found then return existing_balance; end if;
    if exists (select 1 from public.candy_coin_records where id = ledger_id) then
        raise exception 'record id conflict';
    end if;

    update public.profiles
       set candy_coins = candy_coins - amount,
           updated_at = now()
     where user_id = caller_id
       and candy_coins >= amount
     returning candy_coins into new_balance;
    if new_balance is null then raise exception 'insufficient candy coins'; end if;

    insert into public.candy_coin_records (
        id, pair_id, user_id, type, amount, balance_after,
        actor_role, target_role, note, created_at
    ) values (
        ledger_id, coalesce(caller_pair_id, ''), caller_id, 'spend', -amount, new_balance,
        'eater', 'eater', 'order spend', now()
    );

    return new_balance;
end;
$$;

revoke all on function public.spend_eater_candy_coins(integer, uuid) from public;
grant execute on function public.spend_eater_candy_coins(integer, uuid) to authenticated;
