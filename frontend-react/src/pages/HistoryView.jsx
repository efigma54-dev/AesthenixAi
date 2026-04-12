import { useState, useEffect } from 'react';
import { loadHistory, deleteReview, clearHistory } from '../lib/history';
import { hasSupabase } from '../lib/supabase';

function scoreColor(s) { return s >= 75 ? '#4ade80' : s >= 50 ? '#fbbf24' : '#f87171'; }

function timeAgo(iso) {
  const diff = Date.now() - new Date(iso).getTime();
  const m = Math.floor(diff / 60000);
  if (m < 1) return 'just now';
  if (m < 60) return `${m}m ago`;
  const h = Math.floor(m / 60);
  if (h < 24) return `${h}h ago`;
  return `${Math.floor(h / 24)}d ago`;
}

function HistoryRow({ item, onDelete, onRestore }) {
  const c = scoreColor(item.score);
  const id = item.id ?? item.created_at;

  return (
    <div className="card fade-in hover-row" style={{ padding: '10px 14px', cursor: 'default' }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 6 }}>
        {/* Left: filename + time */}
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, minWidth: 0 }}>
          <span style={{ fontSize: 12 }}>☕</span>
          <span style={{ fontSize: 12, fontWeight: 500, color: '#e2e8f0', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
            {item.filename ?? 'untitled.java'}
          </span>
          <span style={{ fontSize: 10, color: '#374151', flexShrink: 0 }}>{timeAgo(item.created_at)}</span>
        </div>

        {/* Right: score + actions */}
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, flexShrink: 0 }}>
          <span style={{ fontSize: 12, fontWeight: 700, color: c }}>{item.score}</span>
          <span style={{ fontSize: 10, color: '#374151' }}>{item.issue_count ?? 0} issues</span>
          <button onClick={() => onRestore(item)} style={{
            fontSize: 10, padding: '2px 7px', borderRadius: 4, cursor: 'pointer',
            background: 'rgba(127,90,240,0.08)', color: '#a78bfa',
            border: '1px solid rgba(127,90,240,0.2)',
          }}>Open</button>
          <button onClick={() => onDelete(id)} style={{
            fontSize: 10, padding: '2px 7px', borderRadius: 4, cursor: 'pointer',
            background: 'transparent', color: '#374151',
            border: '1px solid rgba(255,255,255,0.06)',
            transition: 'color 150ms ease',
          }}
            onMouseEnter={(e) => (e.currentTarget.style.color = '#f87171')}
            onMouseLeave={(e) => (e.currentTarget.style.color = '#374151')}
          >✕</button>
        </div>
      </div>

      {/* Code preview */}
      <pre style={{
        fontSize: 11, color: '#4b5563', fontFamily: 'monospace',
        overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap',
        margin: 0, lineHeight: 1.4,
      }}>
        {(item.code ?? '').split('\n').slice(0, 2).join(' ↵ ')}
      </pre>
    </div>
  );
}

export default function HistoryView({ onRestoreToEditor }) {
  const [history, setHistory] = useState([]);
  const [loading, setLoading] = useState(true);
  const [restored, setRestored] = useState(null);

  useEffect(() => {
    loadHistory().then((data) => { setHistory(data); setLoading(false); });
  }, []);

  const handleDelete = async (id) => {
    await deleteReview(id);
    setHistory((prev) => prev.filter((r) => (r.id ?? r.created_at) !== id));
  };

  const handleClear = async () => {
    await clearHistory();
    setHistory([]);
  };

  const handleRestore = (item) => {
    setRestored(item.filename);
    onRestoreToEditor?.(item);
    setTimeout(() => setRestored(null), 2000);
  };

  if (loading) return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
      {[1, 2, 3].map((i) => <div key={i} className="shimmer" style={{ height: 72, borderRadius: 10 }} />)}
    </div>
  );

  return (
    <div style={{ maxWidth: 720 }}>
      {/* Header */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 16 }}>
        <div>
          <h2 style={{ fontSize: 14, fontWeight: 600, color: '#e2e8f0' }}>Analysis History</h2>
          <p style={{ fontSize: 11, color: '#4b5563', marginTop: 2 }}>
            {hasSupabase ? 'Synced via Supabase' : 'Stored in localStorage'} · {history.length} session{history.length !== 1 ? 's' : ''}
          </p>
        </div>
        {history.length > 0 && (
          <button onClick={handleClear} style={{
            fontSize: 11, padding: '4px 10px', borderRadius: 6, cursor: 'pointer',
            background: 'transparent', color: '#374151',
            border: '1px solid rgba(255,255,255,0.06)',
            transition: 'color 150ms ease',
          }}
            onMouseEnter={(e) => (e.currentTarget.style.color = '#f87171')}
            onMouseLeave={(e) => (e.currentTarget.style.color = '#374151')}
          >Clear all</button>
        )}
      </div>

      {/* Restored toast */}
      {restored && (
        <div className="fade-in" style={{
          marginBottom: 12, padding: '7px 12px', borderRadius: 7, fontSize: 12,
          color: '#4ade80', background: 'rgba(74,222,128,0.07)',
          border: '1px solid rgba(74,222,128,0.15)',
        }}>
          ✓ Opened {restored} in editor
        </div>
      )}

      {/* Empty state */}
      {history.length === 0 ? (
        <div style={{
          display: 'flex', flexDirection: 'column', alignItems: 'center',
          justifyContent: 'center', height: '50vh', gap: 8, textAlign: 'center',
        }}>
          <div style={{ fontSize: 32, opacity: 0.15 }}>🕐</div>
          <p style={{ fontSize: 13, color: '#6b7280' }}>No history yet</p>
          <p style={{ fontSize: 11, color: '#374151' }}>Run an analysis to see it here</p>
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
          {history.map((item, i) => (
            <HistoryRow
              key={item.id ?? item.created_at ?? i}
              item={item}
              onDelete={handleDelete}
              onRestore={handleRestore}
            />
          ))}
        </div>
      )}
    </div>
  );
}
