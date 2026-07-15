-- One-time cleanup for deployments that no longer require email confirmation.
-- First disable "Confirm email" in Supabase Dashboard:
-- Authentication -> Providers -> Email -> Confirm email = OFF.
-- Then run this script once to unlock existing unconfirmed email accounts.

update auth.users
   set email_confirmed_at = coalesce(email_confirmed_at, now()),
       updated_at = now()
 where email is not null
   and email_confirmed_at is null;

