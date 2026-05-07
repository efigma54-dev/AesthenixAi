import * as vscode from 'vscode';
import { Issue } from './api';

// Severity mapping
const SEVERITY: Record<string, vscode.DiagnosticSeverity> = {
  Bug:             vscode.DiagnosticSeverity.Error,
  Security:        vscode.DiagnosticSeverity.Error,
  Performance:     vscode.DiagnosticSeverity.Warning,
  Maintainability: vscode.DiagnosticSeverity.Warning,
  Style:           vscode.DiagnosticSeverity.Information,
  General:         vscode.DiagnosticSeverity.Hint,
};

/**
 * "Why it's bad" explanations shown in hover tooltips.
 * Keeps feedback educational, not just a label.
 */
const WHY_BAD: Record<string, string> = {
  Bug:             '**Why:** This is a logic error that will cause incorrect behaviour or a crash at runtime.',
  Security:        '**Why:** This pattern can be exploited by attackers — e.g. injection, data exposure, or privilege escalation.',
  Performance:     '**Why:** This pattern has poor time or space complexity. It will slow down noticeably as input grows.',
  Maintainability: '**Why:** This makes the code harder to read, test, and change safely over time.',
  Style:           '**Why:** This deviates from Java conventions, making the code harder for other developers to follow.',
  General:         '**Why:** This is a code quality concern that may cause problems in production.',
};

// Decoration types — one per severity colour
let errorDecoration:   vscode.TextEditorDecorationType;
let warningDecoration: vscode.TextEditorDecorationType;
let infoDecoration:    vscode.TextEditorDecorationType;

export function initDecorations() {
  errorDecoration = vscode.window.createTextEditorDecorationType({
    backgroundColor: 'rgba(248,113,113,0.10)',
    borderWidth: '0 0 0 3px',
    borderStyle: 'solid',
    borderColor: 'rgba(248,113,113,0.8)',
    isWholeLine: true,
  });
  warningDecoration = vscode.window.createTextEditorDecorationType({
    backgroundColor: 'rgba(251,191,36,0.08)',
    borderWidth: '0 0 0 3px',
    borderStyle: 'solid',
    borderColor: 'rgba(251,191,36,0.8)',
    isWholeLine: true,
  });
  infoDecoration = vscode.window.createTextEditorDecorationType({
    backgroundColor: 'rgba(96,165,250,0.06)',
    borderWidth: '0 0 0 3px',
    borderStyle: 'solid',
    borderColor: 'rgba(96,165,250,0.6)',
    isWholeLine: true,
  });
}

export function disposeDecorations() {
  errorDecoration?.dispose();
  warningDecoration?.dispose();
  infoDecoration?.dispose();
}

/** Apply inline decorations and VS Code diagnostics for all issues */
export function applyIssues(
  editor: vscode.TextEditor,
  collection: vscode.DiagnosticCollection,
  issues: Issue[]
) {
  const uri       = editor.document.uri;
  const lineCount = editor.document.lineCount;

  const diagnostics:   vscode.Diagnostic[]        = [];
  const errorRanges:   vscode.DecorationOptions[] = [];
  const warningRanges: vscode.DecorationOptions[] = [];
  const infoRanges:    vscode.DecorationOptions[] = [];

  for (const issue of issues) {
    // VS Code lines are 0-indexed; API returns 1-indexed
    const lineIndex = Math.max(0, Math.min((issue.line ?? 1) - 1, lineCount - 1));
    const lineText  = editor.document.lineAt(lineIndex);
    const range     = new vscode.Range(lineIndex, 0, lineIndex, lineText.text.length);

    const severity = SEVERITY[issue.type] ?? vscode.DiagnosticSeverity.Hint;
    const diag     = new vscode.Diagnostic(range, `[${issue.type}] ${issue.message}`, severity);
    diag.source    = 'AESTHENIXAI';
    diagnostics.push(diag);

    // Hover tooltip: issue message + why it's bad + fix hint
    const why  = WHY_BAD[issue.type] ?? '';
    const hover = new vscode.MarkdownString(
      `### $(warning) ${issue.type} — line ${issue.line}\n\n` +
      `${issue.message}\n\n` +
      `${why}\n\n` +
      `*💡 Run **AESTHENIXAI: Apply AI Refactor** for a suggested fix.*`
    );
    hover.isTrusted = true;
    hover.supportThemeIcons = true;

    const decoration: vscode.DecorationOptions = { range, hoverMessage: hover };

    if (severity === vscode.DiagnosticSeverity.Error)        errorRanges.push(decoration);
    else if (severity === vscode.DiagnosticSeverity.Warning) warningRanges.push(decoration);
    else                                                      infoRanges.push(decoration);
  }

  collection.set(uri, diagnostics);

  const cfg = vscode.workspace.getConfiguration('aesthenixai');
  if (cfg.get<boolean>('highlightIssues', true)) {
    editor.setDecorations(errorDecoration,   errorRanges);
    editor.setDecorations(warningDecoration, warningRanges);
    editor.setDecorations(infoDecoration,    infoRanges);
  }
}

/** Clear all decorations and diagnostics for a document */
export function clearIssues(
  editor: vscode.TextEditor,
  collection: vscode.DiagnosticCollection
) {
  collection.delete(editor.document.uri);
  editor.setDecorations(errorDecoration,   []);
  editor.setDecorations(warningDecoration, []);
  editor.setDecorations(infoDecoration,    []);
}
