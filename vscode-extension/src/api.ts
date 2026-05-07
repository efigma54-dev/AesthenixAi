import * as vscode from 'vscode';
import * as crypto from 'crypto';

export interface Issue {
  line: number;
  type: 'Bug' | 'Performance' | 'Security' | 'Style' | 'Maintainability' | 'General';
  message: string;
  fix?: string;   // short one-line fix hint for per-issue code actions
}

export interface ReviewResult {
  score: number;
  issues: Issue[];
  suggestions: string[];
  improvedCode: string;
  parsedInfo?: {
    methodCount: number;
    cyclomaticComplexity: number;
    nestedLoopCount: number;
    longMethodCount: number;
    hasExceptionHandling: boolean;
  };
  fromCache?: boolean;
  fromRules?: boolean;  // true when result came from local rule engine (stage 1)
}

export interface CachedEntry {
  result: ReviewResult;
  timestamp: number;
}

export class ApiError extends Error {
  constructor(
    public readonly type: 'network' | 'timeout' | 'server' | 'validation' | 'ratelimit' | 'auth',
    message: string
  ) {
    super(message);
    this.name = 'ApiError';
  }
}

// ── Output channel ─────────────────────────────────────────
// Single shared channel — created once, reused everywhere
let _channel: vscode.OutputChannel | undefined;
export function getOutputChannel(): vscode.OutputChannel {
  if (!_channel) _channel = vscode.window.createOutputChannel('AESTHENIXAI');
  return _channel;
}
export function log(msg: string) {
  getOutputChannel().appendLine(`[${new Date().toISOString()}] ${msg}`);
}

// ── Config ─────────────────────────────────────────────────
export function getConfig() {
  const cfg = vscode.workspace.getConfiguration('aesthenixai');
  return {
    baseUrl:   cfg.get<string>('backendUrl', 'http://localhost:8082/api'),
    timeoutMs: cfg.get<number>('timeoutMs', 180_000),
    mode:      cfg.get<string>('mode', 'hybrid') as 'ai' | 'offline' | 'hybrid',
  };
}

// ── Supported languages ────────────────────────────────────
export const SUPPORTED_LANGUAGES: Record<string, string> = {
  java:       'java',
  javascript: 'javascript',
  typescript: 'typescript',
  python:     'python',
};

export function isSupportedLanguage(langId: string): boolean {
  return langId in SUPPORTED_LANGUAGES;
}

/**
 * Normalize language ID for the backend.
 * TypeScript is analyzed as JavaScript — same rules, same prompt.
 */
export function normalizeLanguage(langId: string): string {
  if (langId === 'typescript') return 'javascript';
  return SUPPORTED_LANGUAGES[langId] ?? 'java';
}

/** MD5 hash of code — used as cache key */
export function hashCode(code: string): string {
  return crypto.createHash('md5').update(code).digest('hex');
}

/** Normalize raw API response — never returns undefined fields */
function normalize(raw: Record<string, unknown>): ReviewResult {
  return {
    score:        typeof raw.score === 'number' ? Math.max(0, Math.min(100, raw.score)) : 0,
    issues:       Array.isArray(raw.issues)      ? raw.issues as Issue[]       : [],
    suggestions:  Array.isArray(raw.suggestions) ? raw.suggestions as string[] : [],
    improvedCode: typeof raw.improvedCode === 'string' ? raw.improvedCode : '',
    parsedInfo:   raw.parsedInfo as ReviewResult['parsedInfo'] ?? undefined,
  };
}

// ── Local rule engine (Stage 1 — instant, <5ms) ────────────

export interface RuleMatch {
  pattern:    RegExp;
  type:       Issue['type'];
  message:    string;
  fix:        string;
  confidence: number;  // 0.0–1.0 — shown in hover, used for highlight intensity
}

