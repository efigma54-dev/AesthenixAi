import { Link } from 'react-router-dom';

const FEATURES = [
  { icon: '🤖', title: 'AI Code Review', desc: 'GPT-4o returns structured bugs, smells, and fixes.' },
  { icon: '⚡', title: 'Performance Detection', desc: 'Catches nested loops, string concat, and O(n²) patterns.' },
  { icon: '🔒', title: 'Security Analysis', desc: 'Flags missing exception handling and injection risks.' },
  { icon: '📊', title: 'Code Scoring', desc: 'Blended AI + static score out of 100.' },
  { icon: '🐙', title: 'GitHub Repo Scan', desc: 'Every .java file in a public repo reviewed automatically.' },
  { icon: '🔀', title: 'Diff Viewer', desc: 'Side-by-side before/after with word-level highlighting.' },
];

const STEPS = [
  { n: '01', title: 'Paste Code', desc: 'Drop your Java snippet or paste a GitHub URL.' },
  { n: '02', title: 'AI Analysis', desc: 'JavaParser + GPT-4o review logic and patterns.' },
  { n: '03', title: 'Fix Issues', desc: 'Score, highlighted lines, suggestions, improved code.' },
];

/* ── Shared section container ─────────────────────────────── */
const W = { maxWidth: 1080, margin: '0 auto', padding: '0 24px' };

