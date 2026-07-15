-- Repair nullable legacy profile session fields that break Kotlin decoding.
-- Run after table/31_eater_candy_wallet.sql.

update public.profiles
   set session_id = ''
 where session_id is null;

update public.profiles
   set session_updated_at = ''
 where session_updated_at is null;

alter table public.profiles
    alter column session_id set default '',
    alter column session_id set not null,
    alter column session_updated_at set default '',
    alter column session_updated_at set not null;

-- Keep other non-null Kotlin profile fields safe for legacy rows as well.
update public.profiles
   set selected_role = ''
 where selected_role is null;

alter table public.profiles
    alter column selected_role set default '',
    alter column selected_role set not null;
