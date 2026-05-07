import { useState, useEffect, useCallback } from 'react';

const BASE = import.meta.env.VITE_API_URL || 'http://localhost:8082/api';

/* ── Helpers ──────────────────────────────────────────────── */
function fmt(ms) {
  if (!ms) return '—';
  if (ms < 1000) return `${ms}ms`;
  return `${(ms / 1000).toFixed(1)}s`;
}
function pct(n) {
  if (n == null) return '—';
  return typeof n === 'string' ? n : `${n.toFixed(1)}%`;
}

/* ── Stat card ────────────────────────────────────────────── */
function Card({ label, value, sub, color = '#7f5af0', warn = false }) {
  return (
    <div style={{
      background: 'rgba(255,255,255,0.03)',
      border: `1px solid ${warn ? 'rgba(248,113,113,0.3)' : 'rgba(255,255,255,0.06)'}`,
      borderRadius: 12,
      padding: '16px 18px',
      display: 'flex',
      flexDirection: 'column',
      gap: 4,
    }}>
      <span style={{ fontSize: 11, color: '#6b7280', textTransform: 'uppercase', letterSpacing: '0.06em' }}>
        {label}
      </span>
      <span style={{ fontSize: 26, fontWeight: 700, color: warn ? '#f87171' : color, lineHeight: 1.1 }}>
        {value ?? '—'}
      </span>
      {sub && <span style={{ fontSize: 11, color: '#4b5563' }}>{sub}</span>}
    </div>
  );
}

/* ── Bar chart (horizontal) ───────────────────────────────── */
function BarChart({ data, title }) {
  if (!data || Object.keys(data).length === 0) return null;
  const max = Math.max(...Object.values(data), 1);
  return (
    <div style={{
      background: 'rgba(255,255,255,0.03)',
      border: '1px solid rgba(255,255,255,0.06)',
      borderRadius: 12,
      padding: '16px 18px',
    }}>
      <div style={{ fontSize: 12, color: '#9ca3af', marginBottom: 12, fontWeight: 500 }}>{title}</div>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
        {Object.entries(data).map(([key, val]) => (
          <div key={key} style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
            <span style={{ fontSize: 11, color: '#6b7280', width: 100, flexShrink: 0, textAlign: 'right' }}>
              {key}
            </span>
            <div style={{ flex: 1, height: 6, background: 'rgba(255,255,255,0.05)', borderRadius: 3, overflow: 'hidden' }}>
              <div style={{
                height: '100%',
                width: `${(val / max) * 100}%`,
                background: 'linear-gradient(90deg, #7f5af0, #2cb67d)',
                borderRadius: 3,
                transition: 'width 0.4s ease',
              }} />
            </div>
            <span style={{ fontSize: 11, color: '#9ca3af', width: 28, textAlign: 'right', flexShrink: 0 }}>
              {val}
            </span>
          </div>
        ))}
      </div>
    </div>
  );
}

/* ── Latency sparkline (last 100 runs as dots) ────────────── */
function LatencyBar({ p50, p90, p99 }) {
  const bars = [
    { label: 'p50', value: p50, color: '#2cb67d' },
    { label: 'p90', value: p90, color: '#f59e0b' },
    { label: 'p99', value: p99, color: '#f87171' },
  ];
  const max = Math.max(p99, 1);
  return (
    <div style={{
      background: 'rgba(255,255,255,0.03)',
      border: '1px solid rgba(255,255,255,0.06)',
      borderRadius: 12,
      padding: '16px 18px',
    }}>
      <div style={{ fontSize: 12, color: '#9ca3af', marginBottom: 12, fontWeight: 500 }}>
        Latency Percentiles (rolling last 100 PRs)
      </div>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
        {bars.map(({ label, value, color }) => (
          <div key={label} style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
            <span style={{ fontSize: 11, color: '#6b7280', width: 28, flexShrink: 0 }}>{label}</span>
            <div style={{ flex: 1, height: 8, background: 'rgba(255,255,255,0.05)', borderRadius: 4, overflow: 'hidden' }}>
              <div style={{
                height: '100%',
                width: `${(value / max) * 100}%`,
                background: color,
                borderRadius: 4,
                transition: 'width 0.4s ease',
              }} />
            </div>
            <span style={{ fontSize: 12, color, width: 52, textAlign: 'right', flexShrink: 0, fontWeight: 600 }}>
              {fmt(value)}
            </span>
          </div>
        ))}
      </div>
    </div>
  );
}

