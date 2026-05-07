import { Link } from 'react-router-dom';

// ── Design tokens (match global.css) ──────────────────────
const C = {
  bg: '#0b0b0f',
  surface: '#111116',
  border: 'rgba(255,255,255,0.06)',
  accent: '#7f5af0',
  green: '#2cb67d',
  text: '#e2e8f0',
  muted: '#6b7280',
  dim: '#374151',
};

const W = { maxWidth: 1080, margin: '0 auto', padding: '0 24px' };

// ── Feature grid data ──────────────────────────────────────
const FEATURES = [
  {
    icon: '⚡',
    title: 'Instant Rule Engine',
    value: 'Feedback in under 5 seconds',
    desc: 'JavaParser static analysis runs locally — no network round-trip, no waiting.',
  },
  {
    icon: '🤖',
    title: 'AI Review',
    value: 'Context-aware fixes',
    desc: 'Ollama-powered analysis explains why an issue matters and suggests a concrete fix.',
  },
  {
    icon: '🔀',
    title: 'GitHub PR Reviews',
    value: 'Automated pull request analysis',
    desc: 'Every PR gets a score, issue breakdown, and inline suggestions posted as comments.',
  },
  {
    icon: '📊',
    title: 'Quality Scores',
    value: 'Track code health over time',
    desc: 'Blended rule + AI score out of 100. See trends across files and repositories.',
  },
  {
    icon: '🌐',
    title: 'Multi-language',
    value: 'Java, JS, TS, Python',
    desc: 'One tool for your whole stack. Language is auto-detected from file extension.',
  },
  {
    icon: '🛡',
    title: 'Fail-safe Mode',
    value: 'Rule-only fallback when AI unavailable',
    desc: 'If AI is overloaded or offline, rule analysis continues without interruption.',
  },
];

// ── Nav ────────────────────────────────────────────────────
function LandingNav() {
  return (
    <nav style={{
      position: 'sticky', top: 0, zIndex: 50,
      display: 'flex', alignItems: 'center', justifyContent: 'space-between',
      padding: '0 24px', height: '48px',
      background: 'rgba(11,11,15,0.92)',
      borderBottom: `1px solid ${C.border}`,
      backdropFilter: 'blur(12px)',
    }}>
      {/* Logo */}
      <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
        <div style={{
          width: 26, height: 26, borderRadius: 7,
          background: 'linear-gradient(135deg, #7f5af0, #2cb67d)',
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          fontSize: 11, fontWeight: 700, color: '#fff',
        }}>A</div>
        <span style={{ fontSize: 13, fontWeight: 600, color: C.text, letterSpacing: '-0.01em' }}>
          AESTHENIX<span className="gradient-text">AI</span>
        </span>
      </div>

      {/* Nav links + CTAs */}
      <div style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
        {[
          { label: 'Features', href: '#features' },
          { label: 'How it works', href: '#how' },
          { label: 'GitHub', href: 'https://github.com', external: true },
        ].map(({ label, href, external }) => (
          <a key={label} href={href} target={external ? '_blank' : undefined} rel="noreferrer"
            style={{ padding: '4px 10px', fontSize: 13, color: C.muted, cursor: 'pointer', borderRadius: 6, textDecoration: 'none', transition: 'color 150ms ease' }}
            onMouseEnter={e => e.currentTarget.style.color = C.text}
            onMouseLeave={e => e.currentTarget.style.color = C.muted}
          >{label}</a>
        ))}
        <Link to="/app" className="btn-accent" style={{ marginLeft: 8, textDecoration: 'none', display: 'inline-block', padding: '6px 14px', fontSize: 12 }}>
          Open App →
        </Link>
      </div>
    </nav>
  );
}

