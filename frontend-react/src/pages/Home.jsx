import { useState, useMemo } from 'react';
import CodeEditor from '../components/CodeEditor';
import ReviewPanel from '../components/ReviewPanel';
import DiffView from '../components/DiffView';

/* ── Analyze button ───────────────────────────────────────── */
function AnalyzeButton({ onClick, loading, disabled }) {
  return (
    <button
      onClick={onClick}
      disabled={disabled}
      className="w-full py-3 rounded-xl font-semibold text-sm active:scale-95"
      style={{
        transition: 'all 0.25s ease',
        background: disabled
          ? 'rgba(255,255,255,0.04)'
          : 'linear-gradient(135deg, #7f5af0, #2cb67d)',
        color: disabled ? '#374151' : '#fff',
        cursor: disabled ? 'not-allowed' : 'pointer',
        border: disabled ? '1px solid rgba(255,255,255,0.06)' : '1px solid transparent',
        boxShadow: disabled ? 'none' : '0 0 28px rgba(127,90,240,0.3), 0 0 56px rgba(44,182,125,0.1)',
        opacity: loading ? 0.85 : 1,
        transform: disabled ? 'none' : undefined,
      }}
    >
      {loading ? (
        <span className="flex items-center justify-center gap-2.5">
          <span
            className="w-4 h-4 rounded-full border-2 border-white/20 border-t-white"
            style={{ animation: 'spin 0.65s linear infinite' }}
          />
          Analyzing with AI...
        </span>
      ) : (
        'Analyze Code →'
      )}
    </button>
  );
}

/* ── Page ─────────────────────────────────────────────────── */
export default function Home() {
  const [code, setCode] = useState('');
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  // Extract issue line numbers for editor highlighting
  const issueLines = useMemo(
    () => (result?.issues ?? []).map((i) => i.line).filter(Boolean),
    [result]
  );

  const analyzeCode = async () => {
    if (!code.trim()) return;
    setLoading(true);
    setError(null);
    setResult(null);

    try {
      const res = await fetch('http://localhost:8080/api/review', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ code }),
      });

      if (!res.ok) {
        const err = await res.json().catch(() => ({}));
        throw new Error(err.error || `Server error ${res.status}`);
      }

      setResult(await res.json());
    } catch (e) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ padding: '20px 24px 28px', minHeight: 'calc(100vh - 57px)' }}>

      {/* ── Top zone: Editor (wider) | Review panel ─────── */}
      <div
        className="grid gap-5"
        style={{ gridTemplateColumns: '1.2fr 0.8fr', marginBottom: '20px' }}
      >
        {/* Left: editor + error + button */}
        <div className="flex flex-col gap-3">
          <CodeEditor code={code} setCode={setCode} issueLines={issueLines} />

          {error && (
            <div
              className="px-4 py-3 rounded-xl text-sm fade-in"
              style={{
                background: 'rgba(248,113,113,0.08)',
                border: '1px solid rgba(248,113,113,0.2)',
                color: '#f87171',
              }}
            >
              ⚠️ {error}
            </div>
          )}

          <AnalyzeButton
            onClick={analyzeCode}
            loading={loading}
            disabled={loading || !code.trim()}
          />
        </div>

        {/* Right: review panel */}
        <ReviewPanel data={result} loading={loading} />
      </div>

      {/* ── Bottom zone: Diff view ───────────────────────── */}
      {(result?.improvedCode || loading) && (
        <div className="fade-in">
          {loading ? (
            <div
              className="shimmer rounded-2xl"
              style={{ height: '100px', border: '1px solid rgba(255,255,255,0.04)' }}
            />
          ) : (
            <DiffView oldCode={code} newCode={result.improvedCode} />
          )}
        </div>
      )}
    </div>
  );
}
