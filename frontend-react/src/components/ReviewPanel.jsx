import ScoreCard from './ScoreCard';
import IssueList from './IssueList';
import SuggestionList from './SuggestionList';

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

export default function ReviewPanel({ data, status, step = 0, retryLabel = '', onIssueClick, streamingText = '' }) {
  if (status === 'loading') return <LoadingState step={step} retryLabel={retryLabel} streamingText={streamingText} />;
  if (!data || status === 'idle' || status === 'error') return <EmptyState />;

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 8, overflowY: 'auto', maxHeight: 'calc(100vh - 120px)' }}>
      {data.fromCache && (
        <div style={{ fontSize: 10, color: '#4b5563', textAlign: 'right', paddingRight: 2 }}>
          ⚡ Cached result
        </div>
      )}
      <ScoreCard score={data.score} parsedInfo={data.parsedInfo} />
      <IssueList issues={data.issues} onIssueClick={onIssueClick} />
      <SuggestionList suggestions={data.suggestions} />
    </div>
  );
}
