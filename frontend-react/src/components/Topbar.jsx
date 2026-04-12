import ServerStatus from './ServerStatus';

const TITLES = {
  editor: { label: 'Editor', sub: 'Paste Java code and run AI analysis' },
  github: { label: 'GitHub', sub: 'Scan any public repository' },
  history: { label: 'History', sub: 'Past analysis sessions' },
};

export default function Topbar({ active, onOpenPalette }) {
  const { label, sub } = TITLES[active] ?? TITLES.editor;

  return (
    <header style={{
      height: 44,
      display: 'flex', alignItems: 'center', justifyContent: 'space-between',
      padding: '0 16px',
      borderBottom: '1px solid rgba(255,255,255,0.05)',
      background: '#0b0b0f',
      flexShrink: 0,
    }}>
      {/* Left: breadcrumb */}
      <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
        <span style={{ fontSize: 13, fontWeight: 500, color: '#e2e8f0' }}>{label}</span>
        <span style={{ fontSize: 11, color: '#374151' }}>·</span>
        <span style={{ fontSize: 11, color: '#4b5563' }}>{sub}</span>
      </div>

      {/* Right: server status + kbd hint + links */}
      <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
        <ServerStatus />

        <button onClick={onOpenPalette} style={{
          display: 'flex', alignItems: 'center', gap: 6,
          padding: '3px 10px', borderRadius: 6, cursor: 'pointer',
          background: 'rgba(255,255,255,0.04)', border: '1px solid rgba(255,255,255,0.07)',
          fontSize: 11, color: '#4b5563',
          transition: 'background 150ms ease, color 150ms ease',
        }}
          onMouseEnter={(e) => { e.currentTarget.style.background = 'rgba(255,255,255,0.07)'; e.currentTarget.style.color = '#9ca3af'; }}
          onMouseLeave={(e) => { e.currentTarget.style.background = 'rgba(255,255,255,0.04)'; e.currentTarget.style.color = '#4b5563'; }}
        >
          <span>⌘K</span>
          <span style={{ color: '#374151' }}>palette</span>
        </button>

        {['Docs', 'GitHub'].map((item) => (
          <span key={item} style={{
            fontSize: 12, color: '#4b5563', cursor: 'pointer',
            transition: 'color 150ms ease',
          }}
            onMouseEnter={(e) => (e.currentTarget.style.color = '#9ca3af')}
            onMouseLeave={(e) => (e.currentTarget.style.color = '#4b5563')}
          >{item}</span>
        ))}
      </div>
    </header>
  );
}
