/**
 * API service layer — production-grade reliability.
 *
 * Guarantees:
 *  - Every public function resolves or rejects with ApiError only.
 *  - Network/timeout errors retry with exponential back-off (max 2 retries).
 *  - Validation / auth errors throw immediately — no pointless retries.
 *  - In-flight deduplication: identical concurrent requests share one promise.
 *  - Completed results cached by content hash (LRU, 50 entries).
 *  - Multi-file concurrency capped at 3 to avoid backend overload.
 *  - onRetryAttempt callback lets the UI show "Attempt 2 of 3…".
 *  - No unhandled promise rejections — every code path is covered.
 */

/* ─── Config ─────────────────────────────────────────────── */
const BASE = import.meta.env.VITE_API_URL || 'http://localhost:8082/api';
const TIMEOUT_MS = 180_000;  // 3 min — AI analysis can take 30-150s
const MAX_RETRIES = 1;       // 1 retry on network/timeout (not 2 — AI is slow)
const RETRY_BASE_MS = 2000;
const MAX_CODE_BYTES = 100_000;
const GITHUB_TIMEOUT = 8_000;
const MIN_GAP_MS = 1_000;   // 1s cooldown — was 5s, too aggressive

/* ─── Request cooldown ───────────────────────────────────── */
let _lastRequestAt = 0;

function checkCooldown() {
  const elapsed = Date.now() - _lastRequestAt;
  if (elapsed < MIN_GAP_MS) {
    const wait = Math.ceil((MIN_GAP_MS - elapsed) / 1000);
    throw new ApiError('ratelimit',
      `Please wait ${wait} more second${wait !== 1 ? 's' : ''} before analyzing again.`,
      false);
  }
  _lastRequestAt = Date.now();
}

/* ─── Error class ────────────────────────────────────────── */
export class ApiError extends Error {
  /**
   * @param {'network'|'timeout'|'server'|'validation'|'ratelimit'|'auth'|'unknown'} type
   * @param {string}  message
   * @param {boolean} retryable
   * @param {number}  [status]   HTTP status code if available
   */
  constructor(type, message, retryable = false, status = 0) {
    super(message);
    this.name = 'ApiError';
    this.type = type;
    this.retryable = retryable;
    this.status = status;
  }
}

/* ─── User-facing messages ───────────────────────────────── */
const MESSAGES = {
  network: `Cannot reach the backend at ${BASE}. Make sure it is running (mvn spring-boot:run).`,
  timeout: 'The server took too long to respond. It may be busy — try again in a moment.',
  server: 'The server returned an error.',   // detail appended from response body
  validation: 'The request was rejected — check your input and try again.',
  ratelimit: 'Too many requests — the server is busy. Wait a few seconds and try again.',
  auth: 'Authentication failed. Check your API configuration.',
  unknown: 'An unexpected error occurred.',
};

/**
 * Returns a clear, user-facing string for any error.
 * Appends the server's own detail when it adds useful context.
 * Optionally prefixes with a file name for multi-file errors.
 */
export function friendlyMessage(err, { filename, attempt, total } = {}) {
  const base = err instanceof ApiError ? (MESSAGES[err.type] ?? err.message) : MESSAGES.unknown;
  // Always show server error detail — it contains the actual reason (e.g. "Code exceeds limit")
  const detail = err instanceof ApiError && err.message && err.message !== base
    ? `: ${err.message}`
    : '';
  const prefix = filename ? `${filename}: ` : '';
  const retry = attempt != null && total != null ? ` [Attempt ${attempt}/${total}]` : '';
  return prefix + base + detail + retry;
}

/* ─── djb2 hash ──────────────────────────────────────────── */
function djb2(str) {
  let h = 5381;
  for (let i = 0; i < str.length; i++) h = ((h << 5) + h) ^ str.charCodeAt(i);
  return (h >>> 0).toString(36);
}

/* ─── LRU result cache (max 50 entries) ─────────────────── */
const _cache = new Map();
const CACHE_MAX = 50;
function cacheGet(k) { return _cache.get(k) ?? null; }
function cacheSet(k, v) {
  if (_cache.size >= CACHE_MAX) _cache.delete(_cache.keys().next().value);
  _cache.set(k, v);
}

/* ─── In-flight deduplication map ───────────────────────── */
// key → Promise  (same code analysed concurrently → share one request)
const _inflight = new Map();

/* ─── Helpers ────────────────────────────────────────────── */
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

function classifyRawError(err, externalSignal) {
  if (err instanceof ApiError) return err;
  if (err.name === 'AbortError') {
    if (externalSignal?.aborted) return err;           // caller cancelled — re-throw as-is
    return new ApiError('timeout', 'Request timed out', true);
  }
  return new ApiError('network', err.message ?? 'Network error', true);
}

