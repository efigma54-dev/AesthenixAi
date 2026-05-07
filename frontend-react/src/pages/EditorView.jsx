import { useState, useMemo, useEffect, useRef, useCallback, lazy, Suspense } from 'react';
import ReviewPanel from '../components/ReviewPanel';
import FileExplorer from '../components/FileExplorer';
import { reviewCode, reviewCodeAsync, checkHealth, friendlyMessage, ApiError } from '../lib/api';
import { saveReview } from '../lib/history';

// Lazy-load heavy components so they don't block initial render
const CodeEditor = lazy(() => import('../components/CodeEditor'));
const DiffView = lazy(() => import('../components/DiffView'));

function EditorFallback() {
  return (
    <div className="card shimmer" style={{ height: 520, borderRadius: 16 }} />
  );
}

// ── Status machine: idle → loading → success | error ──────
// status: 'idle' | 'loading' | 'success' | 'error'

const SAMPLE = {
  name: 'Example.java',
  content: `public class Example {

    // ⚠ Performance: String concatenation in loop
    public String buildMessage(String[] words) {
        String result = "";
        for (int i = 0; i < words.length; i++) {
            for (int j = 0; j < words[i].length(); j++) {
                result += words[i].charAt(j);
            }
            result += " ";
        }
        return result;
    }

    // ⚠ Security: No exception handling
    public int divide(int a, int b) {
        return a / b;
    }
}`,
};

/* ── Progress steps shown during loading ─────────────────── */
const STEPS = [
  'Checking server…',
  'Queued — waiting for worker…',
  'Running — AI analyzing…',
  'Finalizing results…',
];

// Map job status → step index
const STATUS_TO_STEP = { queued: 1, running: 2, done: 3 };

function RunButton({ onClick, onStop, status, disabled, isStreaming }) {
  const loading = status === 'loading';
  const label = loading ? 'Analyzing code…'
    : status === 'success' ? 'Re-run Analysis'
      : 'Run Analysis';

  if (isStreaming) {
    return (
      <button
        onClick={onStop}
        aria-label="Stop analysis"
        style={{
          width: '100%', padding: '8px', borderRadius: 7,
          fontSize: 13, fontWeight: 600,
          cursor: 'pointer',
          border: '1px solid rgba(239, 68, 68, 0.3)',
          background: 'rgba(239, 68, 68, 0.1)',
          color: '#ef4444',
          opacity: 1,
          transition: 'all 150ms ease',
        }}
      >
        <span style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8 }}>
          ⏹ Stop Analysis
        </span>
      </button>
    );
  }

  return (
    <button
      onClick={onClick}
      disabled={disabled}
      aria-label={label}
      aria-busy={loading}
      style={{
        width: '100%', padding: '8px', borderRadius: 7,
        fontSize: 13, fontWeight: 600,
        cursor: disabled ? 'not-allowed' : 'pointer',
        border: disabled ? '1px solid rgba(255,255,255,0.06)' : 'none',
        background: disabled ? 'rgba(255,255,255,0.04)' : '#7f5af0',
        color: disabled ? '#374151' : '#fff',
        opacity: loading ? 0.8 : 1,
        transition: 'all 150ms ease',
      }}
    >
      {loading
        ? <span style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8 }}>
          <span
            role="status"
            aria-label="Loading"
            style={{ width: 12, height: 12, borderRadius: '50%', border: '2px solid rgba(255,255,255,0.25)', borderTopColor: '#fff', animation: 'spin 0.65s linear infinite', display: 'inline-block' }}
          />
          Analyzing…
        </span>
        : status === 'success' ? 'Re-run Analysis →'
          : 'Run Analysis →'}
    </button>
  );
}