// ── Hero ───────────────────────────────────────────────────
function Hero() {
  return (
    <section style={{ ...W, paddingTop: 80, paddingBottom: 72, textAlign: 'center' }}>
      {/* Badge */}
      <div className="fade-in" style={{
        display: 'inline-flex', alignItems: 'center', gap: 6,
        padding: '4px 12px', borderRadius: 20, marginBottom: 24,
        background: 'rgba(127,90,240,0.08)',
        border: '1px solid rgba(127,90,240,0.2)',
        fontSize: 11, color: '#a78bfa',
      }}>
        <span style={{ width: 5, height: 5, borderRadius: '50%', background: C.accent, display: 'inline-block', animation: 'pulse-dot 2s infinite' }} />
        Now in early access · Free to use
      </div>

      {/* Headline */}
      <h1 className="fade-in" style={{
        fontSize: 'clamp(2rem, 5vw, 3.4rem)', fontWeight: 700,
        lineHeight: 1.1, marginBottom: 20, animationDelay: '50ms',
        letterSpacing: '-0.02em',
      }}>
        Ship cleaner pull requests<br />
        <span className="gradient-text">with AI-powered review</span>
      </h1>

      <p className="fade-in" style={{
        fontSize: 15, color: C.muted, maxWidth: 520, margin: '0 auto 32px',
        lineHeight: 1.7, animationDelay: '100ms',
      }}>
        AESTHENIXAI reviews Java, JavaScript, TypeScript, and Python code
        directly in VS Code and GitHub PRs. Catch risky code before review
        with instant feedback in under 5 seconds.
      </p>

      {/* Two primary CTAs */}
      <div className="fade-in" style={{ display: 'flex', gap: 10, justifyContent: 'center', flexWrap: 'wrap', animationDelay: '150ms' }}>
        <a
          href="https://marketplace.visualstudio.com"
          target="_blank" rel="noreferrer"
          className="btn-accent"
          style={{ textDecoration: 'none', display: 'inline-flex', alignItems: 'center', gap: 7, padding: '10px 22px', fontSize: 13, fontWeight: 600 }}
        >
          <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor"><path d="M23.15 2.587L18.21.21a1.494 1.494 0 0 0-1.705.29l-9.46 8.63-4.12-3.128a.999.999 0 0 0-1.276.057L.327 7.261A1 1 0 0 0 .326 8.74L3.899 12 .326 15.26a1 1 0 0 0 .001 1.479L1.65 17.94a.999.999 0 0 0 1.276.057l4.12-3.128 9.46 8.63a1.492 1.492 0 0 0 1.704.29l4.942-2.377A1.5 1.5 0 0 0 24 19.88V4.12a1.5 1.5 0 0 0-.85-1.533zm-5.146 14.861L10.826 12l7.178-5.448v10.896z" /></svg>
          Install VS Code Extension
        </a>
        <a
          href="https://github.com/apps"
          target="_blank" rel="noreferrer"
          className="btn-ghost"
          style={{ textDecoration: 'none', display: 'inline-flex', alignItems: 'center', gap: 7, padding: '10px 22px', fontSize: 13, fontWeight: 600 }}
        >
          <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor"><path d="M12 0C5.37 0 0 5.37 0 12c0 5.31 3.435 9.795 8.205 11.385.6.105.825-.255.825-.57 0-.285-.015-1.23-.015-2.235-3.015.555-3.795-.735-4.035-1.41-.135-.345-.72-1.41-1.23-1.695-.42-.225-1.02-.78-.015-.795.945-.015 1.62.87 1.845 1.23 1.08 1.815 2.805 1.305 3.495.99.105-.78.42-1.305.765-1.605-2.67-.3-5.46-1.335-5.46-5.925 0-1.305.465-2.385 1.23-3.225-.12-.3-.54-1.53.12-3.18 0 0 1.005-.315 3.3 1.23.96-.27 1.98-.405 3-.405s2.04.135 3 .405c2.295-1.56 3.3-1.23 3.3-1.23.66 1.65.24 2.88.12 3.18.765.84 1.23 1.905 1.23 3.225 0 4.605-2.805 5.625-5.475 5.925.435.375.81 1.095.81 2.22 0 1.605-.015 2.895-.015 3.3 0 .315.225.69.825.57A12.02 12.02 0 0 0 24 12c0-6.63-5.37-12-12-12z" /></svg>
          Add GitHub App
        </a>
      </div>

      {/* Social proof hint */}
      <p className="fade-in" style={{ fontSize: 11, color: C.dim, marginTop: 20, animationDelay: '200ms' }}>
        Built for fast-moving dev teams · Free during early access · No signup required
      </p>
    </section>
  );
}

