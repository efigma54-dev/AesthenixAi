import ScoreCard from './ScoreCard';
import IssueList from './IssueList';
import SuggestionList from './SuggestionList';
import { useState } from 'react';
import { applySafeFixes } from '../lib/api';

const STEPS = [
  'Checking server…',
  'Sending request…',
  'Analyzing code…',
  'Finalizing results…',
];

function SkeletonBlock({ h }) {
  return <div className="shimmer" style={{ height: h, borderRadius: 10 }} />;
}

function EmptyState() {
  return (
    <div className="card" style={{
      display: 'flex', flexDirection: 'column', alignItems: 'center',
      justifyContent: 'center', textAlign: 'center',
      minHeight: 480, padding: 32, gap: 8,
    }}>
      <div style={{ fontSize: 28, marginBottom: 4, opacity: 0.2 }}>⌥</div>
      <p style={{ fontSize: 13, color: '#6b7280' }}>Run analysis to see results</p>
      <p style={{ fontSize: 11, color: '#374151' }}>Score · Issues · Suggestions · Diff</p>
    </div>
  );
}

function LoadingState({ step, retryLabel, streamingText }) {
  const label = retryLabel || STEPS[step] || STEPS[0];
  const isRetrying = Boolean(retryLabel);
  const isStreaming = Boolean(streamingText);

  // Parse streaming text for structured display
  const parseStreamingContent = (text) => {
    if (!text) return null;

    const sections = {
      score: null,
      issues: [],
      suggestions: [],
      thinking: text
    };

    // Try to extract structured data from the streaming text
    try {
      // Look for JSON-like patterns in the text
      const jsonMatch = text.match(/\{[\s\S]*\}/);
      if (jsonMatch) {
        const parsed = JSON.parse(jsonMatch[0]);
        if (parsed.score) sections.score = parsed.score;
        if (parsed.issues) sections.issues = parsed.issues;
        if (parsed.suggestions) sections.suggestions = parsed.suggestions;
        // Remove the JSON part from thinking text
        sections.thinking = text.replace(jsonMatch[0], '').trim();
      }
    } catch (e) {
      // If JSON parsing fails, show as thinking
    }

    return sections;
  };

  const streamingData = parseStreamingContent(streamingText);

  return (
    <div
      role="status"
      aria-live="polite"
      aria-label={label}
      style={{ display: 'flex', flexDirection: 'column', gap: 8 }}
    >
      <div className="card" style={{ padding: '12px 14px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 10 }}>
          <span style={{
            width: 12, height: 12, borderRadius: '50%',
            border: `2px solid ${isRetrying ? 'rgba(251,191,36,0.3)' : 'rgba(127,90,240,0.3)'}`,
            borderTopColor: isRetrying ? '#fbbf24' : '#7f5af0',
            animation: 'spin 0.7s linear infinite',
            display: 'inline-block', flexShrink: 0,
          }} />
          <span style={{ fontSize: 12, color: isRetrying ? '#fbbf24' : '#9ca3af' }}>
            {isStreaming ? 'AI analyzing in real-time…' : label}
          </span>
        </div>
        {/* Step progress bar */}
        <div style={{ display: 'flex', gap: 6 }}>
          {STEPS.map((_, i) => (
            <div key={i} style={{
              flex: 1, height: 3, borderRadius: 2,
              background: i <= step ? (isRetrying ? '#fbbf24' : '#7f5af0') : 'rgba(255,255,255,0.06)',
              transition: 'background 300ms ease',
            }} />
          ))}
        </div>
      </div>

      {isStreaming && streamingData && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
          {/* Score section */}
          {streamingData.score && (
            <div className="card" style={{ padding: '12px 14px' }}>
              <div style={{ fontSize: 11, color: '#6b7280', marginBottom: 8, fontWeight: 500 }}>
                Score:
              </div>
              <div style={{ fontSize: 24, fontWeight: 'bold', color: streamingData.score >= 80 ? '#10b981' : streamingData.score >= 60 ? '#f59e0b' : '#ef4444' }}>
                {streamingData.score}/100
              </div>
            </div>
          )}

          {/* Issues section */}
          {streamingData.issues.length > 0 && (
            <div className="card" style={{ padding: '12px 14px' }}>
              <div style={{ fontSize: 11, color: '#6b7280', marginBottom: 8, fontWeight: 500 }}>
                Issues Found:
              </div>
              <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
                {streamingData.issues.map((issue, i) => (
                  <div key={i} style={{ fontSize: 12, color: '#ef4444', display: 'flex', alignItems: 'flex-start', gap: 6 }}>
                    <span>⚠</span>
                    <span>{issue.message || issue}</span>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Suggestions section */}
          {streamingData.suggestions.length > 0 && (
            <div className="card" style={{ padding: '12px 14px' }}>
              <div style={{ fontSize: 11, color: '#6b7280', marginBottom: 8, fontWeight: 500 }}>
                Suggestions:
              </div>
              <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
                {streamingData.suggestions.map((suggestion, i) => (
                  <div key={i} style={{ fontSize: 12, color: '#10b981', display: 'flex', alignItems: 'flex-start', gap: 6 }}>
                    <span>💡</span>
                    <span>{suggestion.message || suggestion}</span>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* AI Thinking section */}
          {streamingData.thinking && (
            <div className="card" style={{ padding: '12px 14px' }}>
              <div style={{ fontSize: 11, color: '#6b7280', marginBottom: 8, fontWeight: 500 }}>
                AI Analysis:
              </div>
              <div style={{
                whiteSpace: 'pre-wrap',
                wordWrap: 'break-word',
                fontFamily: 'monospace',
                fontSize: 12,
                color: '#e5e7eb',
                maxHeight: 200,
                overflowY: 'auto'
              }}>
                {streamingData.thinking}
                <span style={{ animation: 'blink 1s infinite' }}>▊</span>
              </div>
            </div>
          )}
        </div>
      )}

      {!isStreaming && (
        <>
          <SkeletonBlock h={90} />
          <SkeletonBlock h={150} />
          <SkeletonBlock h={90} />
        </>
      )}
    </div>
  );
}

export default function ReviewPanel({ data, status, step = 0, retryLabel = '', onIssueClick, streamingText = '', originalCode = '', onCodeFixed, analysisTime = null }) {
  const [isFixing, setIsFixing] = useState(false);
  const [fixResult, setFixResult] = useState(null);
  const [copyStatus, setCopyStatus] = useState('idle'); // idle | copying | copied

  const handleFixSafeIssues = async () => {
    if (!originalCode) return;

    setIsFixing(true);
    setFixResult(null);

    try {
      const result = await applySafeFixes(originalCode);
      setFixResult(result);

      // Notify parent component with fixed code
      if (onCodeFixed && result.fixedCode) {
        onCodeFixed(result.fixedCode, result.appliedFixes);
      }
    } catch (err) {
      console.error('Failed to apply safe fixes:', err);
      setFixResult({ error: err.message || 'Failed to apply fixes' });
    } finally {
      setIsFixing(false);
    }
  };

  const handleCopyPRSummary = async () => {
    if (!data) return;

    setCopyStatus('copying');

    const criticalIssues = data.issues?.filter(i => i.severity === 'CRITICAL' || i.severity === 'HIGH') || [];
    const warnings = data.issues?.filter(i => i.severity === 'MEDIUM' || i.severity === 'LOW') || [];

    const summary = `## Code Review Summary

**Score:** ${data.score}/100

**Issues Found:**
- ${criticalIssues.length} critical issue${criticalIssues.length !== 1 ? 's' : ''}
- ${warnings.length} warning${warnings.length !== 1 ? 's' : ''}

${criticalIssues.length > 0 ? `**Critical Issues:**
${criticalIssues.map(i => `- Line ${i.line}: ${i.message}`).join('\n')}
` : ''}
${data.suggestions && data.suggestions.length > 0 ? `**Suggested Fixes:**
${data.suggestions.slice(0, 3).map(s => `- ${s}`).join('\n')}
` : ''}
---
*Generated by AESTHENIXAI Code Review*`;

    try {
      await navigator.clipboard.writeText(summary);
      setCopyStatus('copied');
      setTimeout(() => setCopyStatus('idle'), 2000);
    } catch (err) {
      console.error('Failed to copy:', err);
      setCopyStatus('idle');
    }
  };

  if (status === 'loading') return <LoadingState step={step} retryLabel={retryLabel} streamingText={streamingText} />;
  if (!data || status === 'idle' || status === 'error') return <EmptyState />;

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 8, overflowY: 'auto', maxHeight: 'calc(100vh - 120px)' }}>
      {/* Performance label */}
      {analysisTime && (
        <div style={{ fontSize: 10, color: '#10b981', textAlign: 'right', paddingRight: 2, display: 'flex', alignItems: 'center', justifyContent: 'flex-end', gap: 4 }}>
          <span>⚡</span>
          <span>Analysis completed in {analysisTime}s</span>
        </div>
      )}

      {data.fromCache && (
        <div style={{ fontSize: 10, color: '#4b5563', textAlign: 'right', paddingRight: 2 }}>
          ⚡ Cached result
        </div>
      )}

      {/* Action buttons row */}
      <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
        {/* Fix Safe Issues Button - HIGHEST ROI FEATURE */}
        {originalCode && (
          <div className="card" style={{ flex: '1 1 300px', padding: '12px 14px', background: 'rgba(127, 90, 240, 0.05)', border: '1px solid rgba(127, 90, 240, 0.2)' }}>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 12 }}>
              <div style={{ flex: 1 }}>
                <div style={{ fontSize: 13, fontWeight: 600, color: '#e5e7eb', marginBottom: 4 }}>
                  🛡️ Safe Auto-Fixes
                </div>
                <div style={{ fontSize: 11, color: '#9ca3af' }}>
                  Apply safe improvements
                </div>
              </div>
              <button
                onClick={handleFixSafeIssues}
                disabled={isFixing}
                style={{
                  padding: '8px 16px',
                  background: isFixing ? '#4b5563' : '#7f5af0',
                  color: '#fff',
                  border: 'none',
                  borderRadius: 6,
                  fontSize: 12,
                  fontWeight: 600,
                  cursor: isFixing ? 'not-allowed' : 'pointer',
                  transition: 'all 200ms',
                  opacity: isFixing ? 0.6 : 1,
                }}
                onMouseEnter={(e) => !isFixing && (e.target.style.background = '#6943d6')}
                onMouseLeave={(e) => !isFixing && (e.target.style.background = '#7f5af0')}
              >
                {isFixing ? 'Fixing...' : 'Fix Now'}
              </button>
            </div>

            {/* Show fix results */}
            {fixResult && !fixResult.error && (
              <div style={{ marginTop: 12, paddingTop: 12, borderTop: '1px solid rgba(127, 90, 240, 0.2)' }}>
                <div style={{ fontSize: 12, color: '#10b981', marginBottom: 8, fontWeight: 600 }}>
                  ✅ Applied {fixResult.fixCount} safe fix{fixResult.fixCount !== 1 ? 'es' : ''}
                </div>
                <div style={{ display: 'flex', flexDirection: 'column', gap: 4, maxHeight: 120, overflowY: 'auto' }}>
                  {fixResult.appliedFixes?.map((fix, i) => (
                    <div key={i} style={{ fontSize: 11, color: '#9ca3af', display: 'flex', alignItems: 'flex-start', gap: 6 }}>
                      <span style={{ color: '#10b981', flexShrink: 0 }}>•</span>
                      <span>{fix.description}</span>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {fixResult?.error && (
              <div style={{ marginTop: 12, paddingTop: 12, borderTop: '1px solid rgba(239, 68, 68, 0.2)' }}>
                <div style={{ fontSize: 11, color: '#ef4444' }}>
                  ⚠️ {fixResult.error}
                </div>
              </div>
            )}
          </div>
        )}

        {/* Copy PR Summary Button */}
        <div className="card" style={{ flex: originalCode ? '0 1 200px' : '1 1 300px', padding: '12px 14px', background: 'rgba(16, 185, 129, 0.05)', border: '1px solid rgba(16, 185, 129, 0.2)' }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 12 }}>
            <div style={{ flex: 1 }}>
              <div style={{ fontSize: 13, fontWeight: 600, color: '#e5e7eb', marginBottom: 4 }}>
                📋 Share Results
              </div>
              <div style={{ fontSize: 11, color: '#9ca3af' }}>
                Copy for PR/Slack
              </div>
            </div>
            <button
              onClick={handleCopyPRSummary}
              disabled={copyStatus === 'copying'}
              style={{
                padding: '8px 16px',
                background: copyStatus === 'copied' ? '#10b981' : copyStatus === 'copying' ? '#4b5563' : '#059669',
                color: '#fff',
                border: 'none',
                borderRadius: 6,
                fontSize: 12,
                fontWeight: 600,
                cursor: copyStatus === 'copying' ? 'not-allowed' : 'pointer',
                transition: 'all 200ms',
              }}
              onMouseEnter={(e) => copyStatus === 'idle' && (e.target.style.background = '#047857')}
              onMouseLeave={(e) => copyStatus === 'idle' && (e.target.style.background = '#059669')}
            >
              {copyStatus === 'copied' ? '✓ Copied' : copyStatus === 'copying' ? 'Copying...' : 'Copy'}
            </button>
          </div>
        </div>
      </div>

      <ScoreCard score={data.score} parsedInfo={data.parsedInfo} />
      <IssueList issues={data.issues} onIssueClick={onIssueClick} />
      <SuggestionList suggestions={data.suggestions} />
    </div>
  );
}