/* ── Nav ──────────────────────────────────────────────────── */
function LandingNav() {
  return (
    <nav style={{
      position: 'sticky', top: 0, zIndex: 50,
      display: 'flex', alignItems: 'center', justifyContent: 'space-between',
      padding: '0 24px', height: '44px',
      background: 'rgba(11,11,15,0.92)',
      borderBottom: '1px solid rgba(255,255,255,0.06)',
      backdropFilter: 'blur(12px)',
    }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
        <div style={{
          width: 24, height: 24, borderRadius: 6, background: '#7f5af0',
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          fontSize: 10, fontWeight: 700, color: '#fff',
        }}>A</div>
        <span style={{ fontSize: 13, fontWeight: 600, color: '#e2e8f0' }}>
          AESTHENIX<span className="gradient-text">AI</span>
        </span>
      </div>
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

/* ── Hero ─────────────────────────────────────────────────── */
function Hero() {
  return (
    <section style={{ ...W, paddingTop: 72, paddingBottom: 64 }}>
      {/* Badge */}
      <div className="fade-in" style={{
        display: 'inline-flex', alignItems: 'center', gap: 6,
        padding: '3px 10px', borderRadius: 20, marginBottom: 20,
        background: 'rgba(127,90,240,0.08)',
        border: '1px solid rgba(127,90,240,0.2)',
        fontSize: 11, color: '#a78bfa',
      }}>
        <span style={{ width: 5, height: 5, borderRadius: '50%', background: '#7f5af0', display: 'inline-block', animation: 'pulse-dot 2s infinite' }} />
        GPT-4o + JavaParser
      </div>

      {/* Headline — left-aligned, tighter */}
      <h1 className="fade-in" style={{
        fontSize: 'clamp(1.9rem, 4vw, 3rem)', fontWeight: 600,
        lineHeight: 1.15, marginBottom: 16, animationDelay: '50ms',
        maxWidth: 560,
      }}>
        Analyze Your Code<br />
        <span className="gradient-text">Before It Breaks</span>
      </h1>

      <p className="fade-in" style={{
        fontSize: 14, color: '#6b7280', maxWidth: 440,
        lineHeight: 1.65, marginBottom: 24, animationDelay: '100ms',
      }}>
        AI-powered Java code review — performance insights, issue detection,
        scoring, and clean refactoring suggestions.
      </p>

      {/* CTAs */}
      <div className="fade-in" style={{ display: 'flex', gap: 8, animationDelay: '150ms' }}>
        <Link to="/app" className="btn-accent" style={{ textDecoration: 'none', display: 'inline-block', padding: '8px 20px', fontSize: 13 }}>
          Try Now →
        </Link>
        <a href="https://github.com" target="_blank" rel="noreferrer" className="btn-ghost" style={{ textDecoration: 'none', display: 'inline-block' }}>
          View GitHub
        </a>
      </div>

      {/* Capability pills */}
      <div className="fade-in" style={{ display: 'flex', gap: 6, marginTop: 28, flexWrap: 'wrap', animationDelay: '200ms' }}>
        {['🤖 GPT-4o', '⚡ Static Analysis', '🐙 GitHub Scanner', '🔀 Diff Viewer'].map((p) => (
          <span key={p} style={{
            fontSize: 11, padding: '3px 10px', borderRadius: 20, color: '#4b5563',
            background: 'rgba(255,255,255,0.03)', border: '1px solid rgba(255,255,255,0.06)',
          }}>{p}</span>
        ))}
      </div>
    </section>
  );
}

/* ── Demo preview ─────────────────────────────────────────── */
function DemoPreview() {
  return (
    <section style={{ ...W, paddingBottom: 64 }}>
      <div style={{
        borderRadius: 12, overflow: 'hidden',
        border: '1px solid rgba(127,90,240,0.2)',
        background: '#0b0b0f',
      }}>
        {/* Browser chrome */}
        <div style={{
          display: 'flex', alignItems: 'center', gap: 8, padding: '8px 14px',
          background: 'rgba(0,0,0,0.4)', borderBottom: '1px solid rgba(255,255,255,0.06)',
        }}>
          <div style={{ display: 'flex', gap: 5 }}>
            {['#ff5f57', '#febc2e', '#28c840'].map((c) => (
              <span key={c} style={{ width: 10, height: 10, borderRadius: '50%', background: c, display: 'block' }} />
            ))}
          </div>
          <div style={{
            flex: 1, maxWidth: 200, margin: '0 auto', padding: '2px 10px',
            borderRadius: 5, background: 'rgba(255,255,255,0.04)',
            border: '1px solid rgba(255,255,255,0.07)',
            fontSize: 11, color: '#4b5563', textAlign: 'center', fontFamily: 'monospace',
          }}>localhost:3000/app</div>
        </div>

        {/* Mock content */}
        <div style={{ display: 'grid', gridTemplateColumns: '1.15fr 0.85fr', gap: 12, padding: 16, minHeight: 240 }}>
          {/* Editor mock */}
          <div style={{ borderRadius: 8, overflow: 'hidden', background: '#0d1117', border: '1px solid rgba(255,255,255,0.06)' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 6, padding: '7px 12px', borderBottom: '1px solid rgba(255,255,255,0.06)', background: 'rgba(0,0,0,0.3)' }}>
              <div style={{ display: 'flex', gap: 4 }}>
                {['#ff5f57', '#febc2e', '#28c840'].map((c) => (
                  <span key={c} style={{ width: 9, height: 9, borderRadius: '50%', background: c, display: 'block' }} />
                ))}
              </div>
              <span style={{ fontSize: 11, color: '#4b5563', fontFamily: 'monospace' }}>Main.java</span>
            </div>
            <div style={{ padding: '10px 14px', fontFamily: 'monospace', fontSize: 11, lineHeight: 1.7 }}>
              <div><span style={{ color: '#60a5fa' }}>public class</span> <span style={{ color: '#fbbf24' }}>Example</span> {'{'}</div>
              <div style={{ paddingLeft: 16 }}><span style={{ color: '#60a5fa' }}>public</span> <span style={{ color: '#4ade80' }}>String</span> build() {'{'}</div>
              <div style={{ paddingLeft: 32, background: 'rgba(248,113,113,0.07)', borderLeft: '2px solid #f87171', marginLeft: -2, paddingLeft: 30 }}>
                <span style={{ color: '#d1d5db' }}>String result = </span><span style={{ color: '#fb923c' }}>&quot;&quot;</span>;
                <span style={{ marginLeft: 6, fontSize: 10, padding: '1px 5px', borderRadius: 3, background: 'rgba(248,113,113,0.15)', color: '#f87171' }}>⚠</span>
              </div>
              <div style={{ paddingLeft: 32, color: '#4b5563' }}>{'// nested loop...'}</div>
              <div style={{ paddingLeft: 16 }}>{'}'}</div>
              <div>{'}'}</div>
            </div>
          </div>

          {/* Results mock */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
            <div className="card" style={{ padding: '10px 12px', display: 'flex', alignItems: 'center', gap: 10 }}>
              <div style={{ width: 40, height: 40, borderRadius: '50%', border: '2px solid #4ade80', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 13, fontWeight: 700, color: '#4ade80', flexShrink: 0 }}>82</div>
              <div>
                <div style={{ fontSize: 10, color: '#6b7280' }}>Code Quality</div>
                <div style={{ fontSize: 16, fontWeight: 700, color: '#4ade80' }}>82<span style={{ fontSize: 11, color: '#4b5563', fontWeight: 400 }}>/100</span></div>
                <div style={{ fontSize: 10, color: '#4ade80' }}>Good</div>
              </div>
            </div>
            <div className="card" style={{ padding: '10px 12px' }}>
              <div style={{ fontSize: 10, color: '#6b7280', marginBottom: 6, textTransform: 'uppercase', letterSpacing: '0.06em' }}>Issues</div>
              {[['⚡', 'Performance', 'Use StringBuilder'], ['🔒', 'Security', 'Add exception handling']].map(([icon, type, msg]) => (
                <div key={type} style={{ display: 'flex', gap: 6, padding: '3px 0', fontSize: 11 }}>
                  <span>{icon}</span>
                  <span style={{ color: '#f87171' }}>{type}</span>
                  <span style={{ color: '#6b7280' }}>— {msg}</span>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}

/* ── Features ─────────────────────────────────────────────── */
function Features() {
  return (
    <section style={{ ...W, paddingBottom: 64 }}>
      <div style={{ marginBottom: 32 }}>
        <div style={{ fontSize: 11, color: '#7f5af0', textTransform: 'uppercase', letterSpacing: '0.08em', fontWeight: 600, marginBottom: 8 }}>Features</div>
        <h2 style={{ fontSize: 22, fontWeight: 600, color: '#e2e8f0' }}>Everything you need to ship clean code</h2>
      </div>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3,1fr)', gap: 8 }}>
        {FEATURES.map((f, i) => (
          <div key={f.title} className="feature-card card fade-in" style={{
            padding: '14px 16px', cursor: 'default',
            animationDelay: `${i * 50}ms`,
            transition: 'background 150ms ease, border-color 150ms ease',
          }}
            onMouseEnter={(e) => { e.currentTarget.style.background = 'rgba(255,255,255,0.03)'; e.currentTarget.style.borderColor = 'rgba(127,90,240,0.25)'; }}
            onMouseLeave={(e) => { e.currentTarget.style.background = '#111116'; e.currentTarget.style.borderColor = 'rgba(255,255,255,0.06)'; }}
          >
            <div style={{ fontSize: 18, marginBottom: 8 }}>{f.icon}</div>
            <div style={{ fontSize: 13, fontWeight: 600, color: '#e2e8f0', marginBottom: 4 }}>{f.title}</div>
            <p style={{ fontSize: 12, color: '#6b7280', lineHeight: 1.55, margin: 0 }}>{f.desc}</p>
            <p className="learn-more" style={{ fontSize: 11, color: '#2cb67d', marginTop: 8 }}>Learn more →</p>
          </div>
        ))}
      </div>
    </section>
  );
}

/* ── How it works ─────────────────────────────────────────── */
function HowItWorks() {
  return (
    <section style={{ ...W, paddingBottom: 64 }}>
      <div style={{ marginBottom: 32 }}>
        <div style={{ fontSize: 11, color: '#2cb67d', textTransform: 'uppercase', letterSpacing: '0.08em', fontWeight: 600, marginBottom: 8 }}>How It Works</div>
        <h2 style={{ fontSize: 22, fontWeight: 600, color: '#e2e8f0' }}>Three steps to cleaner code</h2>
      </div>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3,1fr)', gap: 8, position: 'relative' }}>
        {/* Connector */}
        <div style={{
          position: 'absolute', top: 20, left: '18%', right: '18%', height: 1,
          background: 'linear-gradient(90deg, rgba(127,90,240,0.3), rgba(44,182,125,0.3))',
          zIndex: 0,
        }} />
        {STEPS.map((s, i) => (
          <div key={s.n} className="fade-in" style={{
            display: 'flex', flexDirection: 'column', alignItems: 'flex-start', gap: 8,
            padding: '14px 16px', position: 'relative', zIndex: 1,
            animationDelay: `${i * 60}ms`,
          }}>
            <div style={{
              width: 40, height: 40, borderRadius: 8, background: '#0b0b0f',
              border: '1px solid rgba(127,90,240,0.25)',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              fontSize: 13, fontWeight: 700,
              background: 'linear-gradient(135deg, #7f5af0, #2cb67d)',
              WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent',
            }}>{s.n}</div>
            <div style={{ fontSize: 13, fontWeight: 600, color: '#e2e8f0' }}>{s.title}</div>
            <p style={{ fontSize: 12, color: '#6b7280', lineHeight: 1.55, margin: 0 }}>{s.desc}</p>
          </div>
        ))}
      </div>
    </section>
  );
}

/* ── CTA ──────────────────────────────────────────────────── */
function CTA() {
  return (
    <section style={{ ...W, paddingBottom: 80 }}>
      <div className="card" style={{
        padding: '32px 40px', textAlign: 'center',
        borderColor: 'rgba(127,90,240,0.2)',
        background: 'rgba(127,90,240,0.03)',
      }}>
        <h2 style={{ fontSize: 20, fontWeight: 600, marginBottom: 8 }}>Start Reviewing Smarter</h2>
        <p style={{ fontSize: 13, color: '#6b7280', marginBottom: 20 }}>No signup. Paste code and get AI-powered feedback instantly.</p>
        <Link to="/app" className="btn-accent" style={{ textDecoration: 'none', display: 'inline-block', padding: '9px 24px', fontSize: 13 }}>
          Launch App →
        </Link>
      </div>
    </section>
  );
}

/* ── Footer ───────────────────────────────────────────────── */
function Footer() {
  return (
    <footer style={{ borderTop: '1px solid rgba(255,255,255,0.05)', padding: '20px 24px', textAlign: 'center', fontSize: 11, color: '#374151' }}>
      Built with Spring Boot · JavaParser · GPT-4o · React · Tailwind
    </footer>
  );
}

export default function Landing() {
  return (
    <div style={{ background: '#0b0b0f' }}>
      <LandingNav />
      <Hero />
      <DemoPreview />
      <Features />
      <HowItWorks />
      <CTA />
      <Footer />
    </div>
  );
}
