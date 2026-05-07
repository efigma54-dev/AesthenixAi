import * as vscode from 'vscode';
import { Issue, log } from './api';

/**
 * Inline completion provider — Copilot-style ghost text.
 *
 * Behaviour:
 *  - When cursor is on a flagged line, ghost text shows the fix.
 *  - Tab accepts the fix (VS Code built-in).
 *  - `aesthenix.acceptFix` applies the fix directly to the document.
 *  - `aesthenix.rejectFix` clears the suggestion for that line.
 *  - 300ms debounce prevents jitter while the user is typing.
 *  - Only triggers when the line is non-empty (avoids blank-line noise).
 */

// Map: "uri:line" → fix snippet
const fixMap = new Map<string, string>();

// Debounce: track last call time per document
const lastCallMs = new Map<string, number>();
const DEBOUNCE_MS = 300;

/** Called after analysis — stores per-line fixes */
export function applyInlineFixes(uri: vscode.Uri, issues: Issue[]) {
  const prefix = uri.toString() + ':';
  for (const key of fixMap.keys()) {
    if (key.startsWith(prefix)) fixMap.delete(key);
  }
  let stored = 0;
  for (const issue of issues) {
    if (issue.fix && issue.line > 0) {
      fixMap.set(`${uri.toString()}:${issue.line}`, issue.fix);
      stored++;
    }
  }
  if (stored > 0)
    log(`inlineCompletion: stored ${stored} fix(es) for ${uri.fsPath.split(/[\\/]/).pop()}`);
}

/** Clear all inline fixes for a file (called on document change) */
export function clearInlineFixes(uri: vscode.Uri) {
  const prefix = uri.toString() + ':';
  for (const key of fixMap.keys()) {
    if (key.startsWith(prefix)) fixMap.delete(key);
  }
}

/** Get the fix for a specific line (used by acceptFix command) */
export function getFixForLine(uri: vscode.Uri, line: number): string | undefined {
  return fixMap.get(`${uri.toString()}:${line}`);
}

/** Remove fix for a specific line (used by rejectFix command) */
export function rejectFixForLine(uri: vscode.Uri, line: number) {
  fixMap.delete(`${uri.toString()}:${line}`);
  log(`inlineCompletion: rejected fix at line ${line}`);
}

export class AesthenixInlineCompletionProvider implements vscode.InlineCompletionItemProvider {

  provideInlineCompletionItems(
    document: vscode.TextDocument,
    position: vscode.Position,
    _context: vscode.InlineCompletionContext,
    _token: vscode.CancellationToken,
  ): vscode.InlineCompletionList | undefined {
    if (document.languageId !== 'java') return undefined;

    // Debounce — skip if called too recently for this document
    const docKey = document.uri.toString();
    const now = Date.now();
    const last = lastCallMs.get(docKey) ?? 0;
    if (now - last < DEBOUNCE_MS) return undefined;
    lastCallMs.set(docKey, now);

    // Don't trigger on blank lines — avoids noise
    const currentLine = document.lineAt(position.line);
    if (!currentLine.text.trim()) return undefined;

    const line1 = position.line + 1; // API uses 1-indexed lines
    const fix   = fixMap.get(`${document.uri.toString()}:${line1}`);
    if (!fix) return undefined;

    // Indent fix to match current line
    const indent = currentLine.text.match(/^(\s*)/)?.[1] ?? '';
    const insertText = `\n${indent}// ✨ AESTHENIXAI: ${fix}`;

    log(`inlineCompletion: suggesting fix at line ${line1}`);

    return {
      items: [
        new vscode.InlineCompletionItem(
          insertText,
          new vscode.Range(position, position),
        ),
      ],
    };
  }
}