function ErrorBanner({ message, retryable, onRetry }) {
  return (
    <div
      role="alert"
      aria-live="assertive"
      className="fade-in"
      style={{
        padding: '8px 12px', borderRadius: 7, fontSize: 12,
        color: '#f87171', background: 'rgba(248,113,113,0.07)',
        border: '1px solid rgba(248,113,113,0.15)',
        display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 8,
      }}
    >
      <span>⚠ {message}</span>
      {retryable && (
        <button
          onClick={onRetry}
          aria-label="Retry analysis"
          style={{
            fontSize: 11, padding: '2px 8px', borderRadius: 4, cursor: 'pointer',
            background: 'rgba(248,113,113,0.12)', color: '#f87171',
            border: '1px solid rgba(248,113,113,0.25)', flexShrink: 0,
          }}
        >Retry</button>
      )}
    </div>
  );
}

export default function EditorView({ runTrigger = 0, clearTrigger = 0, restoreItem = null, onRestoreConsumed }) {
  const [files, setFiles] = useState([SAMPLE]);
  const [activeFile, setActiveFile] = useState(SAMPLE);
  const [status, setStatus] = useState('idle');   // idle | loading | success | error
  const [result, setResult] = useState(null);
  const [errorInfo, setErrorInfo] = useState(null);     // { message, retryable }
  const [step, setStep] = useState(0);        // loading step index
  const [retryLabel, setRetryLabel] = useState('');     // "Attempt 2 of 3…"
  const [scrollToLine, setScrollToLine] = useState(null); // line to scroll editor to
  const [analysisTime, setAnalysisTime] = useState(null); // time taken for analysis

  const prevRun = useRef(0);
  const prevClear = useRef(0);
  const abortRef = useRef(null);
  const stepTimer = useRef(null);

  const code = activeFile?.content ?? '';

  const issueLines = useMemo(
    () => (result?.issues ?? []).map((i) => i.line).filter((l) => Number.isInteger(l) && l > 0),
    [result]
  );

  // Restore from history
  useEffect(() => {
    if (!restoreItem) return;
    const entry = { name: restoreItem.filename ?? 'restored.java', content: restoreItem.code ?? '' };
    setFiles((prev) => {
      const exists = prev.find((f) => f.name === entry.name);
      return exists ? prev.map((f) => f.name === entry.name ? entry : f) : [...prev, entry];
    });
    setActiveFile(entry);
    setStatus('idle'); setResult(null); setErrorInfo(null);
    onRestoreConsumed?.();
  }, [restoreItem]);

  // Command palette triggers
  useEffect(() => {
    if (runTrigger > prevRun.current) { prevRun.current = runTrigger; run(); }
  }, [runTrigger]);

  useEffect(() => {
    if (clearTrigger > prevClear.current) {
      prevClear.current = clearTrigger;
      updateActiveContent('');
      setStatus('idle'); setResult(null); setErrorInfo(null);
    }
  }, [clearTrigger]);

  // Cleanup on unmount
  useEffect(() => {
    return () => {
      abortRef.current?.abort();
      clearInterval(stepTimer.current);
    };
  }, []);

  /* ── File management ──────────────────────────────────── */
  const updateActiveContent = (content) => {
    if (!activeFile) return;
    setFiles((prev) => prev.map((f) => f.name === activeFile.name ? { ...f, content } : f));
    setActiveFile((f) => ({ ...f, content }));
  };

  const handleAddFiles = (rawFiles) => {
    rawFiles.forEach((raw) => {
      const reader = new FileReader();
      reader.onload = (e) => {
        const entry = { name: raw.name, content: e.target.result };
        setFiles((prev) => prev.find((f) => f.name === raw.name) ? prev : [...prev, entry]);
        setActiveFile(entry);
      };
      reader.readAsText(raw);
    });
  };

  const handleRemoveFile = (file) => {
    const remaining = files.filter((f) => f.name !== file.name);
    setFiles(remaining);
    if (activeFile?.name === file.name) {
      setActiveFile(remaining[0] ?? null);
      setStatus('idle'); setResult(null);
    }
  };

  /* ── Analysis ─────────────────────────────────────────── */
  const run = useCallback(async () => {
    if (!code.trim()) return;

    abortRef.current?.abort();
    abortRef.current = new AbortController();

    setStatus('loading');
    setErrorInfo(null);
    setResult(null);
    setRetryLabel('');
    setStep(0);
    setAnalysisTime(null);

    const startTime = Date.now();

    // Cycle through progress steps while waiting
    let s = 0;
    stepTimer.current = setInterval(() => {
      s = Math.min(s + 1, STEPS.length - 1);
      setStep(s);
    }, 2500);

    try {
      // Health pre-check — fail fast if backend is down
      const healthy = await checkHealth();
      if (!healthy) {
        throw new ApiError(
          'network',
          `Backend not reachable at ${import.meta.env.VITE_API_URL || 'http://localhost:8082/api'}. Run: mvn spring-boot:run`,
          true
        );
      }

      setStep(1);

      // Use async job queue — UI responds instantly, polls for result
      const data = await reviewCodeAsync(code, {
        signal: abortRef.current.signal,
        onStatus: (status) => {
          const s = STATUS_TO_STEP[status];
          if (s != null) setStep(s);
        },
      });

      const elapsed = ((Date.now() - startTime) / 1000).toFixed(1);
      setAnalysisTime(elapsed);

      setResult(data);
      setStatus('success');
      saveReview({ code, score: data.score, issues: data.issues, filename: activeFile?.name ?? 'untitled.java' });
    } catch (err) {
      if (err.name === 'AbortError') return;
      setStatus('error');
      setErrorInfo({
        message: friendlyMessage(err),
        retryable: err instanceof ApiError ? err.retryable : true,
      });
    } finally {
      clearInterval(stepTimer.current);
      setRetryLabel('');
    }
  }, [code, activeFile]);

  const stopAnalysis = useCallback(() => {
    abortRef.current?.abort();
    setStatus('idle');
    setResult(null);
    setErrorInfo(null);
    clearInterval(stepTimer.current);
  }, []);

  /* ── Click issue → scroll editor to line ─────────────── */
  const handleIssueClick = (line) => {
    if (Number.isInteger(line) && line > 0) setScrollToLine(line);
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 12, height: '100%' }}>
      <div style={{ display: 'flex', gap: 0, flex: 1 }}>
        <FileExplorer
          files={files}
          activeFile={activeFile}
          onSelect={(f) => {
            // Cancel any in-flight request for the previous file
            abortRef.current?.abort();
            abortRef.current = null;
            setActiveFile(f);
            setStatus('idle');
            setResult(null);
            setErrorInfo(null);
            setRetryLabel('');
          }}
          onRemove={handleRemoveFile}
          onAddFiles={handleAddFiles}
        />

        <div style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: 8, padding: '0 0 0 12px', minWidth: 0 }}>
          <div style={{ display: 'grid', gridTemplateColumns: '1.15fr 0.85fr', gap: 12 }}>
            {/* Left: editor */}
            <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
              <Suspense fallback={<EditorFallback />}>
                <CodeEditor
                  code={code}
                  setCode={updateActiveContent}
                  issueLines={issueLines}
                  filename={activeFile?.name}
                  scrollToLine={scrollToLine}
                  onScrollConsumed={() => setScrollToLine(null)}
                />
              </Suspense>
              {status === 'error' && errorInfo && (
                <ErrorBanner message={errorInfo.message} retryable={errorInfo.retryable} onRetry={run} />
              )}
              <RunButton onClick={run} onStop={stopAnalysis} status={status} disabled={status === 'loading' || !code.trim()} isStreaming={false} />
            </div>

            {/* Right: results */}
            <ReviewPanel
              data={result}
              status={status}
              step={step}
              retryLabel={retryLabel}
              onIssueClick={handleIssueClick}
              originalCode={code}
              analysisTime={analysisTime}
              onCodeFixed={(fixedCode, appliedFixes) => {
                // Update the editor with fixed code
                updateActiveContent(fixedCode);
                // Optionally re-run analysis to show improvement
                setTimeout(() => run(), 500);
              }}
            />
          </div>

          {result?.improvedCode && (
            <div className="fade-in">
              <Suspense fallback={<div className="shimmer" style={{ height: 120, borderRadius: 10 }} />}>
                <DiffView oldCode={code} newCode={result.improvedCode} />
              </Suspense>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
