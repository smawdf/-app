-- Atomic candy coin spending for order submission.
-- Safe to run multiple times after table/20_client_error_logs.sql.

create or replace function public.spend_candy_coins(amount integer)
returns integer
language plpgsql
security definer
set search_path = public
as $$
declare
    new_balance integer;
begin
    if amount is null or amount <= 0 then
        raise exception 'amount must be positive';
    end if;

    update public.profiles
       set candy_coins = candy_coins - amount,
           updated_at = now()
     where user_id = auth.uid()
       and candy_coins >= amount
     returning candy_coins into new_balance;

    if new_balance is null then
        raise exception 'insufficient candy coins';
    end if;

    return new_balance;
end;
$$;

grant execute on function public.spend_candy_coins(integer) to authenticated;
