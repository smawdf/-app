-- Cloud user assets for device switching.
-- Safe to run multiple times in Supabase SQL Editor.
-- Purpose: persist shop settings, menu dishes, candy coin ledger, and anniversaries by pair/account.

create table if not exists public.shop_settings (
    pair_id text primary key,
    name text not null default '我的小店',
    image_url text not null default '',
    announcement text not null default '欢迎来到我们的温馨小店！今天有新鲜出炉的心形披萨哦~ 🐾',
    categories text[] not null default '{}',
    updated_at timestamptz not null default now()
);

create table if not exists public.menu_dishes (
    id text primary key,
    pair_id text not null,
    name text not null,
    price double precision not null default 0,
    origin_price double precision,
    image_url text not null default '',
    category text not null default '其他',
    description text not null default '',
    sort_order integer not null default 0,
    monthly_sales integer not null default 0,
    stock integer not null default 0,
    is_available boolean not null default true,
    is_signature boolean not null default false,
    updated_at timestamptz not null default now()
);

create table if not exists public.candy_coin_records (
    id text primary key,
    pair_id text not null,
    user_id uuid references auth.users(id) on delete set null,
    type text not null,
    amount integer not null,
    balance_after integer not null,
    actor_role text not null default '',
    target_role text not null default '',
    note text not null default '',
    created_at timestamptz not null default now()
);

create table if not exists public.anniversaries (
    pair_id text primary key,
    paired_at text not null default '',
    updated_at timestamptz not null default now()
);

alter table public.shop_settings enable row level security;
alter table public.menu_dishes enable row level security;
alter table public.candy_coin_records enable row level security;
alter table public.anniversaries enable row level security;

drop policy if exists "Pair members can manage shop settings" on public.shop_settings;
create policy "Pair members can manage shop settings"
    on public.shop_settings for all
    using (pair_id in (select pair_id from public.profiles where user_id = auth.uid()))
    with check (pair_id in (select pair_id from public.profiles where user_id = auth.uid()));

drop policy if exists "Pair members can manage menu dishes" on public.menu_dishes;
create policy "Pair members can manage menu dishes"
    on public.menu_dishes for all
    using (pair_id in (select pair_id from public.profiles where user_id = auth.uid()))
    with check (pair_id in (select pair_id from public.profiles where user_id = auth.uid()));

drop policy if exists "Pair members can manage candy coin records" on public.candy_coin_records;
create policy "Pair members can manage candy coin records"
    on public.candy_coin_records for all
    using (pair_id in (select pair_id from public.profiles where user_id = auth.uid()))
    with check (pair_id in (select pair_id from public.profiles where user_id = auth.uid()));

drop policy if exists "Pair members can manage anniversaries" on public.anniversaries;
create policy "Pair members can manage anniversaries"
    on public.anniversaries for all
    using (pair_id in (select pair_id from public.profiles where user_id = auth.uid()))
    with check (pair_id in (select pair_id from public.profiles where user_id = auth.uid()));

create index if not exists idx_menu_dishes_pair_id_updated_at
    on public.menu_dishes(pair_id, updated_at desc);

create index if not exists idx_candy_coin_records_pair_id_created_at
    on public.candy_coin_records(pair_id, created_at desc);
