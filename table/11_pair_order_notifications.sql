-- Pair order notification support.
-- Run this after table/09_orders_addresses.sql.
-- It lets paired users see the same active orders and gives the app enough
-- metadata to render different home notifications for caretaker/eater roles.

alter table public.orders
    add column if not exists pair_id text not null default '',
    add column if not exists buyer_name text not null default '',
    add column if not exists buyer_avatar_url text not null default '',
    add column if not exists buyer_role text not null default '';

create index if not exists idx_orders_pair_id_created_at
    on public.orders(pair_id, created_at desc);

drop policy if exists "Users can read own orders" on public.orders;
drop policy if exists "Users can read own order items" on public.order_items;
drop policy if exists "Users can update pair orders" on public.orders;

create policy "Users can read pair orders"
    on public.orders for select
    using (
        auth.uid() = user_id
        or exists (
            select 1
            from public.profiles
            where public.profiles.user_id = auth.uid()
              and public.profiles.pair_id = public.orders.pair_id
              and public.orders.pair_id <> ''
              and public.orders.pair_id <> '00000000-0000-0000-0000-000000000000'
        )
    );

create policy "Users can read pair order items"
    on public.order_items for select
    using (
        exists (
            select 1
            from public.orders
            where public.orders.id = public.order_items.order_id
              and (
                  public.orders.user_id = auth.uid()
                  or exists (
                      select 1
                      from public.profiles
                      where public.profiles.user_id = auth.uid()
                        and public.profiles.pair_id = public.orders.pair_id
                        and public.orders.pair_id <> ''
                        and public.orders.pair_id <> '00000000-0000-0000-0000-000000000000'
                  )
              )
        )
    );

create policy "Users can update pair orders"
    on public.orders for update
    using (
        auth.uid() = user_id
        or exists (
            select 1
            from public.profiles
            where public.profiles.user_id = auth.uid()
              and public.profiles.pair_id = public.orders.pair_id
              and public.orders.pair_id <> ''
              and public.orders.pair_id <> '00000000-0000-0000-0000-000000000000'
        )
    )
    with check (
        auth.uid() = user_id
        or exists (
            select 1
            from public.profiles
            where public.profiles.user_id = auth.uid()
              and public.profiles.pair_id = public.orders.pair_id
              and public.orders.pair_id <> ''
              and public.orders.pair_id <> '00000000-0000-0000-0000-000000000000'
        )
    );
