-- Phase 4 ordering flow tables.
-- Run after the existing OrderDisk schema scripts in Supabase SQL Editor.

create table if not exists public.addresses (
    id uuid primary key,
    user_id uuid references auth.users(id) on delete cascade,
    contact_name text not null,
    contact_phone text not null,
    address_line1 text not null,
    address_line2 text not null default '',
    tag text not null default '',
    is_default boolean not null default false,
    created_at timestamptz not null default now()
);

create table if not exists public.orders (
    id uuid primary key,
    user_id uuid references auth.users(id) on delete set null,
    shop_id text not null,
    shop_name text not null,
    shop_cover_url text not null default '',
    status text not null default 'submitted'
        check (status in ('submitted', 'confirmed', 'delivering', 'completed', 'cancelled')),
    address_snapshot text not null,
    buyer_note text not null default '',
    subtotal numeric(10, 2) not null default 0,
    delivery_fee numeric(10, 2) not null default 0,
    total_price numeric(10, 2) not null default 0,
    created_at timestamptz not null default now()
);

create table if not exists public.order_items (
    id uuid primary key,
    order_id uuid not null references public.orders(id) on delete cascade,
    menu_item_id text not null,
    menu_item_name text not null,
    menu_item_image_url text not null default '',
    unit_price numeric(10, 2) not null default 0,
    quantity integer not null check (quantity > 0),
    subtotal numeric(10, 2) not null default 0
);

create index if not exists idx_addresses_user_id on public.addresses(user_id);
create index if not exists idx_orders_user_id_created_at on public.orders(user_id, created_at desc);
create index if not exists idx_order_items_order_id on public.order_items(order_id);

alter table public.addresses enable row level security;
alter table public.orders enable row level security;
alter table public.order_items enable row level security;

create policy "Users can read own addresses"
    on public.addresses for select
    using (auth.uid() = user_id);

create policy "Users can manage own addresses"
    on public.addresses for all
    using (auth.uid() = user_id)
    with check (auth.uid() = user_id);

create policy "Users can read own orders"
    on public.orders for select
    using (auth.uid() = user_id);

create policy "Users can insert own orders"
    on public.orders for insert
    with check (auth.uid() = user_id);

create policy "Users can read own order items"
    on public.order_items for select
    using (
        exists (
            select 1
            from public.orders
            where public.orders.id = public.order_items.order_id
              and public.orders.user_id = auth.uid()
        )
    );

create policy "Users can insert own order items"
    on public.order_items for insert
    with check (
        exists (
            select 1
            from public.orders
            where public.orders.id = public.order_items.order_id
              and public.orders.user_id = auth.uid()
        )
    );
