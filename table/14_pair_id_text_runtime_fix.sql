-- Pair code runtime schema fix.
-- Safe to run multiple times in Supabase SQL Editor.
-- Purpose: current Android app uses 6-character invite codes as pair_id, so
-- pair_id columns must be text instead of uuid.
--
-- Important: pair_id is referenced by RLS policies, so this script drops the
-- dependent policies first, changes the column types, then recreates policies.

drop policy if exists "Users can manage own profile" on public.profiles;

drop policy if exists "Authenticated users can view all dishes" on public.dishes;
drop policy if exists "Pair members can view dishes" on public.dishes;
drop policy if exists "Pair members can insert dishes" on public.dishes;
drop policy if exists "Pair members can update dishes" on public.dishes;
drop policy if exists "Pair members can delete dishes" on public.dishes;

drop policy if exists "Pair members can access dish tags" on public.dish_tags;
drop policy if exists "Pair members can access meals" on public.meals;
drop policy if exists "Pair members can access meal items" on public.meal_items;
drop policy if exists "Pair members can access wishlists" on public.wishlists;

drop policy if exists "Users can read pair orders" on public.orders;
drop policy if exists "Users can read pair order items" on public.order_items;
drop policy if exists "Users can update pair orders" on public.orders;

do $$
begin
    if exists (
        select 1
          from information_schema.columns
         where table_schema = 'public'
           and table_name = 'profiles'
           and column_name = 'pair_id'
           and data_type <> 'text'
    ) then
        alter table public.profiles
            alter column pair_id type text using pair_id::text;
    end if;

    if exists (
        select 1
          from information_schema.columns
         where table_schema = 'public'
           and table_name = 'dishes'
           and column_name = 'pair_id'
           and data_type <> 'text'
    ) then
        alter table public.dishes
            alter column pair_id type text using pair_id::text;
    end if;

    if exists (
        select 1
          from information_schema.columns
         where table_schema = 'public'
           and table_name = 'dish_tags'
           and column_name = 'pair_id'
           and data_type <> 'text'
    ) then
        alter table public.dish_tags
            alter column pair_id type text using pair_id::text;
    end if;

    if exists (
        select 1
          from information_schema.columns
         where table_schema = 'public'
           and table_name = 'meals'
           and column_name = 'pair_id'
           and data_type <> 'text'
    ) then
        alter table public.meals
            alter column pair_id type text using pair_id::text;
    end if;

    if exists (
        select 1
          from information_schema.columns
         where table_schema = 'public'
           and table_name = 'wishlists'
           and column_name = 'pair_id'
           and data_type <> 'text'
    ) then
        alter table public.wishlists
            alter column pair_id type text using pair_id::text;
    end if;
end $$;

alter table public.profiles enable row level security;
alter table public.dishes enable row level security;
alter table public.dish_tags enable row level security;
alter table public.meals enable row level security;
alter table public.meal_items enable row level security;
alter table public.wishlists enable row level security;

alter table public.orders
    add column if not exists pair_id text not null default '',
    add column if not exists buyer_name text not null default '',
    add column if not exists buyer_avatar_url text not null default '',
    add column if not exists buyer_role text not null default '',
    add column if not exists candy_coins_spent integer not null default 0;

alter table public.orders enable row level security;
alter table public.order_items enable row level security;

create policy "Users can manage own profile"
    on public.profiles for all
    using (user_id = auth.uid())
    with check (user_id = auth.uid());

create policy "Pair members can view dishes"
    on public.dishes for select
    using (
        pair_id in (
            select pair_id from public.profiles where user_id = auth.uid()
        )
    );

create policy "Pair members can insert dishes"
    on public.dishes for insert
    with check (
        pair_id in (
            select pair_id from public.profiles where user_id = auth.uid()
        )
    );

create policy "Pair members can update dishes"
    on public.dishes for update
    using (
        pair_id in (
            select pair_id from public.profiles where user_id = auth.uid()
        )
    )
    with check (
        pair_id in (
            select pair_id from public.profiles where user_id = auth.uid()
        )
    );

create policy "Pair members can delete dishes"
    on public.dishes for delete
    using (
        pair_id in (
            select pair_id from public.profiles where user_id = auth.uid()
        )
    );

create policy "Pair members can access dish tags"
    on public.dish_tags for all
    using (
        pair_id in (
            select pair_id from public.profiles where user_id = auth.uid()
        )
    )
    with check (
        pair_id in (
            select pair_id from public.profiles where user_id = auth.uid()
        )
    );

create policy "Pair members can access meals"
    on public.meals for all
    using (
        pair_id in (
            select pair_id from public.profiles where user_id = auth.uid()
        )
    )
    with check (
        pair_id in (
            select pair_id from public.profiles where user_id = auth.uid()
        )
    );

create policy "Pair members can access meal items"
    on public.meal_items for all
    using (
        meal_id in (
            select m.id
              from public.meals m
              join public.profiles p on p.pair_id = m.pair_id
             where p.user_id = auth.uid()
        )
    );

create policy "Pair members can access wishlists"
    on public.wishlists for all
    using (
        pair_id in (
            select pair_id from public.profiles where user_id = auth.uid()
        )
    )
    with check (
        pair_id in (
            select pair_id from public.profiles where user_id = auth.uid()
        )
    );

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

create index if not exists idx_profiles_pair_id on public.profiles(pair_id);
create index if not exists idx_dishes_pair_id on public.dishes(pair_id);
create index if not exists idx_dish_tags_pair_id on public.dish_tags(pair_id);
create index if not exists idx_meals_pair_id on public.meals(pair_id);
create index if not exists idx_wishlists_pair_id on public.wishlists(pair_id);
create index if not exists idx_orders_pair_id_created_at on public.orders(pair_id, created_at desc);
