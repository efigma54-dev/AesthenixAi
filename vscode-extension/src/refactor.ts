import * as vscode from 'vscode';
import { reviewCode, ApiError } from './api';

// Cache: uri → last improved code (so we don't re-call the API if already analyzed)
const improvedCodeCache = new Map<string, string>();

/** Store improved code from an analysis result so the refactor command can use it */
export function cacheImprovedCode(uri: vscode.Uri, improvedCode: string) {
  if (improvedCode.trim()) improvedCodeCache.set(uri.toString(), improvedCode);
}

/** Clear cached improved code when the document changes */
export function clearImprovedCode(uri: vscode.Uri) {
  improvedCodeCache.delete(uri.toString());
}

/**
 * Apply AI refactor to the active document.
 *
 * Flow:
 *  1. Use cached improved code if available (from a previous analysis).
 *  2. Otherwise call the API fresh.
 *  3. Show a diff preview — user must confirm before the edit is applied.
 */
export async function applyRefactor(
  context: vscode.ExtensionContext,
  document?: vscode.TextDocument,
): Promise<void> {
  const doc = document ?? vscode.window.activeTextEditor?.document;
  if (!doc) {
    vscode.window.showWarningMessage('AESTHENIXAI: No active document.');
    return;
  }
  if (doc.languageId !== 'java') {
    vscode.window.showWarningMessage('AESTHENIXAI: Only Java files are supported.');
    return;
  }

  const originalCode = doc.getText();
  if (!originalCode.trim()) {
    vscode.window.showWarningMessage('AESTHENIXAI: File is empty.');
    return;
  }

  const uriKey = doc.uri.toString();
  let improved = improvedCodeCache.get(uriKey);

  // If no cached result, fetch fresh
  if (!improved) {
    improved = await vscode.window.withProgress(
      {
        location: vscode.ProgressLocation.Notification,
        title: 'AESTHENIXAI: Generating refactored code…',
        cancellable: false,
      },
      async () => {
        try {
          const result = await reviewCode(originalCode);
          if (!result.improvedCode.trim()) {
            vscode.window.showInformationMessage('AESTHENIXAI: No improvements suggested for this code.');
            return undefined;
          }
          cacheImprovedCode(doc.uri, result.improvedCode);
          return result.improvedCode;
        } catch (err) {
          const msg = err instanceof ApiError ? err.message : 'Refactor failed.';
          vscode.window.showErrorMessage(`AESTHENIXAI: ${msg}`);
          return undefined;
        }
      }
    );
  }

  if (!improved) return;

  // Show diff in a virtual document so the user can review before accepting
  await showDiffAndConfirm(context, doc, originalCode, improved);
}

async function showDiffAndConfirm(
  context: vscode.ExtensionContext,
  doc: vscode.TextDocument,
  original: string,
  improved: string,
) {
  // Register a virtual document provider for the "improved" side of the diff
  const scheme = 'aesthenix-refactor';
  const provider = new (class implements vscode.TextDocumentContentProvider {
    provideTextDocumentContent(): string { return improved; }
  })();
  const disposable = vscode.workspace.registerTextDocumentContentProvider(scheme, provider);
  context.subscriptions.push(disposable);

  const originalUri = doc.uri;
  const improvedUri = vscode.Uri.parse(`${scheme}:${doc.fileName} (AI Refactored)`);

  // Open diff view
  await vscode.commands.executeCommand(
    'vscode.diff',
    originalUri,
    improvedUri,
    `AESTHENIXAI: ${doc.fileName.split(/[\\/]/).pop()} — Original ↔ AI Refactored`,
    { preview: true },
  );

  // Ask for confirmation
  const choice = await vscode.window.showInformationMessage(
    'Apply the AI-refactored version to your file?',
    { modal: false },
    'Apply',
    'Discard',
  );

  disposable.dispose();

  if (choice !== 'Apply') return;

  // Apply the edit
  const editor = vscode.window.visibleTextEditors.find(e => e.document.uri.toString() === originalUri.toString())
    ?? await vscode.window.showTextDocument(doc);

  await editor.edit(builder => {
    const fullRange = new vscode.Range(
      doc.positionAt(0),
      doc.positionAt(original.length),
    );
    builder.replace(fullRange, improved);
  });

  // Clear cache — the file has changed
  clearImprovedCode(doc.uri);

  vscode.window.setStatusBarMessage('$(check) AESTHENIXAI: Refactor applied.', 5000);
}
