/**
 * History persistence layer.
 * Uses Supabase when configured, falls back to localStorage.
 */
import { supabase, hasSupabase } from './supabase';

const LS_KEY = 'aesthenix_history';
const MAX_LOCAL = 50;

/* ── Save ──────────────────────────────────────────────────── */
export async function saveReview({ code, score, issues = [], filename = 'untitled.java' }) {
  const entry = {
    filename,
    code: code.slice(0, 2000),   // cap stored code to 2KB
    score,
    issue_count: issues.length,
    created_at: new Date().toISOString(),
  };

  if (hasSupabase) {
    const { error } = await supabase.from('reviews').insert([{
      user_id: 'guest',
      ...entry,
    }]);
    if (error) console.warn('Supabase save failed, falling back to localStorage', error);
    else return;
  }

  // localStorage fallback
  const existing = loadLocal();
  const updated = [entry, ...existing].slice(0, MAX_LOCAL);
  localStorage.setItem(LS_KEY, JSON.stringify(updated));
}

/* ── Load ──────────────────────────────────────────────────── */
export async function loadHistory() {
  if (hasSupabase) {
    const { data, error } = await supabase
      .from('reviews')
      .select('*')
      .order('created_at', { ascending: false })
      .limit(50);
    if (!error && data) return data;
    console.warn('Supabase load failed, falling back to localStorage', error);
  }
  return loadLocal();
}

/* ── Delete ────────────────────────────────────────────────── */
export async function deleteReview(id) {
  if (hasSupabase && id) {
    await supabase.from('reviews').delete().eq('id', id);
    return;
  }
  // localStorage: id is the created_at string
  const existing = loadLocal().filter((r) => r.created_at !== id);
  localStorage.setItem(LS_KEY, JSON.stringify(existing));
}

/* ── Clear all ─────────────────────────────────────────────── */
export async function clearHistory() {
  if (hasSupabase) {
    await supabase.from('reviews').delete().eq('user_id', 'guest');
    return;
  }
  localStorage.removeItem(LS_KEY);
}

function loadLocal() {
  try { return JSON.parse(localStorage.getItem(LS_KEY) || '[]'); }
  catch { return []; }
}
