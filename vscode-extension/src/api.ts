import * as vscode from 'vscode';

export interface Issue {
  line: number;
  type: 'Bug' | 'Performance' | 'Security' | 'Style' | 'Maintainability' | 'General';
  message: string;
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

function getConfig() {
  const cfg = vscode.workspace.getConfiguration('aesthenixai');
  return {
    baseUrl: cfg.get<string>('backendUrl', 'http://localhost:8080/api'),
    timeoutMs: cfg.get<number>('timeoutMs', 30_000),
  };
}

/** Normalize raw API response — never returns undefined fields */
function normalize(raw: Record<string, unknown>): ReviewResult {
  return {
    score:        typeof raw.score === 'number' ? Math.max(0, Math.min(100, raw.score)) : 0,
    issues:       Array.isArray(raw.issues)      ? raw.issues as Issue[]  : [],
    suggestions:  Array.isArray(raw.suggestions) ? raw.suggestions as string[] : [],
    improvedCode: typeof raw.improvedCode === 'string' ? raw.improvedCode : '',
    parsedInfo:   raw.parsedInfo as ReviewResult['parsedInfo'] ?? undefined,
  };
}

/** POST /api/review with timeout and typed error handling */
export async function reviewCode(code: string): Promise<ReviewResult> {
  const { baseUrl, timeoutMs } = getConfig();

  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), timeoutMs);

  try {
    const res = await fetch(`${baseUrl}/review`, {
      method:  'POST',
      headers: { 'Content-Type': 'application/json' },
      body:    JSON.stringify({ code }),
      signal:  controller.signal,
    });
    clearTimeout(timer);

    if (!res.ok) {
      const body = await res.json().catch(() => ({})) as Record<string, string>;
      const msg  = body.error ?? `HTTP ${res.status}`;
      if (res.status === 400) throw new ApiError('validation', msg);
      if (res.status === 401) throw new ApiError('auth',       msg);
      if (res.status === 429) throw new ApiError('ratelimit',  msg);
      throw new ApiError('server', msg);
    }

    const raw = await res.json() as Record<string, unknown>;
    return normalize(raw);

  } catch (err) {
    clearTimeout(timer);
    if (err instanceof ApiError) throw err;
    if ((err as Error).name === 'AbortError')
      throw new ApiError('timeout', `Request timed out after ${timeoutMs / 1000}s`);
    throw new ApiError('network',
      `Cannot reach backend at ${baseUrl}. Make sure it is running.`);
  }
}

/** GET /api/health — resolves true when backend is reachable */
export async function checkHealth(): Promise<boolean> {
  const { baseUrl } = getConfig();
  try {
    const res = await fetch(`${baseUrl}/health`, { signal: AbortSignal.timeout(3_000) });
    return res.ok;
  } catch {
    return false;
  }
}
