import * as vscode from 'vscode';
import {
  reviewCode, checkHealth, hashCode, runLocalRules, getConfig, normalizeLanguage,
  ApiError, ReviewResult, CachedEntry, log,
  SUPPORTED_LANGUAGES, isSupportedLanguage,
} from './api';
import { initDecorations, disposeDecorations, applyIssues, clearIssues } from './diagnostics';
import { showResultsPanel } from './panel';
import { AesthenixCodeActionProvider } from './codeActions';
import { applyRefactor, cacheImprovedCode, clearImprovedCode } from './refactor';
import {
  AesthenixInlineCompletionProvider,
  applyInlineFixes, clearInlineFixes,
  getFixForLine, rejectFixForLine,
} from './inlineCompletion';
import { recordRun, logSnapshot } from './metrics';

const diagnosticCollection = vscode.languages.createDiagnosticCollection('aesthenixai');

const MAX_CODE_CHARS   = 8_000;
const CACHE_KEY_PREFIX = 'aesthenixai.cache.';
const CACHE_TTL_MS     = 5 * 60 * 1000;  // 5 minutes

// Track previous code per document for partial re-analysis
const previousCode = new Map<string, string>();

export function activate(context: vscode.ExtensionContext) {
  initDecorations();
  log('Extension activated');

  // ── First-run onboarding ───────────────────────────────────
  const firstRun = context.globalState.get<boolean>('aesthenix.firstRun', true);
  if (firstRun) {
    context.globalState.update('aesthenix.firstRun', false);
    vscode.window.showInformationMessage(
      'Welcome to AESTHENIXAI! Open any Java, JS, TS, or Python file and run "Analyze" to get started.',
      'Open Settings',
      'Dismiss',
    ).then(action => {
      if (action === 'Open Settings')
        vscode.commands.executeCommand('workbench.action.openSettings', 'aesthenixai');
    });
  }

  // ── Commands ───────────────────────────────────────────────
  context.subscriptions.push(
    vscode.commands.registerCommand('aesthenix.analyze',
      () => runAnalysis(context))
  );
  context.subscriptions.push(
    vscode.commands.registerCommand('aesthenix.analyzeSelection',
      () => runAnalysis(context, true))
  );
  context.subscriptions.push(
    vscode.commands.registerCommand('aesthenix.clearDiagnostics', () => {
      const editor = vscode.window.activeTextEditor;
      if (editor) {
        clearIssues(editor, diagnosticCollection);
        clearInlineFixes(editor.document.uri);
      }
    })
  );
  context.subscriptions.push(
    vscode.commands.registerCommand('aesthenix.applyRefactor',
      (doc?: vscode.TextDocument) => applyRefactor(context, doc))
  );

  // ── Accept fix: apply the inline suggestion directly ───────
  context.subscriptions.push(
    vscode.commands.registerCommand('aesthenix.acceptFix', async () => {
      const editor = vscode.window.activeTextEditor;
      if (!editor) return;
      const line1 = editor.selection.active.line + 1;
      const fix   = getFixForLine(editor.document.uri, line1);
      if (!fix) {
        vscode.window.showInformationMessage('AESTHENIXAI: No fix available for this line.');
        return;
      }
      const lineText = editor.document.lineAt(editor.selection.active.line);
      const indent   = lineText.text.match(/^(\s*)/)?.[1] ?? '';
      const pos      = lineText.range.end;
      await editor.edit(b => b.insert(pos, `\n${indent}// ✨ AESTHENIXAI fix:\n${indent}${fix}`));
      rejectFixForLine(editor.document.uri, line1); // consume — don't show again
      vscode.window.setStatusBarMessage('$(check) AESTHENIXAI: Fix applied.', 4_000);
      log(`acceptFix: applied fix at line ${line1}`);
    })
  );

  // ── Reject fix: dismiss the suggestion for this line ───────
  context.subscriptions.push(
    vscode.commands.registerCommand('aesthenix.rejectFix', () => {
      const editor = vscode.window.activeTextEditor;
      if (!editor) return;
      const line1 = editor.selection.active.line + 1;
      rejectFixForLine(editor.document.uri, line1);
      vscode.window.setStatusBarMessage('AESTHENIXAI: Suggestion dismissed.', 3_000);
    })
  );

  // ── Show metrics in output channel ─────────────────────────
  context.subscriptions.push(
    vscode.commands.registerCommand('aesthenix.showMetrics', () => {
      logSnapshot();
      vscode.commands.executeCommand('workbench.action.output.toggleOutput');
    })
  );

  // ── Code action provider (lightbulb quick-fixes) ───────────
  const supportedSelector = Object.keys(SUPPORTED_LANGUAGES).map(lang => ({
    language: lang, scheme: 'file',
  }));
  context.subscriptions.push(
    vscode.languages.registerCodeActionsProvider(
      supportedSelector,
      new AesthenixCodeActionProvider(),
      { providedCodeActionKinds: AesthenixCodeActionProvider.providedCodeActionKinds }
    )
  );

  // ── Inline completion provider (Copilot-style ghost text) ──
  context.subscriptions.push(
    vscode.languages.registerInlineCompletionItemProvider(
      supportedSelector,
      new AesthenixInlineCompletionProvider()
    )
  );

  // ── Auto-analyze on save (opt-in) ──────────────────────────
  context.subscriptions.push(
    vscode.workspace.onDidSaveTextDocument(async (doc) => {
      const cfg = vscode.workspace.getConfiguration('aesthenixai');
      if (!cfg.get<boolean>('autoAnalyzeOnSave', false)) return;
      if (!isSupportedLanguage(doc.languageId)) return;
      const editor = vscode.window.visibleTextEditors.find(e => e.document === doc);
      if (editor) await analyze(context, editor, doc.getText(), doc.languageId);
    })
  );

  // ── Clear caches on document change ────────────────────────
  context.subscriptions.push(
    vscode.workspace.onDidChangeTextDocument(e => {
      if (!isSupportedLanguage(e.document.languageId)) return;
      clearImprovedCode(e.document.uri);
      clearInlineFixes(e.document.uri);
      const hash = hashCode(e.document.getText().slice(0, MAX_CODE_CHARS) + e.document.languageId);
      context.globalState.update(CACHE_KEY_PREFIX + hash, undefined);
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

  const langId = editor.document.languageId;
  if (!isSupportedLanguage(langId)) {
    vscode.window.showWarningMessage(
      `AESTHENIXAI: ${langId} is not supported. Supported: Java, JavaScript, TypeScript, Python.`
    );
    return;
  }

  const code = selectionOnly && !editor.selection.isEmpty
    ? editor.document.getText(editor.selection)
    : editor.document.getText();
  if (!code.trim()) { vscode.window.showWarningMessage('AESTHENIXAI: File is empty.'); return; }
  await analyze(context, editor, code, langId);
}

async function analyze(
  context: vscode.ExtensionContext,
  editor: vscode.TextEditor,
  rawCode: string,
  language = 'java',
) {
  const filename  = editor.document.fileName.split(/[\\/]/).pop() ?? 'file.java';
  const docUriStr = editor.document.uri.toString();
  const startMs   = Date.now();

  // Truncate large files
  let code = rawCode;
  let truncated = false;
  if (code.length > MAX_CODE_CHARS) {
    code = code.slice(0, MAX_CODE_CHARS);
    truncated = true;
  }

  // ── Partial re-analysis: detect changed lines ──────────────
  // If we have a previous result and only a few lines changed,
  // we can skip re-running AI on unchanged sections.
  const prev = previousCode.get(docUriStr);
  const changedLines = prev ? getChangedLines(prev, code) : null;
  const isMinorEdit  = changedLines !== null && changedLines.length <= 5;
  previousCode.set(docUriStr, code);

  // ── Stage 0: TTL-aware cache check ────────────────────────
  const cacheKey = CACHE_KEY_PREFIX + hashCode(code + language);
  const cached   = context.globalState.get<CachedEntry>(cacheKey);
  if (cached && (Date.now() - cached.timestamp) < CACHE_TTL_MS) {
    log(`analyze: cache hit for ${filename} (age ${Math.round((Date.now() - cached.timestamp) / 1000)}s)`);
    applyIssues(editor, diagnosticCollection, cached.result.issues);
    applyInlineFixes(editor.document.uri, cached.result.issues);
    showResultsPanel(context, filename, { ...cached.result, fromCache: true });
    if (cached.result.improvedCode) cacheImprovedCode(editor.document.uri, cached.result.improvedCode);
    const icon = cached.result.score >= 75 ? '$(pass)' : cached.result.score >= 50 ? '$(warning)' : '$(error)';
    vscode.window.setStatusBarMessage(
      `${icon} AESTHENIXAI: ${filename} — ${cached.result.score}/100 ⚡ cached`,
      8_000
    );
    if (truncated) vscode.window.showWarningMessage(
      `AESTHENIXAI: File is large — analyzed first ${MAX_CODE_CHARS} characters.`
    );
    recordRun({ latencyMs: Date.now() - startMs, fromCache: true, fromRules: false });
    return;
  }

  // ── Stage 1: instant local rules (<5ms) ───────────────────
  const { mode } = getConfig();
  const ruleIssues = runLocalRules(code, language);
  if (ruleIssues.length > 0) {
    log(`analyze: stage-1 found ${ruleIssues.length} rule issues`);
    applyIssues(editor, diagnosticCollection, ruleIssues);
    applyInlineFixes(editor.document.uri, ruleIssues);
    showResultsPanel(context, filename, {
      score: 0, issues: ruleIssues, suggestions: [], improvedCode: '', fromRules: true,
    });
    vscode.window.setStatusBarMessage(
      `$(warning) AESTHENIXAI: ${filename} — ${ruleIssues.length} quick issue${ruleIssues.length !== 1 ? 's' : ''} · AI analyzing…`,
      15_000
    );
  }

  // ── Stage 2: AI analysis (async, cancellable) ─────────────
  // For minor edits (≤5 changed lines), skip AI and keep rule results.
  // This makes the extension feel instant for small changes.
  if (isMinorEdit && ruleIssues.length === 0) {
    log(`analyze: minor edit (${changedLines!.length} lines changed) — skipping AI`);
    vscode.window.setStatusBarMessage(
      `$(info) AESTHENIXAI: Minor edit — run Analyze to refresh AI results`,
      6_000
    );
    recordRun({ latencyMs: Date.now() - startMs, fromCache: false, fromRules: true });
    return;
  }

  // Offline mode — rule results only, no AI call
  if (mode === 'offline') {
    log(`analyze: offline mode — skipping AI`);
    if (ruleIssues.length === 0) {
      vscode.window.setStatusBarMessage(`$(pass) AESTHENIXAI: ${filename} — no issues found (offline mode)`, 6_000);
    }
    recordRun({ latencyMs: Date.now() - startMs, fromCache: false, fromRules: true });
    return;
  }

  await vscode.window.withProgress(
    {
      location: vscode.ProgressLocation.Notification,
      title: `AESTHENIXAI: AI analyzing ${filename}…`,
      cancellable: true,
    },
    async (progress, cancelToken) => {
      if (truncated) vscode.window.showWarningMessage(
        `AESTHENIXAI: File is large — analyzing first ${MAX_CODE_CHARS} characters.`
      );

      progress.report({ message: 'Checking server…', increment: 10 });
      const healthy = await checkHealth();
      if (!healthy) {
        const cfg = vscode.workspace.getConfiguration('aesthenixai');
        const url = cfg.get<string>('backendUrl', 'http://localhost:8082/api');
        log(`analyze: backend unreachable at ${url}`);
        recordRun({ latencyMs: Date.now() - startMs, fromCache: false, fromRules: true, error: 'network' });

        if (ruleIssues.length > 0) {
          vscode.window.showWarningMessage(
            `AESTHENIXAI: Backend not reachable — showing rule-based results only. Run: mvn spring-boot:run`,
            'Open Settings'
          ).then(a => {
            if (a === 'Open Settings')
              vscode.commands.executeCommand('workbench.action.openSettings', 'aesthenixai.backendUrl');
          });
        } else {
          const a = await vscode.window.showErrorMessage(
            `AESTHENIXAI: Backend not reachable at ${url}.\n\nRun: mvn spring-boot:run`,
            'Open Settings',
          );
          if (a === 'Open Settings')
            vscode.commands.executeCommand('workbench.action.openSettings', 'aesthenixai.backendUrl');
        }
        return;
      }

      if (cancelToken.isCancellationRequested) return;
      progress.report({ message: 'Sending to AI…', increment: 20 });

      try {
        const result = await reviewCode(code, cancelToken, normalizeLanguage(language));
        if (cancelToken.isCancellationRequested) return;

        progress.report({ message: 'Applying results…', increment: 60 });

        await context.globalState.update(cacheKey, {
          result, timestamp: Date.now(),
        } satisfies CachedEntry);

        applyIssues(editor, diagnosticCollection, result.issues);
        applyInlineFixes(editor.document.uri, result.issues);
        showResultsPanel(context, filename, result);
        if (result.improvedCode) cacheImprovedCode(editor.document.uri, result.improvedCode);

        const latencyMs = Date.now() - startMs;
        recordRun({ latencyMs, fromCache: false, fromRules: false });
        log(`analyze: complete in ${latencyMs}ms — score=${result.score}`);

        const icon = result.score >= 75 ? '$(pass)' : result.score >= 50 ? '$(warning)' : '$(error)';
        const hasImproved = Boolean(result.improvedCode.trim());
        vscode.window.setStatusBarMessage(
          `${icon} AESTHENIXAI: ${filename} — ${result.score}/100, ${result.issues.length} issue${result.issues.length !== 1 ? 's' : ''}${hasImproved ? ' · Apply refactor?' : ''}`,
          10_000
        );

        if (result.issues.length > 0 && hasImproved && !cancelToken.isCancellationRequested) {
          const pick = await vscode.window.showInformationMessage(
            `AESTHENIXAI: ${result.issues.length} issue${result.issues.length !== 1 ? 's' : ''} in ${filename}. Apply AI refactor?`,
            'Apply Refactor', 'Dismiss',
          );
          if (pick === 'Apply Refactor') applyRefactor(context, editor.document);
        }

      } catch (err) {
        if (cancelToken.isCancellationRequested) return;
        const msg      = err instanceof ApiError ? err.message : 'Analysis failed.';
        const errType  = err instanceof ApiError ? err.type : 'unknown';
        log(`analyze: AI error — ${msg}`);
        recordRun({ latencyMs: Date.now() - startMs, fromCache: false, fromRules: ruleIssues.length > 0, error: errType });

        if (ruleIssues.length > 0) {
          vscode.window.showWarningMessage(
            `AESTHENIXAI: AI unavailable (${msg}) — showing rule-based results.`,
            'Retry'
          ).then(a => { if (a === 'Retry') analyze(context, editor, rawCode); });
        } else {
          const a = await vscode.window.showErrorMessage(`AESTHENIXAI: ${msg}`, 'Retry');
          if (a === 'Retry') analyze(context, editor, rawCode);
        }
      }
    }
  );
}

// ── Partial re-analysis helper ─────────────────────────────

/**
 * Returns the 1-indexed line numbers that changed between two code strings.
 * Uses a simple line-by-line diff — fast enough for files up to 8K chars.
 */
function getChangedLines(prev: string, next: string): number[] {
  const prevLines = prev.split('\n');
  const nextLines = next.split('\n');
  const changed: number[] = [];
  const maxLen = Math.max(prevLines.length, nextLines.length);
  for (let i = 0; i < maxLen; i++) {
    if (prevLines[i] !== nextLines[i]) changed.push(i + 1);
  }
  return changed;
}