// ── PR Comment Showcase ────────────────────────────────────
// This is the growth surface — shows exactly what developers see in GitHub
function PRCommentShowcase() {
  return (
    <section style={{ ...W, paddingBottom: 72 }}>
      <div style={{ textAlign: 'center', marginBottom: 36 }}>
        <div style={{ fontSize: 11, color: C.accent, textTransform: 'uppercase', letterSpacing: '0.08em', fontWeight: 600, marginBottom: 8 }}>GitHub Integration</div>
        <h2 style={{ fontSize: 22, fontWeight: 600, color: C.text }}>Every PR gets an automatic review</h2>
        <p style={{ fontSize: 13, color: C.muted, marginTop: 8, maxWidth: 420, margin: '8px auto 0' }}>
          Install the GitHub App and every pull request receives a structured review comment — automatically.
        </p>
      </div>

      {/* Mock GitHub PR comment */}
      <div style={{
        maxWidth: 680, margin: '0 auto',
        borderRadius: 12, overflow: 'hidden',
        border: '1px solid rgba(127,90,240,0.25)',
        background: '#0d1117',
        boxShadow: '0 0 40px rgba(127,90,240,0.08)',
      }}>
        {/* GitHub comment header */}
        <div style={{
          display: 'flex', alignItems: 'center', gap: 10,
          padding: '10px 16px',
          background: 'rgba(255,255,255,0.02)',
          borderBottom: '1px solid rgba(255,255,255,0.06)',
        }}>
          <div style={{
            width: 28, height: 28, borderRadius: '50%',
            background: 'linear-gradient(135deg, #7f5af0, #2cb67d)',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            fontSize: 11, fontWeight: 700, color: '#fff', flexShrink: 0,
          }}>A</div>
          <div>
            <span style={{ fontSize: 12, fontWeight: 600, color: C.text }}>aesthenixai</span>
            <span style={{ fontSize: 11, color: C.muted }}> · bot · just now</span>
          </div>
          <div style={{ marginLeft: 'auto', fontSize: 10, padding: '2px 8px', borderRadius: 20, background: 'rgba(44,182,125,0.1)', border: '1px solid rgba(44,182,125,0.2)', color: C.green }}>
            GitHub App
          </div>
        </div>

        {/* Comment body */}
        <div style={{ padding: '16px 20px', fontFamily: 'monospace', fontSize: 12, lineHeight: 1.8 }}>
          <div style={{ fontSize: 15, fontWeight: 700, color: C.text, marginBottom: 4, fontFamily: 'Inter, sans-serif' }}>
            🤖 AESTHENIXAI Code Review
          </div>
          <div style={{ marginBottom: 12, fontFamily: 'Inter, sans-serif' }}>
            <span style={{ fontWeight: 600, color: C.text }}>Score: 82/100 🟡</span>
            <span style={{ color: C.muted }}> — Quality gate </span>
            <span style={{ color: '#4ade80' }}>passed ✅</span>
          </div>

          <div style={{ marginBottom: 12, fontFamily: 'Inter, sans-serif' }}>
            <div style={{ fontSize: 11, color: C.muted, marginBottom: 4, textTransform: 'uppercase', letterSpacing: '0.06em' }}>Issues</div>
            <div style={{ display: 'flex', gap: 16 }}>
              <span style={{ color: '#f87171' }}>🔴 2 Critical</span>
              <span style={{ color: '#fbbf24' }}>⚠️ 3 Warnings</span>
              <span style={{ color: '#60a5fa' }}>🔵 1 Suggestion</span>
            </div>
          </div>

          <div style={{ marginBottom: 16, fontFamily: 'Inter, sans-serif' }}>
            <div style={{ fontSize: 11, color: C.muted, marginBottom: 6, textTransform: 'uppercase', letterSpacing: '0.06em' }}>Top fixes</div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
              <div style={{ color: C.text }}>
                <span style={{ color: '#f87171' }}>🔴</span>
                <span style={{ color: '#a78bfa', marginLeft: 6 }}>Performance</span>
                <span style={{ color: '#6b7280', fontSize: 11 }}> (high confidence)</span>
                <span style={{ color: C.muted }}> — String concatenation in loop</span>
                <span style={{ color: C.green }}> → StringBuilder sb = new StringBuilder()</span>
              </div>
              <div style={{ color: C.text }}>
                <span style={{ color: '#f87171' }}>🔴</span>
                <span style={{ color: '#a78bfa', marginLeft: 6 }}>Security</span>
                <span style={{ color: '#6b7280', fontSize: 11 }}> (high confidence)</span>
                <span style={{ color: C.muted }}> — Missing input validation on line 47</span>
              </div>
            </div>
          </div>

          <div style={{ padding: '8px 12px', borderRadius: 6, background: 'rgba(127,90,240,0.06)', border: '1px solid rgba(127,90,240,0.15)', fontFamily: 'Inter, sans-serif', fontSize: 12 }}>
            📊 <a href="#" style={{ color: C.accent, textDecoration: 'none' }}>View full report →</a>
          </div>

          <div style={{ marginTop: 12, fontSize: 11, color: C.dim, fontFamily: 'Inter, sans-serif' }}>
            <em>Was this review helpful? React with 👍 or 👎</em>
          </div>
        </div>
      </div>

      {/* Viral loop callout */}
      <p style={{ textAlign: 'center', fontSize: 12, color: C.dim, marginTop: 20 }}>
        Every developer who reviews a PR sees this comment → your tool spreads organically
      </p>
    </section>
  );
}

