import { useState } from 'react';

const EXAMPLES = [
  'https://github.com/spring-projects/spring-petclinic',
  'https://github.com/iluwatar/java-design-patterns',
];

export default function GithubInput({ onAnalyze, loading }) {
  const [url, setUrl] = useState('');

  const handleSubmit = () => {
    const trimmed = url.trim();
    if (!trimmed) return;
    onAnalyze(trimmed);
  };

  const isDisabled = loading || !url.trim();

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
      {/* Gradient-border card */}
      <div style={{
        borderRadius: 16, padding: 1,
        background: 'linear-gradient(135deg, #7f5af0, #2cb67d)',
        boxShadow: '0 0 32px rgba(127,90,240,0.12)',
      }}>
        <div style={{
          background: '#0a0a0f', borderRadius: 15, padding: 20,
          display: 'flex', flexDirection: 'column', gap: 12,
        }}>
          {/* Header */}
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <span style={{ fontSize: 18 }}>🐙</span>
            <span style={{ fontSize: 14, fontWeight: 600, color: '#d1d5db' }}>GitHub Repository</span>
          </div>

          {/* URL input row */}
          <div
            style={{
              display: 'flex', alignItems: 'center', gap: 8,
              padding: '10px 14px', borderRadius: 12,
              background: 'rgba(0,0,0,0.4)',
              border: '1px solid rgba(255,255,255,0.08)',
              transition: 'border-color 0.2s ease',
            }}
            onFocusCapture={(e) => (e.currentTarget.style.borderColor = 'rgba(127,90,240,0.4)')}
            onBlurCapture={(e) => (e.currentTarget.style.borderColor = 'rgba(255,255,255,0.08)')}
          >
            <span style={{ fontSize: 12, color: '#4b5563', fontFamily: 'monospace', whiteSpace: 'nowrap' }}>
              github.com/
            </span>
            <input
              value={url.replace(/^https?:\/\/github\.com\//i, '')}
              onChange={(e) => setUrl('https://github.com/' + e.target.value)}
              onKeyDown={(e) => e.key === 'Enter' && handleSubmit()}
              placeholder="owner/repository"
              style={{
                flex: 1, background: 'transparent', border: 'none', outline: 'none',
                fontSize: 13, color: '#e2e8f0', fontFamily: 'monospace',
              }}
            />
          </div>

          {/* Example buttons */}
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6, alignItems: 'center' }}>
            <span style={{ fontSize: 11, color: '#4b5563' }}>Try:</span>
            {EXAMPLES.map((ex) => (
              <button
                key={ex}
                onClick={() => setUrl(ex)}
                style={{
                  fontSize: 11, padding: '2px 8px', borderRadius: 4, cursor: 'pointer',
                  background: 'rgba(127,90,240,0.08)', color: '#a78bfa',
                  border: '1px solid rgba(127,90,240,0.2)',
                }}
              >
                {ex.replace('https://github.com/', '')}
              </button>
            ))}
          </div>

          {/* Analyze button */}
          <button
            onClick={handleSubmit}
            disabled={isDisabled}
            style={{
              width: '100%', padding: '12px', borderRadius: 12,
              fontWeight: 600, fontSize: 14, cursor: isDisabled ? 'not-allowed' : 'pointer',
              border: isDisabled ? '1px solid rgba(255,255,255,0.06)' : 'none',
              background: isDisabled
                ? 'rgba(255,255,255,0.04)'
                : 'linear-gradient(135deg, #2cb67d, #7f5af0)',
              color: isDisabled ? '#374151' : '#fff',
              boxShadow: isDisabled ? 'none' : '0 0 28px rgba(44,182,125,0.25)',
              transition: 'all 0.25s ease',
            }}
          >
            {loading ? (
              <span style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 10 }}>
                <span style={{
                  width: 16, height: 16, borderRadius: '50%',
                  border: '2px solid rgba(255,255,255,0.2)',
                  borderTopColor: '#fff',
                  animation: 'spin 0.65s linear infinite',
                  display: 'inline-block',
                }} />
                Fetching & Analyzing...
              </span>
            ) : 'Analyze Repository →'}
          </button>
        </div>
      </div>

      {/* Info note */}
      <div style={{
        padding: '10px 14px', borderRadius: 12, fontSize: 12, color: '#6b7280',
        display: 'flex', gap: 8, alignItems: 'flex-start',
        background: 'rgba(255,255,255,0.02)', border: '1px solid rgba(255,255,255,0.05)',
      }}>
        <span style={{ flexShrink: 0 }}>ℹ️</span>
        <span>
          Fetches <code style={{ color: '#a78bfa' }}>.java</code> files from the repo, then runs AI + static analysis on each. Public repos only.
        </span>
      </div>
    </div>
  );
}
