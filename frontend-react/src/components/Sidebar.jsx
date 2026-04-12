import { Link } from 'react-router-dom';

const NAV = [
  { id: 'editor', label: 'Editor', icon: '⌨' },
  { id: 'repo-scan', label: 'Repo Scan', icon: '🔍' },
  { id: 'github', label: 'GitHub', icon: '🐙' },
  { id: 'history', label: 'History', icon: '🕐' },
];

const BOTTOM = [
  { id: 'docs', label: 'Docs', icon: '📖' },
  { id: 'settings', label: 'Settings', icon: '⚙' },
];

export default function Sidebar({ active, setActive }) {
  return (
    <aside style={{
      width: 200,
      height: '100vh',
      background: '#0b0b0f',
      borderRight: '1px solid rgba(255,255,255,0.05)',
      display: 'flex',
      flexDirection: 'column',
      flexShrink: 0,
      userSelect: 'none',
    }}>
      {/* Brand */}
      <Link to="/" style={{
        textDecoration: 'none',
        display: 'flex', alignItems: 'center', gap: 8,
        padding: '14px 14px 10px',
        borderBottom: '1px solid rgba(255,255,255,0.04)',
      }}>
        <div style={{
          width: 22, height: 22, borderRadius: 5, background: '#7f5af0',
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          fontSize: 10, fontWeight: 700, color: '#fff', flexShrink: 0,
        }}>A</div>
        <span style={{ fontSize: 12, fontWeight: 600, color: '#e2e8f0', letterSpacing: '0.01em' }}>
          AESTHENIXAI
        </span>
      </Link>

      {/* Main nav */}
      <nav style={{ flex: 1, padding: '8px 6px', display: 'flex', flexDirection: 'column', gap: 1 }}>
        {NAV.map((item) => {
          const isActive = active === item.id;
          return (
            <button key={item.id} onClick={() => setActive(item.id)} style={{
              display: 'flex', alignItems: 'center', gap: 8,
              padding: '6px 10px', borderRadius: 6, width: '100%',
              border: 'none', cursor: 'pointer', textAlign: 'left',
              fontSize: 13, fontWeight: isActive ? 500 : 400,
              background: isActive ? 'rgba(127,90,240,0.12)' : 'transparent',
              color: isActive ? '#c4b5fd' : '#6b7280',
              transition: 'background 150ms ease, color 150ms ease',
            }}
              onMouseEnter={(e) => { if (!isActive) { e.currentTarget.style.background = 'rgba(255,255,255,0.04)'; e.currentTarget.style.color = '#9ca3af'; } }}
              onMouseLeave={(e) => { if (!isActive) { e.currentTarget.style.background = 'transparent'; e.currentTarget.style.color = '#6b7280'; } }}
            >
              <span style={{ fontSize: 13, width: 16, textAlign: 'center', flexShrink: 0 }}>{item.icon}</span>
              {item.label}
              {isActive && (
                <span style={{ marginLeft: 'auto', width: 4, height: 4, borderRadius: '50%', background: '#7f5af0', flexShrink: 0 }} />
              )}
            </button>
          );
        })}
      </nav>

      {/* Bottom links */}
      <div style={{ padding: '6px 6px 10px', borderTop: '1px solid rgba(255,255,255,0.04)', display: 'flex', flexDirection: 'column', gap: 1 }}>
        {BOTTOM.map((item) => (
          <button key={item.id} style={{
            display: 'flex', alignItems: 'center', gap: 8,
            padding: '5px 10px', borderRadius: 6, width: '100%',
            border: 'none', cursor: 'pointer', textAlign: 'left',
            fontSize: 12, background: 'transparent', color: '#4b5563',
            transition: 'background 150ms ease, color 150ms ease',
          }}
            onMouseEnter={(e) => { e.currentTarget.style.background = 'rgba(255,255,255,0.03)'; e.currentTarget.style.color = '#6b7280'; }}
            onMouseLeave={(e) => { e.currentTarget.style.background = 'transparent'; e.currentTarget.style.color = '#4b5563'; }}
          >
            <span style={{ fontSize: 12, width: 16, textAlign: 'center', flexShrink: 0 }}>{item.icon}</span>
            {item.label}
          </button>
        ))}
      </div>
    </aside>
  );
}