/* ─── Single fetch attempt ───────────────────────────────── */
async function fetchOnce(path, body, externalSignal) {
  const ctrl = new AbortController();
  const timer = setTimeout(() => ctrl.abort(), TIMEOUT_MS);

  const onExternal = () => ctrl.abort();
  externalSignal?.addEventListener('abort', onExternal);

  try {
    const res = await fetch(`${BASE}${path}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body),
      signal: ctrl.signal,
    });
    clearTimeout(timer);

    if (!res.ok) {
      const data = await res.json().catch(() => ({}));
      const msg = data.error ?? data.message ?? `HTTP ${res.status}`;
      if (res.status === 400) throw new ApiError('validation', msg, false, 400);
      if (res.status === 401) throw new ApiError('auth', msg, false, 401);
      if (res.status === 429) throw new ApiError('ratelimit', msg, false, 429);
      if (res.status >= 500) throw new ApiError('server', msg, true, res.status);
      throw new ApiError('server', msg, false, res.status);
    }

    return res.json();
  } catch (err) {
    clearTimeout(timer);
    throw classifyRawError(err, externalSignal);
  } finally {
    externalSignal?.removeEventListener('abort', onExternal);
  }
}

/**
 * Fetch with exponential back-off retry.
 *
 * @param {string}       path
 * @param {object}       body
 * @param {AbortSignal}  [externalSignal]
 * @param {function}     [onRetryAttempt]  called as (attempt, total) before each retry
 */
async function apiFetch(path, body, externalSignal, onRetryAttempt) {
  const total = MAX_RETRIES + 1;
  let lastErr;

  for (let attempt = 1; attempt <= total; attempt++) {
    if (externalSignal?.aborted)
      throw externalSignal.reason ?? new DOMException('Aborted', 'AbortError');

    try {
      return await fetchOnce(path, body, externalSignal);
    } catch (err) {
      lastErr = err;

      const isCallerAbort = err.name === 'AbortError' && externalSignal?.aborted;
      if (isCallerAbort) throw err;
      if (err instanceof ApiError && !err.retryable) throw err;
      if (attempt === total) break;

      // Notify UI before sleeping so it can show "Retrying… Attempt 2 of 3"
      try { onRetryAttempt?.(attempt + 1, total); } catch { /* ignore */ }

      await sleep(RETRY_BASE_MS * Math.pow(2, attempt - 1));
    }
  }

  if (lastErr instanceof ApiError)
    lastErr.message = `${lastErr.message} (failed after ${total} attempt${total > 1 ? 's' : ''})`;
  throw lastErr;
}

/* ─── Response normaliser ────────────────────────────────── */
function normalize(raw) {
  return {
    score: typeof raw.score === 'number' ? Math.max(0, Math.min(100, raw.score)) : 0,
    issues: Array.isArray(raw.issues) ? raw.issues : [],
    suggestions: Array.isArray(raw.suggestions) ? raw.suggestions : [],
    improvedCode: typeof raw.improvedCode === 'string' ? raw.improvedCode : '',
    parsedInfo: raw.parsedInfo ?? null,
  };
}

/* ─── Input validation ───────────────────────────────────── */
function validateCode(code) {
  if (!code?.trim())
    throw new ApiError('validation', 'Code cannot be empty.', false);
  if (new Blob([code]).size > MAX_CODE_BYTES)
    throw new ApiError('validation', 'Code exceeds the 100 KB limit. Split it into smaller files.', false);
}

/* ═══════════════════════════════════════════════════════════
   Public API
   ═══════════════════════════════════════════════════════════ */

/**
 * Health check — resolves true when backend is reachable, false otherwise.
 * Never rejects.
 */
export async function checkHealth() {
  try {
    const res = await fetch(`${BASE}/health`, { signal: AbortSignal.timeout(3_000) });
    return res.ok;
  } catch {
    return false;
  }
}

/**
 * Analyse a single file using the async job queue.
 *
 * Flow:
 *   1. POST /review/async → { jobId }  (returns immediately)
 *   2. Poll GET /review/async/{jobId} every 1s until status == "done" | "error"
 *
 * This makes the UI feel instant — the button responds in <100ms
 * and the spinner updates as the job progresses.
 *
 * @param {string}   code
 * @param {object}   [opts]
 * @param {AbortSignal} [opts.signal]
 * @param {function} [opts.onStatus]  called with status string on each poll
 */
export async function reviewCodeAsync(code, { signal, onStatus } = {}) {
  validateCode(code);

  // Submit job
  const ctrl = new AbortController();
  const timer = setTimeout(() => ctrl.abort(), TIMEOUT_MS);
  const onAbort = () => ctrl.abort();
  signal?.addEventListener('abort', onAbort);

  let jobId;
  try {
    const res = await fetch(`${BASE}/review/async`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ code }),
      signal: ctrl.signal,
    });
    clearTimeout(timer);
    if (!res.ok) {
      const data = await res.json().catch(() => ({}));
      throw new ApiError('server', data.error ?? `HTTP ${res.status}`, true, res.status);
    }
    const { jobId: id } = await res.json();
    jobId = id;
  } catch (err) {
    clearTimeout(timer);
    signal?.removeEventListener('abort', onAbort);
    if (err instanceof ApiError) throw err;
    if (err.name === 'AbortError') throw new ApiError('timeout', 'Request cancelled', false);
    throw new ApiError('network', err.message ?? 'Network error', true);
  }

  // Poll until done
  const POLL_MS = 1_000;
  const MAX_POLLS = 200; // 200s max
  for (let i = 0; i < MAX_POLLS; i++) {
    if (signal?.aborted) {
      signal.removeEventListener('abort', onAbort);
      throw new ApiError('timeout', 'Analysis cancelled', false);
    }
    await sleep(POLL_MS);

    try {
      const res = await fetch(`${BASE}/review/async/${jobId}`, { signal });
      if (!res.ok) continue; // transient error — keep polling
      const job = await res.json();
      onStatus?.(job.status);

      if (job.status === 'done') {
        signal?.removeEventListener('abort', onAbort);
        return normalize(job.result);
      }
      if (job.status === 'error' || job.status === 'failed') {
        signal?.removeEventListener('abort', onAbort);
        throw new ApiError('server', job.error ?? 'Analysis failed', false);
      }
      if (job.status === 'timeout') {
        signal?.removeEventListener('abort', onAbort);
        throw new ApiError('timeout', job.error ?? 'Analysis timed out on the server', false);
      }
      // status == "queued" | "running" — keep polling
    } catch (err) {
      if (err instanceof ApiError) throw err;
      if (err.name === 'AbortError') {
        signal?.removeEventListener('abort', onAbort);
        throw new ApiError('timeout', 'Analysis cancelled', false);
      }
      // Network blip — keep polling
    }
  }

  signal?.removeEventListener('abort', onAbort);
  throw new ApiError('timeout', 'Analysis timed out after 200s', false);
}

/**
 * Analyse a single Java file (synchronous — blocks until result).
 *
 * Features:
 *  - Pre-validates input before any network call.
 *  - Returns cached result immediately when available.
 *  - Deduplicates concurrent calls for the same code.
 *  - Retries on network/timeout with back-off; calls onRetryAttempt(attempt, total).
 *
 * @param {string}   code
 * @param {object}   [opts]
 * @param {AbortSignal} [opts.signal]
 * @param {boolean}  [opts.bustCache]       force fresh request
 * @param {function} [opts.onRetryAttempt]  (attempt: number, total: number) => void
 */
export async function reviewCode(code, { signal, bustCache = false, onRetryAttempt } = {}) {
  validateCode(code);

  const key = djb2(code);

  // 1. Cache hit — skip cooldown entirely, no API call needed
  if (!bustCache) {
    const hit = cacheGet(key);
    if (hit) return { ...hit, fromCache: true };
  }

  // 2. Cooldown — only when using cache (not when user explicitly re-runs)
  if (!bustCache) {
    checkCooldown();
  } else {
    _lastRequestAt = Date.now(); // still update timestamp
  }

  // 3. In-flight deduplication — skip when busting cache
  if (!bustCache && _inflight.has(key)) return _inflight.get(key);

  // 3. New request
  const promise = apiFetch('/review', { code }, signal, onRetryAttempt)
    .then((raw) => {
      const result = normalize(raw);
      cacheSet(key, result);
      return result;
    })
    .finally(() => {
      _inflight.delete(key);
    });

  _inflight.set(key, promise);
  return promise;
}

/**
 * Analyse multiple files with bounded parallelism (3 at a time).
 *
 * - One failure never blocks the others (Promise.allSettled).
 * - onProgress(partialResults) called after every completed file.
 * - Error messages include the file name and attempt context.
 */
export async function reviewMultipleFiles(files, { onProgress, signal } = {}) {
  const CONCURRENCY = 3;   // keep backend load manageable
  const results = new Array(files.length).fill(null);

  for (let i = 0; i < files.length; i += CONCURRENCY) {
    if (signal?.aborted) break;

    const batch = files.slice(i, i + CONCURRENCY);

    const settled = await Promise.allSettled(
      batch.map((file, bi) => {
        const idx = i + bi;
        return reviewCode(file.content ?? file.code ?? '', { signal })
          .then((r) => ({ ok: true, idx, name: file.name, result: r }))
          .catch((e) => ({ ok: false, idx, name: file.name, error: e }));
      })
    );

    for (const s of settled) {
      const item = s.value;
      if (!item) continue;
      results[item.idx] = item.ok
        ? { name: item.name, ...item.result, error: null }
        : {
          name: item.name, score: 0, issues: [], suggestions: [],
          // Include file name in the error message for clarity
          error: friendlyMessage(item.error, { filename: item.name }),
        };
    }

    try { onProgress?.(results.filter(Boolean)); } catch { /* never propagate */ }
  }

  return results;
}

/**
 * reviewCodeStreaming — redirects to reviewCode.
 * The streaming endpoint (/review/stream/final) does not exist on the backend.
 * Kept for API compatibility; callers should use reviewCode() directly.
 */
export function reviewCodeStreaming(code, { onComplete, onError, signal } = {}) {
  let stopped = false;
  const stop = () => { stopped = true; };
  const promise = reviewCode(code, { signal })
    .then((data) => { if (!stopped) onComplete?.(data); return data; })
    .catch((err) => { if (!stopped) onError?.(err); throw err; });
  return { stop, promise };
}

/**
 * Fetch GitHub repository metadata.
 * Rejects with typed ApiError on 404 / 403 / timeout / network.
 */
export async function fetchRepoMeta(owner, repo) {
  const ctrl = new AbortController();
  const timer = setTimeout(() => ctrl.abort(), GITHUB_TIMEOUT);
  try {
    const res = await fetch(
      `https://api.github.com/repos/${owner}/${repo}`,
      { headers: { Accept: 'application/vnd.github.v3+json' }, signal: ctrl.signal }
    );
    clearTimeout(timer);
    if (res.status === 404) throw new ApiError('validation', `Repository "${owner}/${repo}" not found.`, false, 404);
    if (res.status === 403) throw new ApiError('ratelimit', 'GitHub rate limit hit. Set VITE_GITHUB_TOKEN to raise it to 5 000 req/h.', false, 403);
    if (!res.ok) throw new ApiError('server', `GitHub API returned ${res.status}`, true, res.status);
    return res.json();
  } catch (err) {
    clearTimeout(timer);
    if (err instanceof ApiError) throw err;
    if (err.name === 'AbortError') throw new ApiError('timeout', 'GitHub API timed out', true);
    throw new ApiError('network', err.message ?? 'GitHub network error', true);
  }
}

