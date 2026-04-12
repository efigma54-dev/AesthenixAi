import { useState } from 'react';
import { scanRepository, ApiError, friendlyMessage } from '../lib/api';

function RepoScanView() {
  const [repoUrl, setRepoUrl] = useState('');
  const [token, setToken] = useState('');
  const [scanResult, setScanResult] = useState(null);
  const [status, setStatus] = useState('idle'); // idle | loading | success | error
  const [error, setError] = useState(null);

  const handleScan = async () => {
    if (!repoUrl.trim()) return;

    setStatus('loading');
    setError(null);
    setScanResult(null);

    try {
      const result = await scanRepository(repoUrl.trim(), token.trim() || null);
      setScanResult(result);
      setStatus('success');
    } catch (err) {
      setStatus('error');
      setError(friendlyMessage(err));
    }
  };

  const getScoreColor = (score) => {
    if (score >= 80) return '#10b981';
    if (score >= 60) return '#f59e0b';
    return '#ef4444';
  };

  const getScoreLabel = (score) => {
    if (score >= 80) return 'Good';
    if (score >= 60) return 'Needs Work';
    return 'Poor';
  };

  return (
    <div style={{ padding: '24px', maxWidth: '1200px', margin: '0 auto' }}>
      <div style={{ marginBottom: '32px' }}>
        <h1 style={{ fontSize: '28px', fontWeight: 'bold', marginBottom: '8px', color: '#e5e7eb' }}>
          Repository Scanner
        </h1>
        <p style={{ color: '#9ca3af', fontSize: '16px' }}>
          Analyze entire GitHub repositories for code quality, issues, and suggestions
        </p>
      </div>

      {/* Input Form */}
      <div style={{
        background: 'var(--surface)',
        border: '1px solid var(--border)',
        borderRadius: '12px',
        padding: '24px',
        marginBottom: '24px'
      }}>
        <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
          <div>
            <label style={{ display: 'block', fontSize: '14px', fontWeight: '500', color: '#e5e7eb', marginBottom: '8px' }}>
              GitHub Repository URL
            </label>
            <input
              type="url"
              placeholder="https://github.com/owner/repository"
              value={repoUrl}
              onChange={(e) => setRepoUrl(e.target.value)}
              style={{
                width: '100%',
                padding: '12px 16px',
                border: '1px solid var(--border)',
                borderRadius: '8px',
                background: 'var(--bg)',
                color: '#e5e7eb',
                fontSize: '14px'
              }}
            />
          </div>

          <div>
            <label style={{ display: 'block', fontSize: '14px', fontWeight: '500', color: '#e5e7eb', marginBottom: '8px' }}>
              GitHub Token (Optional)
              <span style={{ fontSize: '12px', color: '#6b7280', marginLeft: '8px' }}>
                For private repos or higher rate limits
              </span>
            </label>
            <input
              type="password"
              placeholder="ghp_..."
              value={token}
              onChange={(e) => setToken(e.target.value)}
              style={{
                width: '100%',
                padding: '12px 16px',
                border: '1px solid var(--border)',
                borderRadius: '8px',
                background: 'var(--bg)',
                color: '#e5e7eb',
                fontSize: '14px',
                fontFamily: 'monospace'
              }}
            />
          </div>

          <button
            onClick={handleScan}
            disabled={status === 'loading' || !repoUrl.trim()}
            style={{
              padding: '12px 24px',
              borderRadius: '8px',
              border: 'none',
              background: status === 'loading' ? 'rgba(127, 90, 240, 0.5)' : '#7f5af0',
              color: '#fff',
              fontSize: '16px',
              fontWeight: '600',
              cursor: status === 'loading' || !repoUrl.trim() ? 'not-allowed' : 'pointer',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              gap: '8px',
              transition: 'all 150ms ease'
            }}
          >
            {status === 'loading' ? (
              <>
                <div style={{
                  width: '16px',
                  height: '16px',
                  border: '2px solid rgba(255,255,255,0.3)',
                  borderTopColor: '#fff',
                  borderRadius: '50%',
                  animation: 'spin 0.8s linear infinite'
                }} />
                Scanning Repository...
              </>
            ) : (
              '🔍 Scan Repository'
            )}
          </button>
        </div>
      </div>

      {/* Error Display */}
      {status === 'error' && error && (
        <div style={{
          background: 'rgba(239, 68, 68, 0.1)',
          border: '1px solid rgba(239, 68, 68, 0.3)',
          borderRadius: '8px',
          padding: '16px',
          marginBottom: '24px'
        }}>
          <div style={{ color: '#ef4444', fontSize: '14px' }}>
            ⚠ {error}
          </div>
        </div>
      )}

      {/* Results Display */}
      {status === 'success' && scanResult && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
          {/* Overall Summary */}
          <div style={{
            background: 'var(--surface)',
            border: '1px solid var(--border)',
            borderRadius: '12px',
            padding: '24px'
          }}>
            <h2 style={{ fontSize: '20px', fontWeight: 'bold', color: '#e5e7eb', marginBottom: '16px' }}>
              📊 Repository Analysis: {scanResult.repoName}
            </h2>

            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '16px' }}>
              <div style={{ textAlign: 'center' }}>
                <div style={{ fontSize: '32px', fontWeight: 'bold', color: getScoreColor(scanResult.overallScore) }}>
                  {scanResult.overallScore.toFixed(1)}
                </div>
                <div style={{ fontSize: '14px', color: '#9ca3af' }}>Overall Score</div>
                <div style={{
                  fontSize: '12px',
                  color: getScoreColor(scanResult.overallScore),
                  fontWeight: '500',
                  marginTop: '4px'
                }}>
                  {getScoreLabel(scanResult.overallScore)}
                </div>
              </div>

              <div style={{ textAlign: 'center' }}>
                <div style={{ fontSize: '32px', fontWeight: 'bold', color: '#7f5af0' }}>
                  {scanResult.analyzedFiles}
                </div>
                <div style={{ fontSize: '14px', color: '#9ca3af' }}>Files Analyzed</div>
                <div style={{ fontSize: '12px', color: '#6b7280', marginTop: '4px' }}>
                  of {scanResult.totalFiles} total
                </div>
              </div>

              <div style={{ textAlign: 'center' }}>
                <div style={{ fontSize: '32px', fontWeight: 'bold', color: '#ef4444' }}>
                  {scanResult.allIssues?.length || 0}
                </div>
                <div style={{ fontSize: '14px', color: '#9ca3af' }}>Issues Found</div>
              </div>

              <div style={{ textAlign: 'center' }}>
                <div style={{ fontSize: '32px', fontWeight: 'bold', color: '#10b981' }}>
                  {scanResult.allSuggestions?.length || 0}
                </div>
                <div style={{ fontSize: '14px', color: '#9ca3af' }}>Suggestions</div>
              </div>
            </div>
          </div>

          {/* File Results */}
          <div style={{
            background: 'var(--surface)',
            border: '1px solid var(--border)',
            borderRadius: '12px',
            padding: '24px'
          }}>
            <h3 style={{ fontSize: '18px', fontWeight: 'bold', color: '#e5e7eb', marginBottom: '16px' }}>
              📁 File Analysis Results
            </h3>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
              {scanResult.files?.map((file, index) => (
                <div key={index} style={{
                  padding: '16px',
                  border: '1px solid var(--border)',
                  borderRadius: '8px',
                  background: 'var(--bg)'
                }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
                    <div style={{ fontSize: '16px', fontWeight: '500', color: '#e5e7eb' }}>
                      {file.name}
                    </div>
                    <div style={{
                      fontSize: '18px',
                      fontWeight: 'bold',
                      color: getScoreColor(file.score)
                    }}>
                      {file.score.toFixed(1)}
                    </div>
                  </div>

                  <div style={{ fontSize: '12px', color: '#6b7280', marginBottom: '12px' }}>
                    {file.path}
                  </div>

                  {file.status === 'error' ? (
                    <div style={{ color: '#ef4444', fontSize: '14px' }}>
                      ❌ {file.errorMessage}
                    </div>
                  ) : (
                    <div style={{ display: 'flex', gap: '16px', fontSize: '12px' }}>
                      <span style={{ color: '#ef4444' }}>
                        ⚠ {file.issues?.length || 0} issues
                      </span>
                      <span style={{ color: '#10b981' }}>
                        💡 {file.suggestions?.length || 0} suggestions
                      </span>
                    </div>
                  )}
                </div>
              ))}
            </div>
          </div>

          {/* Issues Summary */}
          {scanResult.allIssues && scanResult.allIssues.length > 0 && (
            <div style={{
              background: 'var(--surface)',
              border: '1px solid var(--border)',
              borderRadius: '12px',
              padding: '24px'
            }}>
              <h3 style={{ fontSize: '18px', fontWeight: 'bold', color: '#e5e7eb', marginBottom: '16px' }}>
                ⚠ Top Issues
              </h3>

              <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                {scanResult.allIssues.slice(0, 10).map((issue, index) => (
                  <div key={index} style={{
                    padding: '12px',
                    border: '1px solid rgba(239, 68, 68, 0.2)',
                    borderRadius: '6px',
                    background: 'rgba(239, 68, 68, 0.05)'
                  }}>
                    <div style={{ fontSize: '14px', color: '#ef4444', fontWeight: '500' }}>
                      {issue.type}: {issue.message}
                    </div>
                    {issue.line && (
                      <div style={{ fontSize: '12px', color: '#6b7280', marginTop: '4px' }}>
                        Line {issue.line}
                      </div>
                    )}
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
}

export default RepoScanView;