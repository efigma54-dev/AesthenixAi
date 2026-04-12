import * as vscode from 'vscode';

/**
 * Provides lightbulb quick-fix actions for every AESTHENIXAI diagnostic.
 * Shows two actions per issue:
 *   1. "Apply AI refactor" — replaces the whole file with the improved version
 *   2. "Show analysis panel" — re-opens the results panel
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
    // Only act on diagnostics from our own source
    const ours = context.diagnostics.filter(d => d.source === 'AESTHENIXAI');
    if (ours.length === 0) return [];

    const actions: vscode.CodeAction[] = [];

    // One "Apply AI refactor" action per diagnostic (lightbulb on each issue line)
    for (const diag of ours) {
      const fix = new vscode.CodeAction(
        `$(sparkle) AESTHENIXAI: Apply AI refactor`,
        vscode.CodeActionKind.QuickFix,
      );
      fix.diagnostics = [diag];
      fix.command = {
        command:   'aesthenix.applyRefactor',
        title:     'Apply AI refactor',
        arguments: [document],
      };
      // Mark as preferred so it appears first in the lightbulb menu
      fix.isPreferred = true;
      actions.push(fix);

      // Secondary: open the results panel
      const open = new vscode.CodeAction(
        `$(info) AESTHENIXAI: Show analysis results`,
        vscode.CodeActionKind.Empty,
      );
      open.diagnostics = [diag];
      open.command = {
        command: 'aesthenix.analyze',
        title:   'Show analysis results',
      };
      actions.push(open);
    }

    return actions;
  }
}