// ── Live Demo Preview ──────────────────────────────────────
function DemoPreview() {
  return (
    <section style={{ ...W, paddingBottom: 72 }}>
      <div style={{ textAlign: 'center', marginBottom: 36 }}>
        <div style={{ fontSize: 11, color: C.green, textTransform: 'uppercase', letterSpacing: '0.08em', fontWeight: 600, marginBottom: 8 }}>VS Code Extension</div>
        <h2 style={{ fontSize: 22, fontWeight: 600, color: C.text }}>Results in under 5 seconds</h2>
        <p style={{ fontSize: 13, color: C.muted, marginTop: 8, maxWidth: 400, margin: '8px auto 0' }}>
          Open a file, click Run Analysis. Rule engine fires instantly, AI result updates automatically.
        </p>
      </div>

      <div style={{
        borderRadius: 12, overflow: 'hidden',
        border: '1px solid rgba(127,90,240,0.2)',
        background: '#0b0b0f',
        boxShadow: '0 0 40px rgba(127,90,240,0.06)',
      }}>
        {/* Browser chrome */}
        <div style={{
          display: 'flex', alignItems: 'center', gap: 8, padding: '8px 14px',
          background: 'rgba(0,0,0,0.4)', borderBottom: `1px solid ${C.border}`,
        }}>
          <div style={{ display: 'flex', gap: 5 }}>
            {['#ff5f57', '#febc2e', '#28c840'].map(c => (
              <span key={c} style={{ width: 10, height: 10, borderRadius: '50%', background: c, display: 'block' }} />
            ))}
          </div>
          <div style={{
            flex: 1, maxWidth: 220, margin: '0 auto', padding: '2px 10px',
            borderRadius: 5, background: 'rgba(255,255,255,0.04)',
            border: `1px solid ${C.border}`,
            fontSize: 11, color: C.dim, textAlign: 'center', fontFamily: 'monospace',
          }}>AESTHENIXAI — VS Code</div>
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: '1.2fr 0.8fr', gap: 0, minHeight: 260 }}>
          {/* Code editor mock */}
          <div style={{ borderRight: `1px solid ${C.border}`, background: '#0d1117' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 6, padding: '7px 12px', borderBottom: `1px solid ${C.border}`, background: 'rgba(0,0,0,0.3)' }}>
              <span style={{ fontSize: 11, color: C.muted, fontFamily: 'monospace' }}>UserService.java</span>
              <span style={{ marginLeft: 'auto', fontSize: 10, padding: '1px 6px', borderRadius: 3, background: 'rgba(248,113,113,0.12)', color: '#f87171', border: '1px solid rgba(248,113,113,0.2)' }}>3 issues</span>
            </div>
            <div style={{ padding: '12px 16px', fontFamily: 'monospace', fontSize: 11, lineHeight: 1.8 }}>
              <div><span style={{ color: '#60a5fa' }}>public class</span> <span style={{ color: '#fbbf24' }}>UserService</span> {'{'}</div>
              <div style={{ paddingLeft: 16 }}><span style={{ color: '#60a5fa' }}>public</span> <span style={{ color: '#4ade80' }}>String</span> buildReport() {'{'}</div>
              <div style={{ paddingLeft: 32, background: 'rgba(248,113,113,0.07)', borderLeft: '2px solid #f87171', marginLeft: -2, paddingLeft: 30 }}>
                <span style={{ color: '#d1d5db' }}>String result = </span>
                <span style={{ color: '#fb923c' }}>&quot;&quot;</span>;
                <span style={{ marginLeft: 8, fontSize: 10, padding: '1px 5px', borderRadius: 3, background: 'rgba(248,113,113,0.15)', color: '#f87171' }}>⚡ Performance</span>
              </div>
              <div style={{ paddingLeft: 32 }}>
                <span style={{ color: '#60a5fa' }}>for</span>
                <span style={{ color: '#d1d5db' }}>(User u : users) {'{'}</span>
              </div>
              <div style={{ paddingLeft: 48, background: 'rgba(248,113,113,0.05)', borderLeft: '2px solid rgba(248,113,113,0.4)', marginLeft: -2, paddingLeft: 46 }}>
                <span style={{ color: '#d1d5db' }}>result </span>
                <span style={{ color: '#f87171' }}>+=</span>
                <span style={{ color: '#d1d5db' }}> u.getName();</span>
              </div>
              <div style={{ paddingLeft: 32, color: C.dim }}>{'}'}</div>
              <div style={{ paddingLeft: 16, color: C.dim }}>{'}'}</div>
              <div style={{ color: C.dim }}>{'}'}</div>
            </div>
          </div>

          {/* Results panel mock */}
          <div style={{ padding: 12, display: 'flex', flexDirection: 'column', gap: 8, background: '#0b0b0f' }}>
            {/* Score */}
            <div className="card" style={{ padding: '10px 12px', display: 'flex', alignItems: 'center', gap: 10 }}>
              <div style={{ width: 38, height: 38, borderRadius: '50%', border: '2px solid #fbbf24', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 13, fontWeight: 700, color: '#fbbf24', flexShrink: 0 }}>74</div>
              <div>
                <div style={{ fontSize: 10, color: C.muted }}>Quality Score</div>
                <div style={{ fontSize: 15, fontWeight: 700, color: '#fbbf24' }}>74<span style={{ fontSize: 11, color: C.dim, fontWeight: 400 }}>/100</span></div>
                <div style={{ fontSize: 10, color: '#fbbf24' }}>Needs attention</div>
              </div>
            </div>

            {/* Issues */}
            <div className="card" style={{ padding: '10px 12px' }}>
              <div style={{ fontSize: 10, color: C.muted, marginBottom: 6, textTransform: 'uppercase', letterSpacing: '0.06em' }}>Issues Found</div>
              {[
                ['⚡', '#fbbf24', 'Performance', 'Use StringBuilder in loop'],
                ['🔒', '#f87171', 'Security', 'Validate user input'],
                ['📝', '#60a5fa', 'Style', 'Method exceeds 30 lines'],
              ].map(([icon, color, type, msg]) => (
                <div key={type} style={{ display: 'flex', gap: 6, padding: '3px 0', fontSize: 11, borderBottom: `1px solid ${C.border}` }}>
                  <span>{icon}</span>
                  <span style={{ color, fontWeight: 500 }}>{type}</span>
                  <span style={{ color: C.muted, fontSize: 10 }}>— {msg}</span>
                </div>
              ))}
            </div>

            {/* AI skip indicator */}
            <div style={{ fontSize: 10, color: C.green, padding: '6px 10px', borderRadius: 6, background: 'rgba(44,182,125,0.06)', border: '1px solid rgba(44,182,125,0.15)', textAlign: 'center' }}>
              ✓ AI analysis complete · 3.2s
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}

