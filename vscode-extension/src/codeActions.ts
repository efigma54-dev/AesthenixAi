import * as vscode from 'vscode';

/**
 * Per-issue-type fix snippets shown in the lightbulb menu.
 * These are short, actionable one-liners — not full refactors.
 */
const QUICK_FIX_LABEL: Record<string, string> = {
  Performance:     'Fix: Use StringBuilder instead of string concatenation',
  Bug:             'Fix: Add proper exception handling',
  Security:        'Fix: Catch specific exception type',
  Maintainability: 'Fix: Extract method / reduce complexity',
  Style:           'Fix: Replace System.out with logger',
  General:         'Fix: Apply AI suggestion',
};

/**
 * Provides lightbulb quick-fix actions for every AESTHENIXAI diagnostic.
 *
 * Per issue:
 *   1. Specific fix label (e.g. "Fix: Use StringBuilder") → triggers full refactor
 *   2. "Show analysis results" → re-runs analysis and opens panel
 */
export class AesthenixCodeActionProvider implements vscode.CodeActionProvider {

  static readonly providedCodeActionKinds = [
    vscode.CodeActionKind.QuickFix,
    vscode.CodeActionKind.RefactorRewrite,
  ];

  provideCodeActions(
    document: vscode.TextDocument,
    _range: vscode.Range,
    context: vscode.CodeActionContext,
  ): vscode.CodeAction[] {
    const ours = context.diagnostics.filter(d => d.source === 'AESTHENIXAI');
    if (ours.length === 0) return [];

    const actions: vscode.CodeAction[] = [];

    for (const diag of ours) {
      // Extract issue type from diagnostic message: "[Performance] ..."
      const typeMatch = diag.message.match(/^\[(\w+)\]/);
      const issueType = typeMatch?.[1] ?? 'General';
      const fixLabel  = QUICK_FIX_LABEL[issueType] ?? QUICK_FIX_LABEL.General;

      // Primary: specific fix label — triggers full AI refactor
      const fix = new vscode.CodeAction(
        `$(sparkle) AESTHENIXAI: ${fixLabel}`,
        vscode.CodeActionKind.QuickFix,
      );
      fix.diagnostics = [diag];
      fix.command = {
        command:   'aesthenix.applyRefactor',
        title:     fixLabel,
        arguments: [document],
      };
      fix.isPreferred = true;
      actions.push(fix);

      // Secondary: re-analyze and open panel
      const open = new vscode.CodeAction(
        `$(info) AESTHENIXAI: Show full analysis`,
        vscode.CodeActionKind.Empty,
      );
      open.diagnostics = [diag];
      open.command = {
        command: 'aesthenix.analyze',
        title:   'Show full analysis',
      };
      actions.push(open);
    }

    return actions;
  }
}
