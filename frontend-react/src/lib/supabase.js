import { createClient } from '@supabase/supabase-js';

// Set these in a .env file:
//   VITE_SUPABASE_URL=https://your-project.supabase.co
//   VITE_SUPABASE_ANON_KEY=your-anon-key
//
// Until configured, history falls back to localStorage automatically.

const url = import.meta.env.VITE_SUPABASE_URL || '';
const key = import.meta.env.VITE_SUPABASE_ANON_KEY || '';

export const supabase = url && key ? createClient(url, key) : null;
export const hasSupabase = Boolean(supabase);