// ── Feature Grid ───────────────────────────────────────────
function Features() {
  return (
    <section id="features" style={{ ...W, paddingBottom: 72 }}>
      <div style={{ textAlign: 'center', marginBottom: 36 }}>
        <div style={{ fontSize: 11, color: C.accent, textTransform: 'uppercase', letterSpacing: '0.08em', fontWeight: 600, marginBottom: 8 }}>Features</div>
        <h2 style={{ fontSize: 22, fontWeight: 600, color: C.text }}>Everything you need to ship clean code</h2>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3,1fr)', gap: 8 }}>
        {FEATURES.map((f, i) => (
          <div key={f.title} className="feature-card card fade-in" style={{
            padding: '16px 18px', animationDelay: `${i * 50}ms`,
            transition: 'background 150ms ease, border-color 150ms ease',
          }}
            onMouseEnter={e => { e.currentTarget.style.background = 'rgba(127,90,240,0.05)'; e.currentTarget.style.borderColor = 'rgba(127,90,240,0.25)'; }}
            onMouseLeave={e => { e.currentTarget.style.background = '#111116'; e.currentTarget.style.borderColor = C.border; }}
          >
            <div style={{ fontSize: 20, marginBottom: 10 }}>{f.icon}</div>
            <div style={{ fontSize: 13, fontWeight: 600, color: C.text, marginBottom: 2 }}>{f.title}</div>
            <div style={{ fontSize: 11, color: C.green, marginBottom: 6, fontWeight: 500 }}>{f.value}</div>
            <p style={{ fontSize: 12, color: C.muted, lineHeight: 1.6, margin: 0 }}>{f.desc}</p>
          </div>
        ))}
      </div>
    </section>
  );
}

