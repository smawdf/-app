-- Atomic partner candy coin recharge with ledger entry.
-- Run after table/23_atomic_order_transitions.sql.

create or replace function public.add_partner_candy_coins_with_record(
    amount integer,
    record_id text,
    record_note text default ''
)
returns integer
language plpgsql
security definer
set search_path = public
as $$
declare
    caller_id uuid := auth.uid();
    caller_pair_id text;
    caller_role text;
    partner_id uuid;
    new_balance integer;
    existing_balance integer;
begin
    if caller_id is null then raise exception 'authentication required'; end if;
    if amount is null or amount <= 0 then raise exception 'amount must be positive'; end if;
    if record_id is null or trim(record_id) = '' then raise exception 'record id is required'; end if;

    perform pg_advisory_xact_lock(hashtextextended(record_id, 0));

    select balance_after into existing_balance
      from public.candy_coin_records
     where id = record_id
       and user_id = caller_id
       and type = 'recharge'
       and public.candy_coin_records.amount = add_partner_candy_coins_with_record.amount;
    if found then return existing_balance; end if;
    if exists (select 1 from public.candy_coin_records where id = record_id) then
        raise exception 'record id conflict';
    end if;

    select pair_id, selected_role
      into caller_pair_id, caller_role
      from public.profiles
     where user_id = caller_id;
    if caller_pair_id is null
       or caller_pair_id = ''
       or caller_pair_id = '00000000-0000-0000-0000-000000000000' then
        raise exception 'pair is required';
    end if;
    if caller_role <> 'caretaker' then raise exception 'caretaker role required'; end if;

    select user_id
      into partner_id
      from public.profiles
     where pair_id = caller_pair_id
       and user_id <> caller_id
     for update;
    if partner_id is null then raise exception 'partner profile not found'; end if;

    update public.profiles
       set candy_coins = least(candy_coins + amount, 9999),
           updated_at = now()
     where user_id = partner_id
     returning candy_coins into new_balance;

    insert into public.candy_coin_records (
        id, pair_id, user_id, type, amount, balance_after,
        actor_role, target_role, note, created_at
    ) values (
        record_id, caller_pair_id, caller_id, 'recharge', amount, new_balance,
        'caretaker', 'eater', coalesce(record_note, ''), now()
    )
    on conflict (id) do nothing;

    return new_balance;
end;
$$;

revoke all on function public.add_partner_candy_coins_with_record(integer, text, text) from public;
grant execute on function public.add_partner_candy_coins_with_record(integer, text, text) to authenticated;