/* ── Pass/fail donut (CSS only) ───────────────────────────── */
function PassRateRing({ passed, failed, timeouts, total }) {
  const passRate = total > 0 ? (passed / total) * 100 : 0;
  const color = passRate >= 70 ? '#2cb67d' : passRate >= 40 ? '#f59e0b' : '#f87171';
  return (
    <div style={{
      background: 'rgba(255,255,255,0.03)',
      border: '1px solid rgba(255,255,255,0.06)',
      borderRadius: 12,
      padding: '16px 18px',
      display: 'flex',
      alignItems: 'center',
      gap: 20,
    }}>
      {/* Ring */}
      <div style={{ position: 'relative', width: 72, height: 72, flexShrink: 0 }}>
        <svg width="72" height="72" viewBox="0 0 72 72">
          <circle cx="36" cy="36" r="28" fill="none" stroke="rgba(255,255,255,0.06)" strokeWidth="8" />
          <circle
            cx="36" cy="36" r="28"
            fill="none"
            stroke={color}
            strokeWidth="8"
            strokeDasharray={`${2 * Math.PI * 28}`}
            strokeDashoffset={`${2 * Math.PI * 28 * (1 - passRate / 100)}`}
            strokeLinecap="round"
            transform="rotate(-90 36 36)"
            style={{ transition: 'stroke-dashoffset 0.5s ease' }}
          />
        </svg>
        <div style={{
          position: 'absolute', inset: 0,
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          fontSize: 13, fontWeight: 700, color,
        }}>
          {passRate.toFixed(0)}%
        </div>
      </div>
      {/* Legend */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
        <div style={{ fontSize: 12, color: '#9ca3af', fontWeight: 500 }}>Pass Rate</div>
        <div style={{ fontSize: 11, color: '#2cb67d' }}>✅ {passed} passed</div>
        <div style={{ fontSize: 11, color: '#f87171' }}>❌ {failed} failed</div>
        <div style={{ fontSize: 11, color: '#f59e0b' }}>⏱ {timeouts} timeout</div>
      </div>
    </div>
  );
}

/* ── Main page ────────────────────────────────────────────── */
export default function DashboardView() {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [lastRefresh, setLastRefresh] = useState(null);

  const load = useCallback(async () => {
    try {
      const res = await fetch(`${BASE}/dashboard`, { signal: AbortSignal.timeout(5000) });
      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      setData(await res.json());
      setLastRefresh(new Date());
      setError(null);
    } catch (e) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
    const id = setInterval(load, 15_000); // auto-refresh every 15s
    return () => clearInterval(id);
  }, [load]);

  if (loading) return (
    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: '60vh' }}>
      <div style={{ width: 20, height: 20, borderRadius: '50%', border: '2px solid rgba(127,90,240,0.3)', borderTopColor: '#7f5af0', animation: 'spin 0.7s linear infinite' }} />
    </div>
  );

  if (error) return (
    <div style={{ padding: 24 }}>
      <div style={{
        padding: '12px 16px', borderRadius: 10,
        background: 'rgba(248,113,113,0.08)', border: '1px solid rgba(248,113,113,0.2)',
        color: '#f87171', fontSize: 13,
      }}>
        ⚠️ Could not load dashboard: {error}
        <button onClick={load} style={{ marginLeft: 12, fontSize: 12, color: '#7f5af0', background: 'none', border: 'none', cursor: 'pointer', textDecoration: 'underline' }}>
          Retry
        </button>
      </div>
    </div>
  );

  const pr = data?.prBot ?? {};
  const lat = data?.latency ?? {};
  const api = data?.api ?? {};
  const causes = data?.failureCauses ?? {};
  const repos = data?.topRepos ?? {};

  // Alert conditions
  const timeoutWarn = parseFloat(pr.timeoutRate) > 20;
  const passWarn = parseFloat(pr.passRate) < 30 && pr.total > 5;

  return (
    <div style={{ padding: '4px 0 24px' }}>

      {/* Header */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 20 }}>
        <div>
          <h2 style={{ fontSize: 16, fontWeight: 600, color: '#e2e8f0', margin: 0 }}>
            📊 Bot Dashboard
          </h2>
          <p style={{ fontSize: 11, color: '#4b5563', margin: '2px 0 0' }}>
            Auto-refreshes every 15s
            {lastRefresh && ` · Last: ${lastRefresh.toLocaleTimeString()}`}
          </p>
        </div>
        <button onClick={load} style={{
          fontSize: 11, color: '#7f5af0', background: 'rgba(127,90,240,0.08)',
          border: '1px solid rgba(127,90,240,0.2)', borderRadius: 6,
          padding: '4px 10px', cursor: 'pointer',
        }}>
          ↻ Refresh
        </button>
      </div>

      {/* Alerts */}
      {(timeoutWarn || passWarn) && (
        <div style={{
          marginBottom: 16, padding: '10px 14px', borderRadius: 10,
          background: 'rgba(248,113,113,0.06)', border: '1px solid rgba(248,113,113,0.2)',
          fontSize: 12, color: '#f87171', display: 'flex', flexDirection: 'column', gap: 4,
        }}>
          {timeoutWarn && <div>⚠️ High timeout rate: {pr.timeoutRate} — check Ollama or increase ANALYSIS_TIMEOUT_SECONDS</div>}
          {passWarn && <div>⚠️ Low pass rate: {pr.passRate} — review SCORE_GATE or check AI quality</div>}
        </div>
      )}

      {/* PR Bot summary cards */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 10, marginBottom: 14 }}>
        <Card label="Total PRs Reviewed" value={pr.total ?? 0} color="#7f5af0" />
        <Card label="Pass Rate" value={pr.passRate} color="#2cb67d" warn={passWarn} />
        <Card label="Avg Latency" value={fmt(lat.avgMs)} sub="lifetime average" color="#f59e0b" />
        <Card label="Timeout Rate" value={pr.timeoutRate} color="#f87171" warn={timeoutWarn} />
      </div>

      {/* Pass rate ring + latency percentiles */}
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1.6fr', gap: 10, marginBottom: 14 }}>
        <PassRateRing
          passed={pr.passed ?? 0}
          failed={pr.failed ?? 0}
          timeouts={pr.timeouts ?? 0}
          total={pr.total ?? 0}
        />
        <LatencyBar p50={lat.p50Ms ?? 0} p90={lat.p90Ms ?? 0} p99={lat.p99Ms ?? 0} />
      </div>

      {/* Failure causes + top repos */}
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10, marginBottom: 14 }}>
        <BarChart data={causes} title="Failure Causes" />
        <BarChart data={repos} title="Top Repos by Review Count" />
      </div>

      {/* API stats */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 10 }}>
        <Card label="API Requests" value={api.totalRequests ?? 0} color="#7f5af0" />
        <Card label="Cache Hit Rate" value={api.cacheHitRate} color="#2cb67d" />
        <Card label="Avg API Latency" value={fmt(api.avgRequestMs)} color="#f59e0b" />
        <Card label="AI Error Rate" value={api.aiErrorRate} color="#f87171" warn={parseFloat(api.aiErrorRate) > 20} />
      </div>

    </div>
  );
}
