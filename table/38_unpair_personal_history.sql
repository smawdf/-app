-- Preserve shared memories when a couple unpairs.
-- Run after table/37_sweet_moment_images.sql and before testing unpair.

alter table public.orders
    add column if not exists viewer_user_ids uuid[] not null default '{}';

-- Images remain readable because dish-images is a public bucket; copied rows keep their URLs.

create index if not exists idx_orders_viewer_user_ids
    on public.orders using gin(viewer_user_ids);

drop policy if exists "Pair members can manage anniversaries" on public.anniversaries;
create policy "Users can manage pair or personal anniversaries"
    on public.anniversaries for all
    using (
        pair_id in (select pair_id from public.profiles where user_id = auth.uid())
        or pair_id = ('user:' || auth.uid()::text)
    )
    with check (
        pair_id in (select pair_id from public.profiles where user_id = auth.uid())
        or pair_id = ('user:' || auth.uid()::text)
    );

drop policy if exists "Users can read pair orders" on public.orders;
create policy "Users can read pair orders"
    on public.orders for select
    using (
        auth.uid() = user_id
        or auth.uid() = any(viewer_user_ids)
        or exists (
            select 1 from public.profiles
             where public.profiles.user_id = auth.uid()
               and public.profiles.pair_id = public.orders.pair_id
               and public.orders.pair_id <> ''
               and public.orders.pair_id <> '00000000-0000-0000-0000-000000000000'
        )
    );

drop policy if exists "Users can read pair order items" on public.order_items;
create policy "Users can read pair order items"
    on public.order_items for select
    using (
        exists (
            select 1 from public.orders
             where public.orders.id = public.order_items.order_id
               and (
                   public.orders.user_id = auth.uid()
                   or auth.uid() = any(public.orders.viewer_user_ids)
                   or exists (
                       select 1 from public.profiles
                        where public.profiles.user_id = auth.uid()
                          and public.profiles.pair_id = public.orders.pair_id
                          and public.orders.pair_id <> ''
                          and public.orders.pair_id <> '00000000-0000-0000-0000-000000000000'
                   )
               )
        )
    );

create or replace function public.unpair_current_pair()
returns boolean
language plpgsql
security definer
set search_path = public
as $$
declare
    caller_id uuid := auth.uid();
    caller_pair text;
    caller_name text;
    partner_id uuid;
    member_ids uuid[];
    member_id uuid;
    personal_scope text;
begin
    if caller_id is null then raise exception 'authentication required'; end if;

    select pair_id, nickname into caller_pair, caller_name
      from public.profiles where user_id = caller_id for update;
    if caller_pair is null or caller_pair = '' or caller_pair = '00000000-0000-0000-0000-000000000000' then
        return false;
    end if;

    select array_agg(user_id order by user_id)
      into member_ids
      from public.profiles
     where pair_id = caller_pair;

    select user_id into partner_id
      from public.profiles
     where pair_id = caller_pair and user_id <> caller_id
     limit 1;

    update public.orders
       set viewer_user_ids = (
           select array_agg(distinct viewer_id)
             from unnest(viewer_user_ids || coalesce(member_ids, '{}')) as viewer_id
       )
     where pair_id = caller_pair;

    foreach member_id in array coalesce(member_ids, array[caller_id]) loop
        personal_scope := 'user:' || member_id::text;

        insert into public.shop_settings(pair_id, name, image_url, announcement, categories, updated_at)
        select personal_scope, name, image_url, announcement, categories, now()
          from public.shop_settings where pair_id = caller_pair
        on conflict (pair_id) do update set
            name = excluded.name,
            image_url = excluded.image_url,
            announcement = excluded.announcement,
            categories = excluded.categories,
            updated_at = excluded.updated_at;

        insert into public.menu_dishes(
            id, pair_id, name, price, origin_price, image_url, category, description,
            sort_order, monthly_sales, stock, is_available, is_signature, updated_at, deleted_at
        )
        select member_id::text || ':' || md5(id), personal_scope, name, price, origin_price,
               image_url, category, description, sort_order, monthly_sales, stock,
               is_available, is_signature, now(), deleted_at
          from public.menu_dishes where pair_id = caller_pair
        on conflict (id) do update set
            pair_id = excluded.pair_id,
            name = excluded.name,
            price = excluded.price,
            origin_price = excluded.origin_price,
            image_url = excluded.image_url,
            category = excluded.category,
            description = excluded.description,
            sort_order = excluded.sort_order,
            monthly_sales = excluded.monthly_sales,
            stock = excluded.stock,
            is_available = excluded.is_available,
            is_signature = excluded.is_signature,
            updated_at = excluded.updated_at,
            deleted_at = excluded.deleted_at;

        insert into public.anniversaries(pair_id, paired_at, updated_at)
        select personal_scope, paired_at, now()
          from public.anniversaries where pair_id = caller_pair
        on conflict (pair_id) do update set
            paired_at = excluded.paired_at,
            updated_at = excluded.updated_at;
    end loop;

    update public.profiles
       set pair_id = '00000000-0000-0000-0000-000000000000', selected_role = '', updated_at = now()
     where pair_id = caller_pair;

    if partner_id is not null then
        insert into public.pair_events(recipient_id, actor_id, event_type, message)
        values (partner_id, caller_id, 'unpaired', coalesce(nullif(caller_name, ''), '对方') || ' 已解除与你的绑定，共同记录已保留');
    end if;
    return true;
end;
$$;

revoke all on function public.unpair_current_pair() from public;
grant execute on function public.unpair_current_pair() to authenticated;
