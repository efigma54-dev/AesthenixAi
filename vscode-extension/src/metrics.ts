import { log } from './api';

/**
 * Lightweight in-process metrics tracker.
 *
 * Tracks: total runs, avg latency, cache hits, errors by type.
 * Exposed via the AESTHENIXAI output channel on demand.
 * No external dependencies — no telemetry sent anywhere.
 */

interface MetricsSnapshot {
  totalRuns:    number;
  cacheHits:    number;
  ruleOnlyRuns: number;  // AI was unavailable, rule results only
  aiRuns:       number;
  errors:       number;
  avgLatencyMs: number;
  p90LatencyMs: number;
  errorsByType: Record<string, number>;
}

let totalRuns    = 0;
let cacheHits    = 0;
let ruleOnlyRuns = 0;
let aiRuns       = 0;
let errors       = 0;
const latencies: number[] = [];
const errorsByType: Record<string, number> = {};

export function recordRun(opts: {
  latencyMs:  number;
  fromCache:  boolean;
  fromRules:  boolean;  // AI unavailable, rule-only result
  error?:     string;   // error type if failed
}) {
  totalRuns++;
  if (opts.fromCache) {
    cacheHits++;
  } else if (opts.error) {
    errors++;
    errorsByType[opts.error] = (errorsByType[opts.error] ?? 0) + 1;
  } else if (opts.fromRules) {
    ruleOnlyRuns++;
  } else {
    aiRuns++;
    latencies.push(opts.latencyMs);
    // Keep only last 100 latencies for rolling percentile
    if (latencies.length > 100) latencies.shift();
  }
}

export function getSnapshot(): MetricsSnapshot {
  const sorted = [...latencies].sort((a, b) => a - b);
  const avg = sorted.length > 0
    ? Math.round(sorted.reduce((s, v) => s + v, 0) / sorted.length)
    : 0;
  const p90 = sorted.length > 0
    ? sorted[Math.floor(sorted.length * 0.9)]
    : 0;

  return {
    totalRuns, cacheHits, ruleOnlyRuns, aiRuns, errors,
    avgLatencyMs: avg,
    p90LatencyMs: p90,
    errorsByType: { ...errorsByType },
  };
}

export function logSnapshot() {
  const s = getSnapshot();
  log('── Metrics ──────────────────────────────────────────');
  log(`  Total runs:    ${s.totalRuns}`);
  log(`  Cache hits:    ${s.cacheHits} (${s.totalRuns > 0 ? Math.round(s.cacheHits / s.totalRuns * 100) : 0}%)`);
  log(`  AI runs:       ${s.aiRuns}`);
  log(`  Rule-only:     ${s.ruleOnlyRuns}`);
  log(`  Errors:        ${s.errors}`);
  log(`  Avg latency:   ${s.avgLatencyMs}ms`);
  log(`  p90 latency:   ${s.p90LatencyMs}ms`);
  if (Object.keys(s.errorsByType).length > 0) {
    log(`  Error types:   ${JSON.stringify(s.errorsByType)}`);
  }
  log('─────────────────────────────────────────────────────');
}
