-- Allow unpaired accounts to sync their own shop settings and menu dishes.
-- Safe to run multiple times in Supabase SQL Editor.
-- The app stores unpaired shop data with pair_id = 'user:' || auth.uid().
-- After pairing, it continues to use the real pair_id.

drop policy if exists "Pair members can manage shop settings" on public.shop_settings;
create policy "Pair members can manage shop settings"
    on public.shop_settings for all
    using (
        pair_id in (select pair_id from public.profiles where user_id = auth.uid())
        or pair_id = ('user:' || auth.uid()::text)
    )
    with check (
        pair_id in (select pair_id from public.profiles where user_id = auth.uid())
        or pair_id = ('user:' || auth.uid()::text)
    );

drop policy if exists "Pair members can manage menu dishes" on public.menu_dishes;
create policy "Pair members can manage menu dishes"
    on public.menu_dishes for all
    using (
        pair_id in (select pair_id from public.profiles where user_id = auth.uid())
        or pair_id = ('user:' || auth.uid()::text)
    )
    with check (
        pair_id in (select pair_id from public.profiles where user_id = auth.uid())
        or pair_id = ('user:' || auth.uid()::text)
    );