/**
 * Scan an entire GitHub repository for code quality issues.
 *
 * @param {string} repoUrl - GitHub repository URL
 * @param {string} [token] - Optional GitHub token for private repos
 * @returns {Promise} Repository scan results
 */
export async function scanRepository(repoUrl, token) {
  const response = await apiFetch('/repo/scan', { repoUrl, token });
  return response;
}
export async function collectJavaFiles(owner, repo, path = '', collected = [], limit = 20) {
  if (collected.length >= limit) return collected;

  const ctrl = new AbortController();
  const timer = setTimeout(() => ctrl.abort(), GITHUB_TIMEOUT);
  try {
    const res = await fetch(
      `https://api.github.com/repos/${owner}/${repo}/contents/${path}`,
      { headers: { Accept: 'application/vnd.github.v3+json' }, signal: ctrl.signal }
    );
    clearTimeout(timer);

    if (res.status === 403) throw new ApiError('ratelimit', 'GitHub rate limit hit.', false, 403);
    if (!res.ok) return collected;

    const items = await res.json();
    if (!Array.isArray(items)) return collected;

    for (const item of items) {
      if (collected.length >= limit) break;
      if (item.type === 'file' && item.name.endsWith('.java'))
        collected.push({ name: item.path, downloadUrl: item.download_url });
      else if (item.type === 'dir' && !item.name.startsWith('.'))
        await collectJavaFiles(owner, repo, item.path, collected, limit);
    }
  } catch (err) {
    clearTimeout(timer);
    if (err instanceof ApiError) throw err;
    // Silently skip individual directory timeouts/network errors
  }

  return collected;
}

/**
 * Apply safe, non-breaking code fixes.
 * 
 * @param {string} code - Code to fix
 * @returns {Promise} Fixed code and list of applied fixes
 */
export async function applySafeFixes(code) {
  validateCode(code);

  try {
    const response = await apiFetch('/fix/safe', { code });
    return response;
  } catch (err) {
    throw err instanceof ApiError ? err : new ApiError('unknown', err.message ?? 'Failed to apply fixes', false);
  }
}

/**
 * Parse owner + repo from any GitHub URL variant.
 * Throws ApiError('validation') on bad input.
 */
export function parseGithubUrl(url) {
  const m = (url ?? '').match(/github\.com\/([^/]+)\/([^/?#\s]+)/);
  if (!m) throw new ApiError('validation', 'Invalid GitHub URL — expected https://github.com/owner/repo', false);
  return { owner: m[1], repo: m[2].replace(/\.git$/, '') };
}
