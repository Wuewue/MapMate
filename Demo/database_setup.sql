-- Run this in your Supabase SQL Editor to set up the Real-time Engine!

-- 1. Create the locations table to store everyone's GPS coordinates
CREATE TABLE public.locations (
  user_id uuid references auth.users on delete cascade not null primary key,
  latitude double precision not null,
  longitude double precision not null,
  updated_at timestamp with time zone default timezone('utc'::text, now())
);

-- 2. Turn on Realtime for the locations table so the app can listen to it
alter publication supabase_realtime add table public.locations;

-- 3. Set up basic security rules (for prototype, allow everyone to read/write)
-- WARNING: In production, you would restrict this to only allow friends!
ALTER TABLE public.locations ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Allow users to update their own location" 
ON public.locations FOR ALL 
USING (true)
WITH CHECK (true);
