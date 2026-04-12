import { useState } from 'react';
import ReactDiffViewer, { DiffMethod } from 'react-diff-viewer-continued';

const DARK_THEME = {
  variables: {
    dark: {
      diffViewerBackground: '#0a0a0f',
      diffViewerColor: '#e2e8f0',
      addedBackground: 'rgba(44,182,125,0.1)',
      addedColor: '#e2e8f0',
      removedBackground: 'rgba(248,113,113,0.1)',
      removedColor: '#e2e8f0',
      wordAddedBackground: 'rgba(44,182,125,0.25)',
      wordRemovedBackground: 'rgba(248,113,113,0.25)',
      addedGutterBackground: 'rgba(44,182,125,0.15)',
      removedGutterBackground: 'rgba(248,113,113,0.15)',
      gutterBackground: '#0f0f18',
      gutterBackgroundDark: '#0f0f18',
      highlightBackground: 'rgba(127,90,240,0.1)',
      highlightGutterBackground: 'rgba(127,90,240,0.15)',
      codeFoldBackground: '#111118',
      emptyLineBackground: '#0a0a0f',
      gutterColor: '#374151',
      addedGutterColor: '#4ade80',
      removedGutterColor: '#f87171',
      codeFoldContentColor: '#6b7280',
      diffViewerTitleBackground: '#0f0f18',
      diffViewerTitleColor: '#9ca3af',
      diffViewerTitleBorderColor: 'rgba(255,255,255,0.06)',
    },
  },
};

export default function DiffView({ oldCode, newCode }) {
  const [split, setSplit] = useState(true);
  const [copied, setCopied] = useState(false);

  if (!newCode) return null;

  const handleCopy = () => {
    navigator.clipboard.writeText(newCode);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div
      className="fade-in overflow-hidden"
      style={{
        border: '1px solid rgba(255,255,255,0.07)',
        borderRadius: '16px',
        background: '#0a0a0f',
      }}
    >
      {/* Header */}
      <div
        className="flex items-center justify-between px-5 py-3"
        style={{ borderBottom: '1px solid rgba(255,255,255,0.06)', background: 'rgba(0,0,0,0.3)' }}
      >
        <div className="flex items-center gap-3">
          <span className="text-xs font-semibold text-gray-400 uppercase tracking-widest">
            Improved Code
          </span>
          <span
            className="text-xs px-2 py-0.5 rounded"
            style={{ background: 'rgba(44,182,125,0.1)', color: '#2cb67d', border: '1px solid rgba(44,182,125,0.2)' }}
          >
            Diff View
          </span>
        </div>

        <div className="flex items-center gap-2">
          {/* Split / Unified toggle */}
          <div
            className="flex rounded-lg overflow-hidden text-xs"
            style={{ border: '1px solid rgba(255,255,255,0.08)' }}
          >
            {[['Split', true], ['Unified', false]].map(([label, val]) => (
              <button
                key={label}
                onClick={() => setSplit(val)}
                className="px-3 py-1.5 transition-colors duration-150"
                style={{
                  background: split === val ? 'rgba(127,90,240,0.2)' : 'transparent',
                  color: split === val ? '#a78bfa' : '#6b7280',
                  cursor: 'pointer',
                  border: 'none',
                }}
              >
                {label}
              </button>
            ))}
          </div>

          <button
            onClick={handleCopy}
            className="text-xs px-3 py-1.5 rounded-lg transition-all duration-150 active:scale-95"
            style={{
              background: copied ? 'rgba(44,182,125,0.15)' : 'rgba(255,255,255,0.05)',
              color: copied ? '#2cb67d' : '#9ca3af',
              border: copied ? '1px solid rgba(44,182,125,0.3)' : '1px solid rgba(255,255,255,0.08)',
              cursor: 'pointer',
            }}
          >
            {copied ? '✓ Copied' : 'Copy'}
          </button>
        </div>
      </div>

      {/* Diff */}
      <div className="diff-container overflow-auto" style={{ maxHeight: '380px' }}>
        <ReactDiffViewer
          oldValue={oldCode}
          newValue={newCode}
          splitView={split}
          useDarkTheme
          compareMethod={DiffMethod.WORDS}
          styles={DARK_THEME}
          leftTitle="Original"
          rightTitle="Improved"
          hideLineNumbers={false}
        />
      </div>
    </div>
  );
}