// ── How It Works ───────────────────────────────────────────
function HowItWorks() {
  const steps = [
    { n: '01', title: 'Install the extension', desc: 'One click from the VS Code Marketplace. No configuration required.' },
    { n: '02', title: 'Click Run Analysis', desc: 'Open any supported file and trigger analysis from the command palette or toolbar.' },
    { n: '03', title: 'Fix issues instantly', desc: 'See your score, highlighted issues with line numbers, and one-click suggested fixes.' },
  ];

  return (
    <section id="how" style={{ ...W, paddingBottom: 72 }}>
      <div style={{ textAlign: 'center', marginBottom: 36 }}>
        <div style={{ fontSize: 11, color: C.green, textTransform: 'uppercase', letterSpacing: '0.08em', fontWeight: 600, marginBottom: 8 }}>How It Works</div>
        <h2 style={{ fontSize: 22, fontWeight: 600, color: C.text }}>Up and running in 30 seconds</h2>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3,1fr)', gap: 8, position: 'relative' }}>
        {/* Connector line */}
        <div style={{
          position: 'absolute', top: 22, left: '18%', right: '18%', height: 1,
          background: 'linear-gradient(90deg, rgba(127,90,240,0.3), rgba(44,182,125,0.3))',
          zIndex: 0,
        }} />
        {steps.map((s, i) => (
          <div key={s.n} className="fade-in" style={{
            padding: '16px', position: 'relative', zIndex: 1,
            animationDelay: `${i * 60}ms`,
          }}>
            <div style={{
              width: 44, height: 44, borderRadius: 10, marginBottom: 12,
              background: 'linear-gradient(135deg, rgba(127,90,240,0.15), rgba(44,182,125,0.15))',
              border: '1px solid rgba(127,90,240,0.2)',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              fontSize: 14, fontWeight: 700,
              background: 'linear-gradient(135deg, #7f5af0, #2cb67d)',
              WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent',
            }}>{s.n}</div>
            <div style={{ fontSize: 13, fontWeight: 600, color: C.text, marginBottom: 6 }}>{s.title}</div>
            <p style={{ fontSize: 12, color: C.muted, lineHeight: 1.6, margin: 0 }}>{s.desc}</p>
          </div>
        ))}
      </div>
    </section>
  );
}

