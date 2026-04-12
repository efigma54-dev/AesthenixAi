import { useEffect, useState } from 'react';

const scoreColor = (s) =>
  s >= 75 ? '#4ade80' : s >= 50 ? '#fbbf24' : '#f87171';
const scoreLabel = (s) =>
  s >= 75 ? 'Good' : s >= 50 ? 'Average' : 'Poor';

export default function ScoreCard({ score, parsedInfo }) {
  const [count, setCount] = useState(0);
  const hex = scoreColor(score);

  useEffect(() => {
    let frame;
    const start = performance.now();
    const tick = (now) => {
      const p = Math.min((now - start) / 700, 1);
      setCount(Math.round((1 - Math.pow(1 - p, 3)) * score));
      if (p < 1) frame = requestAnimationFrame(tick);
    };
    frame = requestAnimationFrame(tick);
    return () => cancelAnimationFrame(frame);
  }, [score]);

  const r = 28, circ = 2 * Math.PI * r;
  const offset = circ - (score / 100) * circ;

  const stats = [
    { label: 'Methods', value: parsedInfo?.methodCount ?? '—' },
    { label: 'Complexity', value: parsedInfo?.cyclomaticComplexity ?? '—' },
    { label: 'Loops', value: parsedInfo?.nestedLoopCount ?? '—' },
    { label: 'Long Fns', value: parsedInfo?.longMethodCount ?? '—' },
  ];

  return (
    <div className="card fade-in" style={{ padding: '14px 16px' }}>
      {/* Score row */}
      <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
        {/* Mini ring */}
        <svg width="64" height="64" viewBox="0 0 64 64" style={{ flexShrink: 0 }}>
          <circle cx="32" cy="32" r={r} fill="none" stroke="rgba(255,255,255,0.06)" strokeWidth="5" />
          <circle cx="32" cy="32" r={r} fill="none" stroke={hex} strokeWidth="5"
            strokeLinecap="round" strokeDasharray={circ} strokeDashoffset={offset}
            transform="rotate(-90 32 32)"
            style={{ transition: 'stroke-dashoffset 0.7s cubic-bezier(0.34,1.56,0.64,1)' }}
          />
          <text x="32" y="37" textAnchor="middle" fontSize="13" fontWeight="700" fill={hex}>{count}</text>
        </svg>

        <div>
          <div style={{ fontSize: 11, color: '#6b7280', marginBottom: 2 }}>Code Quality</div>
          <div style={{ fontSize: 20, fontWeight: 700, color: hex, lineHeight: 1 }}>
            {score}<span style={{ fontSize: 12, color: '#4b5563', fontWeight: 400 }}>/100</span>
          </div>
          <div style={{ fontSize: 11, color: hex, marginTop: 3 }}>{scoreLabel(score)}</div>
        </div>
      </div>

      {/* Stats row */}
      <div style={{
        display: 'grid', gridTemplateColumns: 'repeat(4,1fr)',
        gap: 8, marginTop: 12, paddingTop: 12,
        borderTop: '1px solid rgba(255,255,255,0.06)',
      }}>
        {stats.map((s) => (
          <div key={s.label} style={{ textAlign: 'center' }}>
            <div style={{ fontSize: 15, fontWeight: 700, color: '#7f5af0' }}>{s.value}</div>
            <div style={{ fontSize: 10, color: '#4b5563', marginTop: 2 }}>{s.label}</div>
          </div>
        ))}
      </div>
    </div>
  );
}
