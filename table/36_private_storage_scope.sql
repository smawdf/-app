-- Restrict private-release image uploads to the current user or pair folder.
-- Run after table/35_caretaker_order_acceptance.sql.

alter table public.user_preferences
    alter column order_notifications_enabled set default true;

update public.user_preferences
   set order_notifications_enabled = true,
       updated_at = now()
 where order_notifications_enabled = false;

drop policy if exists "Authenticated users can upload images" on storage.objects;
drop policy if exists "Users can upload to their pair folder" on storage.objects;

create policy "Users can upload to their private scope"
    on storage.objects for insert
    with check (
        bucket_id = 'dish-images'
        and auth.uid() is not null
        and (
            (storage.foldername(name))[1] = ('user:' || auth.uid()::text)
            or (storage.foldername(name))[1] in (
                select pair_id
                from public.profiles
                where user_id = auth.uid()
                  and pair_id <> ''
                  and pair_id <> '00000000-0000-0000-0000-000000000000'
            )
        )
    );
