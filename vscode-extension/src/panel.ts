import * as vscode from 'vscode';
import { ReviewResult } from './api';

let panel: vscode.WebviewPanel | undefined;

/** Open (or reveal) the results sidebar panel */
export function showResultsPanel(
  context: vscode.ExtensionContext,
  filename: string,
  result: ReviewResult
) {
  if (panel) {
    panel.reveal(vscode.ViewColumn.Beside);
  } else {
    panel = vscode.window.createWebviewPanel(
      'aesthenixResults',
      'AESTHENIXAI Results',
      vscode.ViewColumn.Beside,
      { enableScripts: false, retainContextWhenHidden: true }
    );
    panel.onDidDispose(() => { panel = undefined; }, null, context.subscriptions);
  }

  panel.title = `AESTHENIXAI — ${filename}`;
  panel.webview.html = buildHtml(filename, result);
}

function scoreColor(score: number): string {
  if (score >= 75) return '#4ade80';
  if (score >= 50) return '#fbbf24';
  return '#f87171';
}

function scoreLabel(score: number): string {
  if (score >= 75) return 'Good';
  if (score >= 50) return 'Average';
  return 'Poor';
}

const TYPE_COLOR: Record<string, string> = {
  Bug:             '#f87171',
  Security:        '#f87171',
  Performance:     '#fbbf24',
  Maintainability: '#a78bfa',
  Style:           '#60a5fa',
  General:         '#6b7280',
};

const TYPE_ICON: Record<string, string> = {
  Bug:             '🐛',
  Security:        '🔒',
  Performance:     '⚡',
  Maintainability: '🔧',
  Style:           '🎨',
  General:         '·',
};

function buildHtml(filename: string, r: ReviewResult): string {
  const color = scoreColor(r.score);
  const label = scoreLabel(r.score);

  const issuesHtml = r.issues.length === 0
    ? `<p style="color:#4b5563;font-size:12px">✓ No issues found</p>`
    : r.issues.map(i => {
        const c    = TYPE_COLOR[i.type] ?? '#6b7280';
        const icon = TYPE_ICON[i.type]  ?? '·';
        return `
          <div style="border-left:3px solid ${c};padding:6px 10px;margin-bottom:6px;background:rgba(255,255,255,0.03);border-radius:0 6px 6px 0">
            <div style="font-size:11px;color:${c};margin-bottom:3px">${icon} ${i.type} <span style="color:#4b5563">line ${i.line}</span></div>
            <div style="font-size:12px;color:#9ca3af">${escHtml(i.message)}</div>
          </div>`;
      }).join('')
  ;

  const suggestionsHtml = r.suggestions.length === 0 ? '' : `
    <h3 style="font-size:11px;color:#6b7280;text-transform:uppercase;letter-spacing:.06em;margin:16px 0 8px">Suggestions</h3>
    ${r.suggestions.map(s => `
      <div style="display:flex;gap:8px;padding:5px 0;font-size:12px;color:#9ca3af">
        <span style="color:#7f5af0;flex-shrink:0">→</span>${escHtml(s)}
      </div>`).join('')}
  `;

  const statsHtml = r.parsedInfo ? `
    <div style="display:grid;grid-template-columns:repeat(4,1fr);gap:8px;margin-top:12px;padding-top:12px;border-top:1px solid rgba(255,255,255,0.06)">
      ${[
        ['Methods',    r.parsedInfo.methodCount],
        ['Complexity', r.parsedInfo.cyclomaticComplexity],
        ['Loops',      r.parsedInfo.nestedLoopCount],
        ['Long Fns',   r.parsedInfo.longMethodCount],
      ].map(([l, v]) => `
        <div style="text-align:center">
          <div style="font-size:15px;font-weight:700;color:#7f5af0">${v}</div>
          <div style="font-size:10px;color:#4b5563;margin-top:2px">${l}</div>
        </div>`).join('')}
    </div>` : '';

  return `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8"/>
  <meta name="viewport" content="width=device-width,initial-scale=1"/>
  <style>
    * { box-sizing:border-box; margin:0; padding:0; }
    body { background:#0b0b0f; color:#e2e8f0; font-family:'Segoe UI',system-ui,sans-serif; padding:16px; font-size:13px; }
    h2 { font-size:13px; font-weight:600; color:#e2e8f0; margin-bottom:4px; }
    h3 { font-size:11px; font-weight:600; color:#6b7280; text-transform:uppercase; letter-spacing:.06em; margin:16px 0 8px; }
    .card { background:rgba(255,255,255,0.04); border:1px solid rgba(255,255,255,0.07); border-radius:10px; padding:14px; margin-bottom:12px; }
    .badge { display:inline-block; padding:2px 8px; border-radius:4px; font-size:11px; font-weight:600; }
  </style>
</head>
<body>
  <h2 style="margin-bottom:12px;color:#6b7280;font-weight:400">${escHtml(filename)}</h2>

  <!-- Score card -->
  <div class="card">
    <div style="display:flex;align-items:center;gap:12px">
      <div style="width:52px;height:52px;border-radius:50%;border:3px solid ${color};display:flex;align-items:center;justify-content:center;font-size:16px;font-weight:700;color:${color};flex-shrink:0">${r.score}</div>
      <div>
        <div style="font-size:11px;color:#6b7280;margin-bottom:2px">Code Quality</div>
        <div style="font-size:20px;font-weight:700;color:${color}">${r.score}<span style="font-size:12px;color:#4b5563;font-weight:400">/100</span></div>
        <span class="badge" style="color:${color};background:${color}18;border:1px solid ${color}30;margin-top:4px">${label}</span>
      </div>
    </div>
    ${statsHtml}
  </div>

  <!-- Issues -->
  <h3>Issues <span style="color:#f87171;background:rgba(248,113,113,0.1);border:1px solid rgba(248,113,113,0.2);padding:1px 7px;border-radius:4px;font-size:10px;margin-left:4px">${r.issues.length}</span></h3>
  <div class="card" style="padding:10px 12px">${issuesHtml}</div>

  ${suggestionsHtml}

  ${r.fromCache ? '<p style="font-size:10px;color:#374151;text-align:right;margin-top:8px">⚡ Cached result</p>' : ''}
</body>
</html>`;
}

function escHtml(s: string): string {
  return s.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
}