const JAVA_RULES: RuleMatch[] = [
  {
    pattern:    /\bfor\s*\([^)]*\)\s*\{[^{}]*\+=[^;]+;/s,
    type:       'Performance',
    message:    'String concatenation inside a loop — use StringBuilder.',
    fix:        'StringBuilder sb = new StringBuilder(); sb.append(value);',
    confidence: 0.95,
  },
  {
    pattern:    /catch\s*\(\s*\w[\w.]*\s+\w+\s*\)\s*\{\s*\}/,
    type:       'Bug',
    message:    'Empty catch block — exception is silently swallowed.',
    fix:        'log.error("Unexpected error", e); throw new RuntimeException(e);',
    confidence: 0.98,
  },
  {
    pattern:    /catch\s*\(\s*Exception\s+/,
    type:       'Security',
    message:    'Catching generic Exception — catch specific exceptions instead.',
    fix:        'catch (SpecificException e) { ... }',
    confidence: 0.85,
  },
  {
    pattern:    /for\s*\([^)]*\)\s*\{[^{}]*for\s*\(/s,
    type:       'Performance',
    message:    'Nested loop detected — O(n²) or worse. Consider extracting inner logic.',
    fix:        'private void processItems(List<Item> items) { ... }',
    confidence: 0.90,
  },
  {
    pattern:    /System\.out\.print/,
    type:       'Style',
    message:    'System.out.print in production code — use a logger instead.',
    fix:        'private static final Logger log = LoggerFactory.getLogger(ClassName.class);',
    confidence: 0.99,
  },
  {
    pattern:    /[^!]=\s*null\b|\bnull\s*==/,
    type:       'Bug',
    message:    'Null check with == — consider Objects.isNull() or Optional.',
    fix:        'Objects.isNull(value)  // or  Optional.ofNullable(value)',
    confidence: 0.80,
  },
  {
    pattern:    /new\s+\w+\[\s*\d{4,}\s*\]/,
    type:       'Performance',
    message:    'Large fixed-size array allocation — consider using a List or dynamic structure.',
    fix:        'List<Type> list = new ArrayList<>();',
    confidence: 0.75,
  },
];

const JS_TS_RULES: RuleMatch[] = [
  {
    pattern:    /\bvar\s+\w/,
    type:       'Style',
    message:    'Avoid var — use const or let instead.',
    fix:        'const value = ...;  // or let if reassigned',
    confidence: 0.99,
  },
  {
    pattern:    /[^=!<>]==[^=]/,
    type:       'Bug',
    message:    'Loose equality (==) — use strict equality (===) to avoid type coercion.',
    fix:        'if (value === expected) { ... }',
    confidence: 0.90,
  },
  {
    pattern:    /console\.(log|warn|error)\s*\(/,
    type:       'Style',
    message:    'console statement in production code — remove or use a logger.',
    fix:        '// Remove console statement or use a logging library',
    confidence: 0.85,
  },
  {
    pattern:    /catch\s*\(\s*\w+\s*\)\s*\{\s*\}/,
    type:       'Bug',
    message:    'Empty catch block — error is silently swallowed.',
    fix:        'catch (err) { console.error("Unexpected error:", err); throw err; }',
    confidence: 0.98,
  },
  {
    pattern:    /for\s*\(\s*\w+\s+in\s+/,
    type:       'Bug',
    message:    'for...in on arrays iterates keys, not values — use for...of or forEach.',
    fix:        'for (const item of array) { ... }',
    confidence: 0.88,
  },
  {
    pattern:    /==\s*null\b|\bnull\s*==/,
    type:       'Bug',
    message:    'Loose null check — use === null or nullish coalescing (??).',
    fix:        'if (value === null || value === undefined) { ... }',
    confidence: 0.80,
  },
];

const PYTHON_RULES: RuleMatch[] = [
  {
    pattern:    /except\s*:/,
    type:       'Bug',
    message:    'Bare except clause — catches all exceptions including SystemExit.',
    fix:        'except Exception as e:  # catch specific exception',
    confidence: 0.98,
  },
  {
    pattern:    /\bprint\s*\(/,
    type:       'Style',
    message:    'print() in production code — use logging module instead.',
    fix:        'import logging; logging.info("message")',
    confidence: 0.85,
  },
  {
    pattern:    /==\s*None\b|\bNone\s*==/,
    type:       'Bug',
    message:    'Comparison to None with == — use "is None" instead.',
    fix:        'if value is None:',
    confidence: 0.99,
  },
  {
    pattern:    /\beval\s*\(/,
    type:       'Security',
    message:    'eval() is dangerous — can execute arbitrary code.',
    fix:        '# Use ast.literal_eval() for safe evaluation of literals',
    confidence: 0.99,
  },
  {
    pattern:    /\bexec\s*\(/,
    type:       'Security',
    message:    'exec() is dangerous — can execute arbitrary code.',
    fix:        '# Avoid exec(); use explicit function calls instead',
    confidence: 0.99,
  },
  {
    pattern:    /open\s*\([^)]+\)(?!\s*as\b)/,
    type:       'Bug',
    message:    'File opened without context manager — use "with open(...) as f:" to ensure it closes.',
    fix:        'with open(filename, "r") as f:\n    content = f.read()',
    confidence: 0.80,
  },
];

const RULES_BY_LANGUAGE: Record<string, RuleMatch[]> = {
  java:       JAVA_RULES,
  javascript: JS_TS_RULES,
  typescript: JS_TS_RULES,
  python:     PYTHON_RULES,
};

/**
 * Run local rules for the given language.
 * Finds ALL occurrences per rule (not just the first line).
 * Returns issues sorted by confidence descending.
 */
export function runLocalRules(code: string, language = 'java'): Issue[] {
  const normalized = normalizeLanguage(language);
  const rules      = RULES_BY_LANGUAGE[normalized] ?? JAVA_RULES;
  const lines      = code.split('\n');
  const issues: Issue[] = [];

  for (const rule of rules) {
    // Reset lastIndex for global patterns
    rule.pattern.lastIndex = 0;

    // Find all matching lines (not just the first)
    const matchedLines: number[] = [];
    for (let i = 0; i < lines.length; i++) {
      rule.pattern.lastIndex = 0;
      if (rule.pattern.test(lines[i])) matchedLines.push(i + 1);
    }

    // If no per-line match, try multi-line match and use first line
    if (matchedLines.length === 0) {
      rule.pattern.lastIndex = 0;
      if (!rule.pattern.test(code)) continue;
      matchedLines.push(1);
    }

    // Emit one issue per matched line (cap at 3 to avoid noise)
    for (const lineNum of matchedLines.slice(0, 3)) {
      issues.push({
        line:    lineNum,
        type:    rule.type,
        message: `${rule.message} (confidence: ${Math.round(rule.confidence * 100)}%)`,
        fix:     rule.fix,
      });
    }
  }

  // Sort by confidence descending so highest-confidence issues appear first
  return issues.sort((a, b) => {
    const confA = parseFloat(a.message.match(/(\d+)%/)?.[1] ?? '0');
    const confB = parseFloat(b.message.match(/(\d+)%/)?.[1] ?? '0');
    return confB - confA;
  });
}

// ── Remote API ─────────────────────────────────────────────

/**
 * POST /api/review with timeout and cancellation support.
 * Pass a CancellationToken to abort when the user cancels the progress dialog.
 */
export async function reviewCode(
  code: string,
  cancelToken?: vscode.CancellationToken,
  language = 'java',
): Promise<ReviewResult> {
  const { baseUrl, timeoutMs } = getConfig();
  log(`reviewCode: POST ${baseUrl}/review (${code.length} chars, lang=${language})`);

  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), timeoutMs);
  const cancelDisposable = cancelToken?.onCancellationRequested(() => controller.abort());

  try {
    const res = await fetch(`${baseUrl}/review`, {
      method:  'POST',
      headers: { 'Content-Type': 'application/json' },
      body:    JSON.stringify({ code, language }),
      signal:  controller.signal,
    });
    clearTimeout(timer);
    cancelDisposable?.dispose();

    if (!res.ok) {
      const body = await res.json().catch(() => ({})) as Record<string, string>;
      const msg  = body.error ?? `HTTP ${res.status}`;
      log(`reviewCode: error ${res.status} — ${msg}`);
      if (res.status === 400) throw new ApiError('validation', msg);
      if (res.status === 401) throw new ApiError('auth',       msg);
      if (res.status === 429) throw new ApiError('ratelimit',  msg);
      throw new ApiError('server', msg);
    }

    const raw = await res.json() as Record<string, unknown>;
    const result = normalize(raw);
    log(`reviewCode: success — score=${result.score} issues=${result.issues.length}`);
    return result;

  } catch (err) {
    clearTimeout(timer);
    cancelDisposable?.dispose();
    if (err instanceof ApiError) throw err;
    if ((err as Error).name === 'AbortError') {
      if (cancelToken?.isCancellationRequested) {
        log('reviewCode: cancelled by user');
        throw new ApiError('timeout', 'Analysis cancelled.');
      }
      log(`reviewCode: timed out after ${timeoutMs / 1000}s`);
      throw new ApiError('timeout', `Request timed out after ${timeoutMs / 1000}s`);
    }
    log(`reviewCode: network error — ${(err as Error).message}`);
    throw new ApiError('network',
      `Cannot reach backend at ${baseUrl}. Make sure it is running (mvn spring-boot:run).`);
  }
}

/** GET /api/health — resolves true when backend is reachable */
export async function checkHealth(): Promise<boolean> {
  const { baseUrl } = getConfig();
  try {
    const res = await fetch(`${baseUrl}/health`, { signal: AbortSignal.timeout(3_000) });
    log(`checkHealth: ${res.ok ? 'OK' : 'FAIL'} (${res.status})`);
    return res.ok;
  } catch {
    log('checkHealth: unreachable');
    return false;
  }
}