// ── Why AESTHENIXAI? ───────────────────────────────────────
function WhySection() {
  return (
    <section style={{ ...W, paddingBottom: 72 }}>
      <div style={{ textAlign: 'center', marginBottom: 36 }}>
        <div style={{ fontSize: 11, color: C.accent, textTransform: 'uppercase', letterSpacing: '0.08em', fontWeight: 600, marginBottom: 8 }}>Why AESTHENIXAI?</div>
        <h2 style={{ fontSize: 22, fontWeight: 600, color: C.text }}>Built for real pull requests</h2>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3,1fr)', gap: 12, maxWidth: 900, margin: '0 auto' }}>
        {[
          { title: 'Fast enough for daily use', desc: 'Rule engine responds in under 5 seconds. No waiting, no blocking your workflow.' },
          { title: 'Safe enough for teams', desc: 'Hybrid analysis combines reliable rules with AI insights. Never breaks your build.' },
          { title: 'Works where you work', desc: 'VS Code extension, GitHub App, and web interface. One tool, everywhere.' },
        ].map((item, i) => (
          <div key={item.title} className="card fade-in" style={{
            padding: '20px', textAlign: 'center',
            animationDelay: `${i * 50}ms`,
          }}>
            <div style={{ fontSize: 14, fontWeight: 600, color: C.text, marginBottom: 8 }}>{item.title}</div>
            <p style={{ fontSize: 12, color: C.muted, lineHeight: 1.6, margin: 0 }}>{item.desc}</p>
          </div>
        ))}
      </div>
    </section>
  );
}

// ── Early Testers ──────────────────────────────────────────
function Testimonials() {
  const quotes = [
    { text: 'Caught 3 real bugs in my API layer before code review.', author: 'Backend engineer' },
    { text: 'Way faster feedback than waiting for review.', author: 'Indie developer' },
    { text: 'The safe auto-fixes saved me 20 minutes of cleanup.', author: 'Full-stack developer' },
  ];

  return (
    <section style={{ ...W, paddingBottom: 72 }}>
      <div style={{ textAlign: 'center', marginBottom: 36 }}>
        <div style={{ fontSize: 11, color: C.green, textTransform: 'uppercase', letterSpacing: '0.08em', fontWeight: 600, marginBottom: 8 }}>Used by Early Testers</div>
        <h2 style={{ fontSize: 22, fontWeight: 600, color: C.text }}>Developers shipping cleaner code</h2>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3,1fr)', gap: 12, maxWidth: 900, margin: '0 auto' }}>
        {quotes.map((q, i) => (
          <div key={i} className="card fade-in" style={{
            padding: '18px 20px',
            animationDelay: `${i * 50}ms`,
            borderColor: 'rgba(127,90,240,0.15)',
          }}>
            <div style={{ fontSize: 13, color: C.text, lineHeight: 1.6, marginBottom: 12, fontStyle: 'italic' }}>
              "{q.text}"
            </div>
            <div style={{ fontSize: 11, color: C.muted }}>— {q.author}</div>
          </div>
        ))}
      </div>
    </section>
  );
}

