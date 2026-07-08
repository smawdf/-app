-- Candy coins support.
-- Run this after table/11_pair_order_notifications.sql.
-- Candy coins are an in-app playful ordering allowance, not real money.

alter table public.profiles
    add column if not exists candy_coins integer not null default 66;

alter table public.orders
    add column if not exists candy_coins_spent integer not null default 0;

create or replace function public.add_partner_candy_coins(amount integer)
returns integer
language plpgsql
security definer
set search_path = public
as $$
declare
    caller_pair_id text;
    new_balance integer;
begin
    if amount is null or amount <= 0 then
        raise exception 'amount must be positive';
    end if;

    select pair_id::text
      into caller_pair_id
      from public.profiles
     where user_id = auth.uid()
     limit 1;

    if caller_pair_id is null
       or caller_pair_id = ''
       or caller_pair_id = '00000000-0000-0000-0000-000000000000' then
        raise exception 'pair is required';
    end if;

    update public.profiles
       set candy_coins = least(candy_coins + amount, 9999),
           updated_at = now()
     where pair_id::text = caller_pair_id
       and user_id <> auth.uid()
     returning candy_coins into new_balance;

    if new_balance is null then
        raise exception 'partner profile not found';
    end if;

    return new_balance;
end;
$$;

grant execute on function public.add_partner_candy_coins(integer) to authenticated;
