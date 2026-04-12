const TYPE = {
  Bug: { color: '#f87171', icon: '🐛' },
  Performance: { color: '#fbbf24', icon: '⚡' },
  Security: { color: '#f87171', icon: '🔒' },
  Style: { color: '#60a5fa', icon: '🎨' },
  Maintainability: { color: '#a78bfa', icon: '🔧' },
  General: { color: '#6b7280', icon: '·' },
};

function IssueRow({ issue, index, onClick }) {
  const t = TYPE[issue.type] || TYPE.General;
  const hasLine = Number.isInteger(issue.line) && issue.line > 0;

  return (
    <div
      className="fade-in hover-row"
      onClick={() => hasLine && onClick?.(issue.line)}
      role="listitem"
      aria-label={`${issue.type} issue${hasLine ? ` on line ${issue.line}` : ''}: ${issue.message}`}
      tabIndex={hasLine ? 0 : undefined}
      onKeyDown={(e) => e.key === 'Enter' && hasLine && onClick?.(issue.line)}
      style={{
        animationDelay: `${index * 40}ms`,
        display: 'flex', alignItems: 'flex-start', gap: 8,
        padding: '7px 8px',
        borderLeft: `2px solid ${t.color}`,
        cursor: hasLine ? 'pointer' : 'default',
      }}
      title={hasLine ? `Click to jump to line ${issue.line}` : undefined}
    >
      <span style={{ fontSize: 11, marginTop: 1, flexShrink: 0 }}>{t.icon}</span>
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginBottom: 2 }}>
          <span style={{ fontSize: 11, fontWeight: 600, color: t.color }}>{issue.type}</span>
          {hasLine && (
            <span style={{ fontSize: 10, color: '#4b5563', fontFamily: 'monospace' }}>
              :{issue.line}
            </span>
          )}
        </div>
        <p style={{ fontSize: 12, color: '#9ca3af', lineHeight: 1.5, margin: 0 }}>{issue.message}</p>
      </div>
      {/* Jump hint */}
      {hasLine && (
        <span style={{ fontSize: 9, color: '#374151', flexShrink: 0, marginTop: 2, opacity: 0.7 }}>↗</span>
      )}
    </div>
  );
}

export default function IssueList({ issues, onIssueClick }) {
  if (!issues?.length) return (
    <div className="card fade-in" style={{ padding: '12px 16px', textAlign: 'center' }}>
      <span style={{ fontSize: 12, color: '#4b5563' }}>✓ No issues found</span>
    </div>
  );

  return (
    <div className="card fade-in" style={{ padding: '10px 12px' }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 8 }}>
        <span style={{ fontSize: 11, fontWeight: 600, color: '#6b7280', textTransform: 'uppercase', letterSpacing: '0.06em' }}>Issues</span>
        <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
          <span style={{ fontSize: 9, color: '#374151' }}>click to jump</span>
          <span style={{ fontSize: 11, color: '#f87171', background: 'rgba(248,113,113,0.08)', border: '1px solid rgba(248,113,113,0.15)', padding: '1px 7px', borderRadius: 4 }}>
            {issues.length}
          </span>
        </div>
      </div>
      <div role="list" aria-label={`${issues.length} code issue${issues.length !== 1 ? 's' : ''}`} style={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
        {issues.map((issue, i) => (
          <IssueRow key={i} issue={issue} index={i} onClick={onIssueClick} />
        ))}
      </div>
    </div>
  );
}