// ── Final CTA ──────────────────────────────────────────────
function CTA() {
  return (
    <section style={{ ...W, paddingBottom: 88 }}>
      <div className="card" style={{
        padding: '40px 48px', textAlign: 'center',
        borderColor: 'rgba(127,90,240,0.2)',
        background: 'rgba(127,90,240,0.03)',
        position: 'relative', overflow: 'hidden',
      }}>
        {/* Glow */}
        <div style={{
          position: 'absolute', top: -60, left: '50%', transform: 'translateX(-50%)',
          width: 300, height: 200,
          background: 'radial-gradient(ellipse, rgba(127,90,240,0.12) 0%, transparent 70%)',
          pointerEvents: 'none',
        }} />

        <h2 style={{ fontSize: 22, fontWeight: 700, marginBottom: 10, letterSpacing: '-0.01em' }}>
          Start reviewing smarter PRs today
        </h2>
        <p style={{ fontSize: 13, color: C.muted, marginBottom: 28, maxWidth: 380, margin: '0 auto 28px' }}>
          Free during early access. No signup. Works in VS Code and GitHub out of the box.
        </p>

        <div style={{ display: 'flex', gap: 10, justifyContent: 'center', flexWrap: 'wrap' }}>
          <a
            href="https://marketplace.visualstudio.com"
            target="_blank" rel="noreferrer"
            className="btn-accent"
            style={{ textDecoration: 'none', display: 'inline-flex', alignItems: 'center', gap: 7, padding: '10px 24px', fontSize: 13, fontWeight: 600 }}
          >
            Install Extension
          </a>
          <a
            href="https://github.com/apps"
            target="_blank" rel="noreferrer"
            className="btn-ghost"
            style={{ textDecoration: 'none', display: 'inline-flex', alignItems: 'center', gap: 7, padding: '10px 24px', fontSize: 13, fontWeight: 600 }}
          >
            Connect GitHub
          </a>
        </div>
      </div>
    </section>
  );
}

// ── Footer ─────────────────────────────────────────────────
function Footer() {
  return (
    <footer style={{
      borderTop: `1px solid ${C.border}`,
      padding: '24px',
      display: 'flex', alignItems: 'center', justifyContent: 'space-between',
      flexWrap: 'wrap', gap: 12,
    }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
        <div style={{
          width: 20, height: 20, borderRadius: 5,
          background: 'linear-gradient(135deg, #7f5af0, #2cb67d)',
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          fontSize: 9, fontWeight: 700, color: '#fff',
        }}>A</div>
        <span style={{ fontSize: 12, color: C.dim }}>AESTHENIXAI</span>
      </div>
      <div style={{ fontSize: 11, color: C.dim }}>
        Built with Spring Boot · JavaParser · Ollama · React
      </div>
      <div style={{ display: 'flex', gap: 16 }}>
        {[
          { label: 'GitHub', href: 'https://github.com' },
          { label: 'Marketplace', href: 'https://marketplace.visualstudio.com' },
        ].map(({ label, href }) => (
          <a key={label} href={href} target="_blank" rel="noreferrer"
            style={{ fontSize: 11, color: C.dim, textDecoration: 'none', transition: 'color 150ms ease' }}
            onMouseEnter={e => e.currentTarget.style.color = C.muted}
            onMouseLeave={e => e.currentTarget.style.color = C.dim}
          >{label}</a>
        ))}
      </div>
    </footer>
  );
}

// ── Page ───────────────────────────────────────────────────
export default function Landing() {
  return (
    <div style={{ background: C.bg, minHeight: '100vh' }}>
      <LandingNav />
      <Hero />
      <PRCommentShowcase />
      <DemoPreview />
      <Features />
      <HowItWorks />
      <WhySection />
      <Testimonials />
      <CTA />
      <Footer />
    </div>
  );
}
