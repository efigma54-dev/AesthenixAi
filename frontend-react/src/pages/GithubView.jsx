import { useState } from 'react';
import GithubInput from '../components/GithubInput';
import GithubResults from '../components/GithubResults';
import { parseGithubUrl, fetchRepoMeta, collectJavaFiles, reviewMultipleFiles, friendlyMessage, ApiError } from '../lib/api';

export default function GithubView() {
  const [status, setStatus] = useState('idle');  // idle | loading | success | error
  const [errorInfo, setErrorInfo] = useState(null);
  const [repoMeta, setRepoMeta] = useState(null);
  const [results, setResults] = useState([]);
  const [progress, setProgress] = useState([]);      // streaming partial results

  const analyze = async (url) => {
    if (status === 'loading') return;
    setStatus('loading'); setErrorInfo(null);
    setRepoMeta(null); setResults([]); setProgress([]);

    try {
      const { owner, repo } = parseGithubUrl(url);

      // Fetch metadata + file list in parallel
      const [meta, files] = await Promise.all([
        fetchRepoMeta(owner, repo),
        collectJavaFiles(owner, repo, '', [], 20),
      ]);

      if (!files.length) throw new ApiError('validation', 'No .java files found in this repository.', false);
      setRepoMeta(meta);

      // Fetch all file contents in parallel, then review in parallel batches
      const withContent = await Promise.all(
        files.map(async (f) => {
          const content = await fetch(f.downloadUrl).then((r) => r.text());
          return { name: f.name, content };
        })
      );

      const final = await reviewMultipleFiles(withContent, {
        onProgress: (partial) => setProgress([...partial]),
      });

      setResults(final);
      setStatus('success');
    } catch (err) {
      setStatus('error');
      setErrorInfo({ message: friendlyMessage(err), retryable: err instanceof ApiError ? err.retryable : true });
    }
  };

  const retry = () => {
    // Re-trigger with the last URL — GithubInput holds its own state
    // so we just reset to idle and let the user click again
    setStatus('idle'); setErrorInfo(null);
  };

  return (
    <div style={{ display: 'grid', gridTemplateColumns: '1.15fr 0.85fr', gap: 12 }}>
      {/* Left */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
        <GithubInput onAnalyze={analyze} loading={status === 'loading'} />

        {status === 'error' && errorInfo && (
          <div className="fade-in" style={{
            padding: '8px 12px', borderRadius: 7, fontSize: 12, color: '#f87171',
            background: 'rgba(248,113,113,0.07)', border: '1px solid rgba(248,113,113,0.15)',
            display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 8,
          }}>
            <span>⚠ {errorInfo.message}</span>
            {errorInfo.retryable && (
              <button onClick={retry} style={{
                fontSize: 11, padding: '2px 8px', borderRadius: 4, cursor: 'pointer',
                background: 'rgba(248,113,113,0.12)', color: '#f87171',
                border: '1px solid rgba(248,113,113,0.25)', flexShrink: 0,
              }}>Retry</button>
            )}
          </div>
        )}

        {/* Live progress — shows as files complete */}
        {status === 'loading' && (
          <div className="card fade-in" style={{ padding: '10px 12px' }}>
            <div style={{ fontSize: 11, color: '#6b7280', marginBottom: 8 }}>
              Analyzing in parallel… {progress.length} / 20 files
            </div>
            {/* Progress bar */}
            <div style={{ height: 3, background: 'rgba(255,255,255,0.06)', borderRadius: 2, marginBottom: 8, overflow: 'hidden' }}>
              <div style={{
                height: '100%', borderRadius: 2,
                background: 'linear-gradient(90deg, #7f5af0, #2cb67d)',
                width: `${Math.min((progress.length / 20) * 100, 100)}%`,
                transition: 'width 300ms ease',
              }} />
            </div>
            {progress.slice(-5).map((r, i) => (
              <div key={i} className="fade-in" style={{
                display: 'flex', justifyContent: 'space-between', fontSize: 11, padding: '2px 0',
              }}>
                <span style={{ color: '#9ca3af', fontFamily: 'monospace', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', maxWidth: '75%' }}>
                  {r.error ? '✕' : '✓'} {r.name}
                </span>
                <span style={{ color: r.error ? '#f87171' : '#4ade80', fontWeight: 600, flexShrink: 0 }}>
                  {r.error ? 'failed' : r.score}
                </span>
              </div>
            ))}
            <div className="shimmer" style={{ height: 11, width: '55%', marginTop: 6 }} />
          </div>
        )}
      </div>

      {/* Right */}
      {status === 'success' && repoMeta && results.length > 0
        ? <GithubResults repo={repoMeta} results={results} />
        : (
          <div className="card" style={{
            display: 'flex', flexDirection: 'column', alignItems: 'center',
            justifyContent: 'center', textAlign: 'center', minHeight: 440, gap: 8,
          }}>
            <div style={{ fontSize: 28, opacity: 0.2 }}>🐙</div>
            <p style={{ fontSize: 13, color: '#6b7280' }}>
              {status === 'loading' ? 'Fetching repository…' : 'Paste a GitHub repo URL'}
            </p>
            <p style={{ fontSize: 11, color: '#374151' }}>
              {status === 'loading' ? 'Analyzing .java files in parallel' : 'Fetches .java files · AI reviews each one'}
            </p>
          </div>
        )
      }
    </div>
  );
}
