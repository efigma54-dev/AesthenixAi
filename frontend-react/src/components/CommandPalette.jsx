import { Command } from 'cmdk';
import { useEffect } from 'react';

const ACTIONS = [
  {
    group: 'Navigate', items: [
      { label: 'Go to Editor', icon: '⌨', action: 'nav:editor' },
      { label: 'Go to GitHub Analyzer', icon: '🐙', action: 'nav:github' },
      { label: 'Go to History', icon: '🕐', action: 'nav:history' },
    ]
  },
  {
    group: 'Actions', items: [
      { label: 'Run Analysis', icon: '▶', action: 'run' },
      { label: 'Clear Editor', icon: '✕', action: 'clear' },
    ]
  },
  {
    group: 'Links', items: [
      { label: 'View Docs', icon: '📖', action: 'link:docs' },
      { label: 'View GitHub', icon: '🔗', action: 'link:github' },
    ]
  },
];

export default function CommandPalette({ open, onOpenChange, onAction }) {
  // Ctrl/Cmd+K
  useEffect(() => {
    const handler = (e) => {
      if (e.key === 'k' && (e.metaKey || e.ctrlKey)) {
        e.preventDefault();
        onOpenChange((prev) => !prev);
      }
    };
    document.addEventListener('keydown', handler);
    return () => document.removeEventListener('keydown', handler);
  }, [onOpenChange]);

  return (
    <Command.Dialog
      open={open}
      onOpenChange={onOpenChange}
      label="Command palette"
      style={{
        position: 'fixed', inset: 0, zIndex: 100,
        display: 'flex', alignItems: 'flex-start', justifyContent: 'center',
        paddingTop: 120,
        background: 'rgba(0,0,0,0.55)',
        backdropFilter: 'blur(6px)',
      }}
    >
      <div style={{
        width: 480, borderRadius: 12, overflow: 'hidden',
        background: '#111116',
        border: '1px solid rgba(255,255,255,0.1)',
        boxShadow: '0 24px 64px rgba(0,0,0,0.6)',
      }}>
        {/* Search input */}
        <div style={{
          display: 'flex', alignItems: 'center', gap: 10,
          padding: '12px 16px',
          borderBottom: '1px solid rgba(255,255,255,0.06)',
        }}>
          <span style={{ fontSize: 14, color: '#4b5563', flexShrink: 0 }}>⌕</span>
          <Command.Input
            placeholder="Search actions…"
            style={{
              flex: 1, background: 'transparent', border: 'none', outline: 'none',
              fontSize: 13, color: '#e2e8f0',
            }}
          />
          <kbd style={{
            fontSize: 10, padding: '2px 6px', borderRadius: 4,
            background: 'rgba(255,255,255,0.06)', border: '1px solid rgba(255,255,255,0.1)',
            color: '#4b5563',
          }}>ESC</kbd>
        </div>

        {/* Results */}
        <Command.List style={{ maxHeight: 320, overflowY: 'auto', padding: '6px 8px' }}>
          <Command.Empty style={{ padding: '20px', textAlign: 'center', fontSize: 12, color: '#4b5563' }}>
            No results found.
          </Command.Empty>

          {ACTIONS.map(({ group, items }) => (
            <Command.Group key={group} heading={group}
              style={{ marginBottom: 4 }}
            >
              {/* Group label */}
              <div style={{
                fontSize: 10, fontWeight: 600, color: '#374151',
                textTransform: 'uppercase', letterSpacing: '0.07em',
                padding: '6px 8px 4px',
              }}>{group}</div>

              {items.map((item) => (
                <Command.Item
                  key={item.action}
                  value={item.label}
                  onSelect={() => { onAction(item.action); onOpenChange(false); }}
                  style={{
                    display: 'flex', alignItems: 'center', gap: 10,
                    padding: '7px 10px', borderRadius: 7, cursor: 'pointer',
                    fontSize: 13, color: '#9ca3af',
                    transition: 'background 100ms ease',
                  }}
                // cmdk adds data-selected; we style via CSS
                >
                  <span style={{ fontSize: 13, width: 18, textAlign: 'center', flexShrink: 0 }}>{item.icon}</span>
                  {item.label}
                </Command.Item>
              ))}
            </Command.Group>
          ))}
        </Command.List>

        {/* Footer hint */}
        <div style={{
          display: 'flex', gap: 16, padding: '8px 16px',
          borderTop: '1px solid rgba(255,255,255,0.05)',
          fontSize: 10, color: '#374151',
        }}>
          <span><kbd style={{ fontFamily: 'inherit' }}>↑↓</kbd> navigate</span>
          <span><kbd style={{ fontFamily: 'inherit' }}>↵</kbd> select</span>
          <span><kbd style={{ fontFamily: 'inherit' }}>ESC</kbd> close</span>
        </div>
      </div>
    </Command.Dialog>
  );
}
