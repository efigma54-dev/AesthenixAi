import { useState } from 'react';
import ScoreCard from './ScoreCard';
import IssueList from './IssueList';

function scoreColor(s) { return s >= 75 ? '#4ade80' : s >= 50 ? '#fbbf24' : '#f87171'; }

function FileRow({ file, index, onSelect, selected }) {
  const c = scoreColor(file.score);
  return (
    <div className="fade-in hover-row" onClick={() => onSelect(file)}
      style={{
        animationDelay: `${index * 35}ms`,
        display: 'flex', alignItems: 'center', justifyContent: 'space-between',
        padding: '6px 8px', cursor: 'pointer', borderRadius: 6,
        background: selected ? 'rgba(127,90,240,0.08)' : 'transparent',
        border: selected ? '1px solid rgba(127,90,240,0.2)' : '1px solid transparent',
        transition: 'all 150ms ease',
      }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 6, minWidth: 0 }}>
        <span style={{ fontSize: 11, flexShrink: 0 }}>📄</span>
        <span style={{ fontSize: 11, color: '#9ca3af', fontFamily: 'monospace', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{file.name}</span>
      </div>
      <div style={{ display: 'flex', alignItems: 'center', gap: 8, flexShrink: 0 }}>
        <span style={{ fontSize: 10, color: '#4b5563' }}>{file.issues?.length ?? 0} issues</span>
        <span style={{ fontSize: 11, fontWeight: 700, color: c, background: `${c}12`, border: `1px solid ${c}25`, padding: '1px 6px', borderRadius: 4 }}>{file.score}</span>
      </div>
    </div>
  );
}

function RepoStats({ repo, results }) {
  const avg = results.length ? Math.round(results.reduce((s, r) => s + r.score, 0) / results.length) : 0;
  const issues = results.reduce((s, r) => s + (r.issues?.length ?? 0), 0);
  const c = scoreColor(avg);
  return (
    <div className="card fade-in" style={{ padding: '12px 14px' }}>
      <div style={{ display: 'flex', alignItems: 'flex-start', gap: 10, marginBottom: 12, paddingBottom: 12, borderBottom: '1px solid rgba(255,255,255,0.06)' }}>
        <span style={{ fontSize: 20 }}>🐙</span>
        <div style={{ minWidth: 0 }}>
          <div style={{ fontSize: 13, fontWeight: 600, color: '#e2e8f0', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{repo.full_name}</div>
          {repo.description && <div className="line-clamp-2" style={{ fontSize: 11, color: '#6b7280', marginTop: 2 }}>{repo.description}</div>}
          <div style={{ display: 'flex', gap: 10, marginTop: 4 }}>
            {repo.language && <span style={{ fontSize: 10, color: '#a78bfa' }}>⬡ {repo.language}</span>}
            {repo.license?.spdx_id && <span style={{ fontSize: 10, color: '#4b5563' }}>{repo.license.spdx_id}</span>}
          </div>
        </div>
      </div>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4,1fr)', gap: 8 }}>
        {[
          { label: 'Files', value: results.length, color: '#a78bfa' },
          { label: 'Issues', value: issues, color: '#f87171' },
          { label: 'Score', value: `${avg}/100`, color: c },
          { label: 'Stars', value: repo.stargazers_count ?? '—', color: '#fbbf24' },
        ].map((s) => (
          <div key={s.label} style={{ textAlign: 'center' }}>
            <div style={{ fontSize: 14, fontWeight: 700, color: s.color }}>{s.value}</div>
            <div style={{ fontSize: 10, color: '#4b5563', marginTop: 2 }}>{s.label}</div>
          </div>
        ))}
      </div>
    </div>
  );
}

export default function GithubResults({ repo, results }) {
  const [selected, setSelected] = useState(results[0] ?? null);
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 8, overflowY: 'auto', maxHeight: 'calc(100vh - 120px)' }}>
      <RepoStats repo={repo} results={results} />

      <div className="card fade-in" style={{ padding: '10px 12px' }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 6 }}>
          <span style={{ fontSize: 11, fontWeight: 600, color: '#6b7280', textTransform: 'uppercase', letterSpacing: '0.06em' }}>Java Files</span>
          <span style={{ fontSize: 10, color: '#a78bfa', background: 'rgba(167,139,250,0.08)', border: '1px solid rgba(167,139,250,0.15)', padding: '1px 7px', borderRadius: 4 }}>{results.length}</span>
        </div>
        {results.map((file, i) => (
          <FileRow key={file.name} file={file} index={i} selected={selected?.name === file.name} onSelect={setSelected} />
        ))}
      </div>

      {selected && (
        <div className="fade-in" key={selected.name} style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
          <div style={{ fontSize: 11, color: '#a78bfa', fontFamily: 'monospace', padding: '6px 10px', borderRadius: 6, background: 'rgba(127,90,240,0.06)', border: '1px solid rgba(127,90,240,0.15)' }}>
            📄 {selected.name}
          </div>
          <ScoreCard score={selected.score} parsedInfo={selected.parsedInfo} />
          <IssueList issues={selected.issues} />
        </div>
      )}
    </div>
  );
}
