import { useRef, useEffect } from 'react';
import Editor, { useMonaco } from '@monaco-editor/react';

const SAMPLE_CODE = `public class Example {

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
}`;

export default function CodeEditor({ code, setCode, issueLines = [], filename = 'Main.java', scrollToLine = null, onScrollConsumed }) {
  const editorRef = useRef(null);
  const monaco = useMonaco();
  const decorations = useRef([]);

  // Apply line highlights when issues arrive
  useEffect(() => {
    const editor = editorRef.current;
    if (!editor || !monaco || !issueLines.length) return;
    const newDecs = issueLines
      .filter((l) => Number.isInteger(l) && l > 0)
      .map((line) => ({
        range: new monaco.Range(line, 1, line, 1),
        options: {
          isWholeLine: true,
          className: 'issue-line-highlight',
          marginClassName: 'issue-line-highlight-margin',
          overviewRuler: { color: '#f87171', position: 1 },
        },
      }));
    decorations.current = editor.deltaDecorations(decorations.current, newDecs);
  }, [issueLines, monaco]);

  // Clear decorations when code is edited
  useEffect(() => {
    const editor = editorRef.current;
    if (!editor || !decorations.current.length) return;
    decorations.current = editor.deltaDecorations(decorations.current, []);
  }, [code]);

  // Scroll to line when an issue is clicked
  useEffect(() => {
    const editor = editorRef.current;
    if (!editor || !scrollToLine) return;
    editor.revealLineInCenter(scrollToLine);
    editor.setPosition({ lineNumber: scrollToLine, column: 1 });
    editor.focus();
    onScrollConsumed?.();
  }, [scrollToLine]);

  return (
    /* Gradient border: 1px wrapper with gradient bg, inner div with solid bg */
    <div
      className="glow"
      style={{
        borderRadius: '16px',
        padding: '1px',
        background: 'linear-gradient(135deg, #7f5af0, #2cb67d)',
      }}
    >
      <div
        style={{
          height: '520px',
          background: '#0a0a0f',
          borderRadius: '15px',
          display: 'flex',
          flexDirection: 'column',
          overflow: 'hidden',
        }}
      >
        {/* VS Code-style title bar */}
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            padding: '10px 16px',
            borderBottom: '1px solid rgba(255,255,255,0.06)',
            background: 'rgba(0,0,0,0.35)',
            flexShrink: 0,
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
            <div style={{ display: 'flex', gap: '6px' }}>
              <span style={{ width: 12, height: 12, borderRadius: '50%', background: '#ff5f57', display: 'block' }} />
              <span style={{ width: 12, height: 12, borderRadius: '50%', background: '#febc2e', display: 'block' }} />
              <span style={{ width: 12, height: 12, borderRadius: '50%', background: '#28c840', display: 'block' }} />
            </div>
            <span style={{ fontSize: 12, color: '#6b7280', fontFamily: 'monospace' }}>{filename}</span>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            {issueLines.length > 0 && (
              <span style={{
                fontSize: 11, padding: '2px 8px', borderRadius: 4, fontFamily: 'monospace',
                background: 'rgba(248,113,113,0.1)', color: '#f87171',
                border: '1px solid rgba(248,113,113,0.2)',
              }}>
                {issueLines.length} issue{issueLines.length !== 1 ? 's' : ''} highlighted
              </span>
            )}
            <span style={{
              fontSize: 11, fontWeight: 600, padding: '4px 10px', borderRadius: 6,
              fontFamily: 'monospace', background: 'rgba(127,90,240,0.12)',
              color: '#a78bfa', border: '1px solid rgba(127,90,240,0.25)',
            }}>
              JAVA
            </span>
          </div>
        </div>

        {/* Monaco */}
        <div style={{ flex: 1, overflow: 'hidden' }}>
          <Editor
            height="100%"
            defaultLanguage="java"
            theme="vs-dark"
            value={code || SAMPLE_CODE}
            onChange={(val) => setCode(val || '')}
            onMount={(editor) => { editorRef.current = editor; }}
            options={{
              fontSize: 13,
              fontFamily: "'Fira Code', 'Cascadia Code', 'Consolas', monospace",
              fontLigatures: true,
              minimap: { enabled: false },
              scrollBeyondLastLine: false,
              lineNumbers: 'on',
              renderLineHighlight: 'line',
              padding: { top: 14, bottom: 14 },
              smoothScrolling: true,
              cursorBlinking: 'smooth',
              cursorSmoothCaretAnimation: 'on',
              bracketPairColorization: { enabled: true },
              guides: { bracketPairs: true },
              overviewRulerLanes: 2,
            }}
          />
        </div>
      </div>
    </div>
  );
}
