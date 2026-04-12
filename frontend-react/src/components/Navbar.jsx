import { Link } from 'react-router-dom';

export default function Navbar() {
  return (
    <nav style={{
      position: 'sticky', top: 0, zIndex: 50,
      display: 'flex', alignItems: 'center', justifyContent: 'space-between',
      padding: '0 24px', height: '44px',
      background: 'rgba(11,11,15,0.92)',
      borderBottom: '1px solid rgba(255,255,255,0.06)',
      backdropFilter: 'blur(12px)',
    }}>
      <Link to="/" style={{ textDecoration: 'none', display: 'flex', alignItems: 'center', gap: 8 }}>
        <div style={{
          width: 24, height: 24, borderRadius: 6,
          background: '#7f5af0',
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          fontSize: 10, fontWeight: 700, color: '#fff',
        }}>A</div>
        <span style={{ fontSize: 13, fontWeight: 600, color: '#e2e8f0', letterSpacing: '0.01em' }}>
          AESTHENIX<span className="gradient-text">AI</span>
        </span>
      </Link>

      <div style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
        {['Docs', 'GitHub'].map((item) => (
          <span key={item} style={{
            padding: '4px 10px', fontSize: 13, color: '#6b7280', cursor: 'pointer',
            borderRadius: 6, transition: 'color 150ms ease, background 150ms ease',
          }}
            onMouseEnter={(e) => { e.currentTarget.style.color = '#e2e8f0'; e.currentTarget.style.background = 'rgba(255,255,255,0.04)'; }}
            onMouseLeave={(e) => { e.currentTarget.style.color = '#6b7280'; e.currentTarget.style.background = 'transparent'; }}
          >{item}</span>
        ))}
        <Link to="/app" className="btn-accent" style={{ marginLeft: 8, textDecoration: 'none', display: 'inline-block' }}>
          Launch App →
        </Link>
      </div>
    </nav>
  );
}
