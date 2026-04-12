/**
 * VS Code–style file explorer panel.
 * Supports flat file list with active highlight, rename, and remove.
 * Folder grouping is derived automatically from file paths.
 */

const EXT_ICON = {
  java: '☕',
  js: '📜',
  ts: '📘',
  py: '🐍',
  md: '📝',
};

function fileIcon(name) {
  const ext = name.split('.').pop()?.toLowerCase();
  return EXT_ICON[ext] ?? '📄';
}

function basename(path) {
  return path.split('/').pop() ?? path;
}

function dirname(path) {
  const parts = path.split('/');
  return parts.length > 1 ? parts.slice(0, -1).join('/') : null;
}

/* ── Single file row ──────────────────────────────────────── */
function FileRow({ file, isActive, depth, onSelect, onRemove }) {
  return (
    <div
      onClick={() => onSelect(file)}
      style={{
        display: 'flex', alignItems: 'center', justifyContent: 'space-between',
        gap: 6, padding: `4px ${8 + depth * 12}px 4px ${8 + depth * 12}px`,
        borderRadius: 5, cursor: 'pointer',
        background: isActive ? 'rgba(127,90,240,0.12)' : 'transparent',
        color: isActive ? '#c4b5fd' : '#9ca3af',
        transition: 'background 100ms ease, color 100ms ease',
        userSelect: 'none',
      }}
      onMouseEnter={(e) => { if (!isActive) e.currentTarget.style.background = 'rgba(255,255,255,0.04)'; }}
      onMouseLeave={(e) => { if (!isActive) e.currentTarget.style.background = 'transparent'; }}
    >
      <div style={{ display: 'flex', alignItems: 'center', gap: 6, minWidth: 0 }}>
        <span style={{ fontSize: 11, flexShrink: 0 }}>{fileIcon(file.name)}</span>
        <span style={{ fontSize: 12, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
          {basename(file.name)}
        </span>
      </div>
      <button
        onClick={(e) => { e.stopPropagation(); onRemove(file); }}
        style={{
          background: 'none', border: 'none', cursor: 'pointer',
          fontSize: 11, color: '#374151', padding: '0 2px', flexShrink: 0,
          opacity: 0, transition: 'opacity 100ms ease',
        }}
        onMouseEnter={(e) => (e.currentTarget.style.color = '#f87171')}
        onMouseLeave={(e) => (e.currentTarget.style.color = '#374151')}
        className="file-remove-btn"
        title="Remove file"
      >✕</button>
    </div>
  );
}

/* ── Main component ───────────────────────────────────────── */
export default function FileExplorer({ files, activeFile, onSelect, onRemove, onAddFiles }) {
  // Group files by top-level directory
  const groups = {};
  files.forEach((f) => {
    const dir = dirname(f.name) ?? '(root)';
    if (!groups[dir]) groups[dir] = [];
    groups[dir].push(f);
  });

  return (
    <div style={{
      width: 200, flexShrink: 0,
      borderRight: '1px solid rgba(255,255,255,0.05)',
      display: 'flex', flexDirection: 'column',
      background: '#0b0b0f',
      userSelect: 'none',
    }}>
      {/* Header */}
      <div style={{
        display: 'flex', alignItems: 'center', justifyContent: 'space-between',
        padding: '8px 10px 6px',
        borderBottom: '1px solid rgba(255,255,255,0.04)',
      }}>
        <span style={{ fontSize: 10, fontWeight: 600, color: '#374151', textTransform: 'uppercase', letterSpacing: '0.07em' }}>
          Files
        </span>
        <label title="Add files" style={{
          fontSize: 14, color: '#4b5563', cursor: 'pointer', lineHeight: 1,
          transition: 'color 150ms ease',
        }}
          onMouseEnter={(e) => (e.currentTarget.style.color = '#9ca3af')}
          onMouseLeave={(e) => (e.currentTarget.style.color = '#4b5563')}
        >
          +
          <input
            type="file" multiple accept=".java,.js,.ts,.py,.md"
            style={{ display: 'none' }}
            onChange={(e) => onAddFiles(Array.from(e.target.files))}
          />
        </label>
      </div>

      {/* File tree */}
      <div style={{ flex: 1, overflowY: 'auto', padding: '4px 4px' }}>
        {files.length === 0 ? (
          <div style={{ padding: '16px 10px', textAlign: 'center' }}>
            <p style={{ fontSize: 11, color: '#374151' }}>No files open</p>
            <p style={{ fontSize: 10, color: '#1f2937', marginTop: 4 }}>Click + to add</p>
          </div>
        ) : (
          Object.entries(groups).map(([dir, groupFiles]) => (
            <div key={dir}>
              {/* Folder label — only show if there's more than one group or it's not root */}
              {(Object.keys(groups).length > 1 || dir !== '(root)') && (
                <div style={{
                  fontSize: 10, color: '#374151', padding: '6px 8px 2px',
                  textTransform: 'uppercase', letterSpacing: '0.06em',
                  display: 'flex', alignItems: 'center', gap: 4,
                }}>
                  <span>▾</span> {dir === '(root)' ? 'root' : dir.split('/').pop()}
                </div>
              )}
              {groupFiles.map((file) => (
                <FileRow
                  key={file.name}
                  file={file}
                  isActive={activeFile?.name === file.name}
                  depth={dir === '(root)' ? 0 : 1}
                  onSelect={onSelect}
                  onRemove={onRemove}
                />
              ))}
            </div>
          ))
        )}
      </div>

      {/* File count footer */}
      {files.length > 0 && (
        <div style={{
          padding: '5px 10px',
          borderTop: '1px solid rgba(255,255,255,0.04)',
          fontSize: 10, color: '#374151',
        }}>
          {files.length} file{files.length !== 1 ? 's' : ''}
        </div>
      )}

      {/* CSS for hover show of remove button */}
      <style>{`
        div:hover > div > .file-remove-btn { opacity: 1 !important; }
      `}</style>
    </div>
  );
}
