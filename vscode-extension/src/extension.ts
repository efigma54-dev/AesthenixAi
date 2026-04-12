import * as vscode from 'vscode';
import { reviewCode, checkHealth, ApiError } from './api';
import { initDecorations, disposeDecorations, applyIssues, clearIssues } from './diagnostics';
import { showResultsPanel } from './panel';
import { AesthenixCodeActionProvider } from './codeActions';
import { applyRefactor, cacheImprovedCode, clearImprovedCode } from './refactor';

const diagnosticCollection = vscode.languages.createDiagnosticCollection('aesthenixai');

export function activate(context: vscode.ExtensionContext) {
  initDecorations();

  // ── Analyze current file ───────────────────────────────────
  context.subscriptions.push(
    vscode.commands.registerCommand('aesthenix.analyze', () => runAnalysis(context))
  );

  // ── Analyze selection ──────────────────────────────────────
  context.subscriptions.push(
    vscode.commands.registerCommand('aesthenix.analyzeSelection', () => runAnalysis(context, true))
  );

  // ── Clear diagnostics ──────────────────────────────────────
  context.subscriptions.push(
    vscode.commands.registerCommand('aesthenix.clearDiagnostics', () => {
      const editor = vscode.window.activeTextEditor;
      if (editor) clearIssues(editor, diagnosticCollection);
    })
  );

  // ── Apply AI refactor (called from command palette or code action) ──
  context.subscriptions.push(
    vscode.commands.registerCommand(
      'aesthenix.applyRefactor',
      (doc?: vscode.TextDocument) => applyRefactor(context, doc)
    )
  );

  // ── Code action provider (lightbulb quick-fixes) ───────────
  context.subscriptions.push(
    vscode.languages.registerCodeActionsProvider(
      { language: 'java', scheme: 'file' },
      new AesthenixCodeActionProvider(),
      { providedCodeActionKinds: AesthenixCodeActionProvider.providedCodeActionKinds }
    )
  );

  // ── Auto-analyze on save (opt-in) ──────────────────────────
  context.subscriptions.push(
    vscode.workspace.onDidSaveTextDocument(async (doc) => {
      const cfg = vscode.workspace.getConfiguration('aesthenixai');
      if (!cfg.get<boolean>('autoAnalyzeOnSave', false)) return;
      if (doc.languageId !== 'java') return;
      const editor = vscode.window.visibleTextEditors.find(e => e.document === doc);
      if (editor) await analyze(context, editor, doc.getText());
    })
  );

  // ── Clear improved-code cache when document changes ────────
  context.subscriptions.push(
    vscode.workspace.onDidChangeTextDocument(e => {
      if (e.document.languageId === 'java') clearImprovedCode(e.document.uri);
    })
  );

  context.subscriptions.push(diagnosticCollection);
}

export function deactivate() {
  disposeDecorations();
  diagnosticCollection.dispose();
}

// ── Core analysis flow ─────────────────────────────────────
async function runAnalysis(context: vscode.ExtensionContext, selectionOnly = false) {
  const editor = vscode.window.activeTextEditor;
  if (!editor) { vscode.window.showWarningMessage('AESTHENIXAI: No active editor.'); return; }
  if (editor.document.languageId !== 'java') { vscode.window.showWarningMessage('AESTHENIXAI: Only Java files are supported.'); return; }

  const code = selectionOnly && !editor.selection.isEmpty
    ? editor.document.getText(editor.selection)
    : editor.document.getText();

  if (!code.trim()) { vscode.window.showWarningMessage('AESTHENIXAI: File is empty.'); return; }

  await analyze(context, editor, code);
}

async function analyze(
  context: vscode.ExtensionContext,
  editor: vscode.TextEditor,
  code: string,
) {
  const filename = editor.document.fileName.split(/[\\/]/).pop() ?? 'file.java';

  await vscode.window.withProgress(
    { location: vscode.ProgressLocation.Notification, title: `AESTHENIXAI: Analyzing ${filename}…`, cancellable: false },
    async (progress) => {
      progress.report({ message: 'Checking server…', increment: 10 });
      const healthy = await checkHealth();
      if (!healthy) {
        const cfg = vscode.workspace.getConfiguration('aesthenixai');
        const url = cfg.get<string>('backendUrl', 'http://localhost:8080/api');
        const action = await vscode.window.showErrorMessage(
          `AESTHENIXAI: Backend not reachable at ${url}. Run: ./start.sh`,
          'Open Settings',
        );
        if (action === 'Open Settings')
          vscode.commands.executeCommand('workbench.action.openSettings', 'aesthenixai.backendUrl');
        return;
      }

      progress.report({ message: 'Analyzing with AI…', increment: 40 });
      try {
        const result = await reviewCode(code);

        progress.report({ message: 'Applying results…', increment: 40 });
        applyIssues(editor, diagnosticCollection, result.issues);
        showResultsPanel(context, filename, result);

        // Cache improved code so the refactor command can use it without a second API call
        if (result.improvedCode) cacheImprovedCode(editor.document.uri, result.improvedCode);

        const icon = result.score >= 75 ? '$(pass)' : result.score >= 50 ? '$(warning)' : '$(error)';
        const hasImproved = Boolean(result.improvedCode.trim());

        // Offer refactor in status bar if improvements exist
        const statusMsg = `${icon} AESTHENIXAI: ${filename} — ${result.score}/100, ${result.issues.length} issue${result.issues.length !== 1 ? 's' : ''}${hasImproved ? ' · Apply refactor?' : ''}`;
        vscode.window.setStatusBarMessage(statusMsg, 10_000);

        // If there are issues and improved code, nudge the user
        if (result.issues.length > 0 && hasImproved) {
          const pick = await vscode.window.showInformationMessage(
            `AESTHENIXAI: Found ${result.issues.length} issue${result.issues.length !== 1 ? 's' : ''} in ${filename}. Apply AI refactor?`,
            'Apply Refactor',
            'Dismiss',
          );
          if (pick === 'Apply Refactor') applyRefactor(context, editor.document);
        }

      } catch (err) {
        const msg = err instanceof ApiError ? err.message : 'Analysis failed.';
        const action = await vscode.window.showErrorMessage(`AESTHENIXAI: ${msg}`, 'Retry');
        if (action === 'Retry') analyze(context, editor, code);
      }
    }
  );
}
