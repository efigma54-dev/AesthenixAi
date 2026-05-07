package com.aicode.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * Metrics service — tracks code review and PR bot performance.
 *
 * Design:
 *  - AtomicLong counters for lifetime totals (lock-free, non-blocking)
 *  - ArrayDeque ring buffer (last 100 runs) for rolling latency percentiles
 *  - ConcurrentHashMap for per-repo counts (top-10 exposed)
 *  - Threshold alerts logged as WARN — no external infra needed
 *
 * All public methods are safe to call from multiple threads.
 */
@Service
public class MetricsService {

  private final ReviewQueueService reviewQueueService;
  private final AIUsageLimiter     aiUsageLimiter;
  private final FeedbackService    feedbackService;

  public MetricsService(ReviewQueueService reviewQueueService,
                        AIUsageLimiter aiUsageLimiter,
                        FeedbackService feedbackService) {
    this.reviewQueueService = reviewQueueService;
    this.aiUsageLimiter     = aiUsageLimiter;
    this.feedbackService    = feedbackService;
  }

  private static final Logger log = LoggerFactory.getLogger(MetricsService.class);

  // ── Code review (direct API) ───────────────────────────────
  private final AtomicLong totalRequests    = new AtomicLong(0);
  private final AtomicLong totalRequestTime = new AtomicLong(0);
  private final AtomicLong cacheHits        = new AtomicLong(0);
  private final AtomicLong cacheMisses      = new AtomicLong(0);

  // ── AI service ─────────────────────────────────────────────
  private final AtomicLong aiRequests    = new AtomicLong(0);
  private final AtomicLong aiErrors      = new AtomicLong(0);
  private final AtomicLong aiTotalLatency = new AtomicLong(0);

  // ── General errors ─────────────────────────────────────────
  private final AtomicLong totalErrors = new AtomicLong(0);

  // ── PR bot — lifetime counters ─────────────────────────────
  private final AtomicLong prReviewsTotal   = new AtomicLong(0);
  private final AtomicLong prReviewsPassed  = new AtomicLong(0);
  private final AtomicLong prReviewsFailed  = new AtomicLong(0);
  private final AtomicLong prReviewsTimeout = new AtomicLong(0);
  private final AtomicLong prTotalLatency   = new AtomicLong(0);

  // ── PR bot — failure cause breakdown ──────────────────────
  private final LongAdder failGithub401  = new LongAdder();
  private final LongAdder failTimeout    = new LongAdder();
  private final LongAdder failParseError = new LongAdder();
  private final LongAdder failEmptyPatch = new LongAdder();
  private final LongAdder failRateLimit  = new LongAdder();
  private final LongAdder failOther      = new LongAdder();

  // ── Rolling window — last 100 PR review latencies ─────────
  // Guarded by `this` lock — only used for percentile calculation
  private final ArrayDeque<RunRecord> recentRuns = new ArrayDeque<>(100);
  private static final int WINDOW_SIZE = 100;

  // ── Per-repo counts ────────────────────────────────────────
  private final ConcurrentHashMap<String, LongAdder> repoReviewCounts = new ConcurrentHashMap<>();

  // ── Alert thresholds ───────────────────────────────────────
  private static final double TIMEOUT_RATE_WARN  = 0.20; // 20%
  private static final double PASS_RATE_WARN     = 0.30; // below 30% pass rate
  private static final long   LATENCY_WARN_MS    = 120_000; // 2 min avg

  // ── Async job queue metrics ────────────────────────────────
  private final AtomicLong asyncJobsTotal    = new AtomicLong(0);
  private final AtomicLong asyncJobsDone     = new AtomicLong(0);
  private final AtomicLong asyncJobsFailed   = new AtomicLong(0);
  private final AtomicLong asyncJobsTimeout  = new AtomicLong(0);
  private final AtomicLong asyncJobsRejected = new AtomicLong(0);
  private final AtomicLong asyncTotalLatency = new AtomicLong(0);

  // ── Rate limit rejection counters (by identity type) ──────
  private final LongAdder rateLimitGithub = new LongAdder();
  private final LongAdder rateLimitHeader = new LongAdder();
  private final LongAdder rateLimitIp     = new LongAdder();

  // ── Priority rejection counters ────────────────────────────
  private final LongAdder priorityRejectedHigh   = new LongAdder();
  private final LongAdder priorityRejectedMedium = new LongAdder();
  private final LongAdder priorityRejectedLow    = new LongAdder();

  // ── Failover event counter ─────────────────────────────────
  private final AtomicLong failoverEvents = new AtomicLong(0);

  /**
   * Record a completed async job.
   * Called by ReviewJobService on every terminal state transition.
   */
  public void recordAsyncJob(ReviewJobService.JobStatus status, long durationMs) {
    asyncJobsTotal.incrementAndGet();
    asyncTotalLatency.addAndGet(durationMs);
    switch (status) {
      case DONE     -> asyncJobsDone.incrementAndGet();
      case FAILED   -> asyncJobsFailed.incrementAndGet();
      case TIMEOUT  -> asyncJobsTimeout.incrementAndGet();
      case REJECTED -> asyncJobsRejected.incrementAndGet();
      default       -> {} // QUEUED/RUNNING not terminal
    }
  }

  /** Record a job rejection due to queue overflow, broken down by priority. */
  public void recordPriorityRejection(com.aicode.model.Priority priority) {
    switch (priority) {
      case HIGH   -> priorityRejectedHigh.increment();
      case MEDIUM -> priorityRejectedMedium.increment();
      case LOW    -> priorityRejectedLow.increment();
    }
    log.info("Job rejected due to queue overflow — priority={}", priority);
  }

  /** Log a WARN when queue depth exceeds the alert threshold. */
  public void recordQueueAlert(int depth) {
    log.warn("ALERT: queue depth {} exceeds alert threshold", depth);
  }

  /** Record a rate limit rejection, broken down by identity type (github/header/ip). */
  public void recordRateLimitRejection(String identityType) {
    switch (identityType) {
      case "github" -> rateLimitGithub.increment();
      case "header" -> rateLimitHeader.increment();
      case "ip"     -> rateLimitIp.increment();
      default       -> log.warn("Unknown identityType for rate limit rejection: {}", identityType);
    }
    log.info("Rate limit rejection — identityType={}", identityType);
  }

  /**
   * Record a failover event (entering or leaving failover mode).
   *
   * @param entering true when entering failover, false when leaving
   * @param at       the instant the event occurred
   */
  public void recordFailoverEvent(boolean entering, Instant at) {
    failoverEvents.incrementAndGet();
    if (entering) {
      log.warn("FAILOVER: entering failover mode at={}", at);
    } else {
      log.info("FAILOVER: leaving failover mode at={}", at);
    }
  }

  /** Log a WARN when the job failure rate exceeds the alert threshold. */
  public void recordFailureRateAlert(double rate) {
    log.warn("ALERT: job failure rate {:.1f}% exceeds alert threshold", rate * 100);
  }

  /** Log a WARN when average job latency exceeds the alert threshold. */
  public void recordLatencyAlert(long avgMs) {
    log.warn("ALERT: avg job latency {}ms exceeds alert threshold", avgMs);
  }

  // ── Public API ─────────────────────────────────────────────

  public void recordAnalysis(long durationMs, boolean wasCached) {
    totalRequests.incrementAndGet();
    totalRequestTime.addAndGet(durationMs);
    if (wasCached) cacheHits.incrementAndGet();
    else           cacheMisses.incrementAndGet();
    log.info("Analysis completed — duration={}ms cached={} avgDuration={}ms",
        durationMs, wasCached, avgRequestTime());
  }

  public void recordAICall(long latencyMs, boolean success) {
    aiRequests.incrementAndGet();
    aiTotalLatency.addAndGet(latencyMs);
    if (!success) aiErrors.incrementAndGet();
  }

  public void recordError(String errorType, String message) {
    totalErrors.incrementAndGet();
    log.warn("Error recorded — type={} message={}", errorType, message);
  }

  /**
   * Record a completed PR review.
   *
   * @param durationMs  wall-clock time from webhook receipt to check-run posted
   * @param passed      true if score >= SCORE_GATE
   * @param timedOut    true if analysis exceeded the hard timeout
   * @param repo        "owner/repo" — used for per-repo breakdown
   * @param failureCause null on success; one of GITHUB_401 / TIMEOUT / PARSE_ERROR /
   *                     EMPTY_PATCH / RATE_LIMIT / OTHER on failure
   */
  public void recordPRReview(long durationMs, boolean passed, boolean timedOut,
                              String repo, FailureCause failureCause) {
    prReviewsTotal.incrementAndGet();
    prTotalLatency.addAndGet(durationMs);

    if (timedOut || failureCause == FailureCause.TIMEOUT) {
      prReviewsTimeout.incrementAndGet();
      failTimeout.increment();
    } else if (passed) {
      prReviewsPassed.incrementAndGet();
    } else {
      prReviewsFailed.incrementAndGet();
      if (failureCause != null) recordFailureCause(failureCause);
    }

    // Rolling window (lock only for deque mutation)
    synchronized (this) {
      if (recentRuns.size() >= WINDOW_SIZE) recentRuns.pollFirst();
      recentRuns.addLast(new RunRecord(durationMs, passed, timedOut, Instant.now()));
    }

    // Per-repo count
    if (repo != null && !repo.isBlank()) {
      repoReviewCounts.computeIfAbsent(repo, k -> new LongAdder()).increment();
    }

    log.info("PR review — passed={} timedOut={} duration={}ms totalPRs={} repo={}",
        passed, timedOut, durationMs, prReviewsTotal.get(), repo);

    checkAlerts();
  }

  /** Backward-compatible overload (no repo / cause) — used by existing callers. */
  public void recordPRReview(long durationMs, boolean passed, boolean timedOut) {
    recordPRReview(durationMs, passed, timedOut, null, timedOut ? FailureCause.TIMEOUT : null);
  }

  // ── Snapshot ───────────────────────────────────────────────

  public MetricsSnapshot getMetrics() {
    long total = prReviewsTotal.get();
    long avgPR = total > 0 ? prTotalLatency.get() / total : 0L;

    Percentiles p = computePercentiles();

    Map<String, Long> topRepos = repoReviewCounts.entrySet().stream()
        .sorted((a, b) -> Long.compare(b.getValue().sum(), a.getValue().sum()))
        .limit(10)
        .collect(java.util.LinkedHashMap::new,
                 (m, e) -> m.put(e.getKey(), e.getValue().sum()),
                 Map::putAll);

    Map<String, Long> failureCauses = Map.of(
        "github_401",  failGithub401.sum(),
        "timeout",     failTimeout.sum(),
        "parse_error", failParseError.sum(),
        "empty_patch", failEmptyPatch.sum(),
        "rate_limit",  failRateLimit.sum(),
        "other",       failOther.sum()
    );

    long asyncTotal = asyncJobsTotal.get();
    long avgAsyncMs = asyncTotal > 0 ? asyncTotalLatency.get() / asyncTotal : 0L;
    double jobFailureRate = asyncTotal > 0
        ? (double) asyncJobsFailed.get() / asyncTotal
        : 0.0;

    Map<String, Long> jobRejectionsByPriority = new LinkedHashMap<>();
    jobRejectionsByPriority.put("HIGH",   priorityRejectedHigh.sum());
    jobRejectionsByPriority.put("MEDIUM", priorityRejectedMedium.sum());
    jobRejectionsByPriority.put("LOW",    priorityRejectedLow.sum());

    int queueDepth  = reviewQueueService.getQueueDepth();
    int activeJobs  = reviewQueueService.getActiveCount();
    List<Map.Entry<String, Integer>> aiDailyUsageTop10 = aiUsageLimiter.getTop10Usage();
    FeedbackService.FeedbackMetrics feedback = feedbackService.getMetrics();

    return new MetricsSnapshot(
        totalRequests.get(), avgRequestTime(), cacheHitRate(),
        aiRequests.get(), avgAILatency(), aiErrorRate(),
        totalErrors.get(),
        total, prReviewsPassed.get(), prReviewsFailed.get(),
        prReviewsTimeout.get(), avgPR,
        p.p50, p.p90, p.p99,
        topRepos, failureCauses,
        asyncTotal, asyncJobsDone.get(), asyncJobsFailed.get(),
        asyncJobsTimeout.get(), asyncJobsRejected.get(),
        avgAsyncMs,
        queueDepth, activeJobs, avgAsyncMs, jobFailureRate,
        jobRejectionsByPriority, aiDailyUsageTop10, feedback
    );
  }

  // ── Private helpers ────────────────────────────────────────

  private void recordFailureCause(FailureCause cause) {
    switch (cause) {
      case GITHUB_401  -> failGithub401.increment();
      case TIMEOUT     -> failTimeout.increment();
      case PARSE_ERROR -> failParseError.increment();
      case EMPTY_PATCH -> failEmptyPatch.increment();
      case RATE_LIMIT  -> failRateLimit.increment();
      default          -> failOther.increment();
    }
  }

  private synchronized Percentiles computePercentiles() {
    if (recentRuns.isEmpty()) return new Percentiles(0, 0, 0);
    long[] sorted = recentRuns.stream()
        .mapToLong(r -> r.durationMs)
        .sorted().toArray();
    return new Percentiles(
        sorted[(int) (sorted.length * 0.50)],
        sorted[(int) (sorted.length * 0.90)],
        sorted[sorted.length - 1]
    );
  }

  private void checkAlerts() {
    long total = prReviewsTotal.get();
    if (total < 5) return; // not enough data

    double timeoutRate = (double) prReviewsTimeout.get() / total;
    double passRate    = (double) prReviewsPassed.get()  / total;
    long   avgMs       = total > 0 ? prTotalLatency.get() / total : 0;

    if (timeoutRate > TIMEOUT_RATE_WARN)
      log.warn("ALERT: high timeout rate {:.1f}% (threshold {}%)",
          timeoutRate * 100, (int)(TIMEOUT_RATE_WARN * 100));
    if (passRate < PASS_RATE_WARN)
      log.warn("ALERT: low pass rate {:.1f}% (threshold {}%)",
          passRate * 100, (int)(PASS_RATE_WARN * 100));
    if (avgMs > LATENCY_WARN_MS)
      log.warn("ALERT: high avg PR latency {}ms (threshold {}ms)", avgMs, LATENCY_WARN_MS);
  }

  private long avgRequestTime() {
    long r = totalRequests.get();
    return r > 0 ? totalRequestTime.get() / r : 0;
  }

  private double cacheHitRate() {
    long t = cacheHits.get() + cacheMisses.get();
    return t > 0 ? (double) cacheHits.get() / t * 100 : 0;
  }

  private long avgAILatency() {
    long r = aiRequests.get();
    return r > 0 ? aiTotalLatency.get() / r : 0;
  }

  private double aiErrorRate() {
    long r = aiRequests.get();
    return r > 0 ? (double) aiErrors.get() / r * 100 : 0;
  }

  // ── Inner types ────────────────────────────────────────────

  public enum FailureCause {
    GITHUB_401, TIMEOUT, PARSE_ERROR, EMPTY_PATCH, RATE_LIMIT, OTHER
  }

  private record RunRecord(long durationMs, boolean passed, boolean timedOut, Instant at) {}
  private record Percentiles(long p50, long p90, long p99) {}

  public static class MetricsSnapshot {
    // Code review
    public final long   totalRequests;
    public final long   avgRequestTimeMs;
    public final double cacheHitRate;
    // AI
    public final long   aiRequests;
    public final long   avgAILatencyMs;
    public final double aiErrorRate;
    public final long   totalErrors;
    // PR bot — lifetime
    public final long   prReviewsTotal;
    public final long   prReviewsPassed;
    public final long   prReviewsFailed;
    public final long   prReviewsTimeout;
    public final long   avgPRLatencyMs;
    // PR bot — percentiles (rolling last 100)
    public final long   p50LatencyMs;
    public final long   p90LatencyMs;
    public final long   p99LatencyMs;
    // PR bot — breakdowns
    public final Map<String, Long> topRepos;
    public final Map<String, Long> failureCauses;
    // Async job queue
    public final long asyncTotal;
    public final long asyncDone;
    public final long asyncFailed;
    public final long asyncTimeout;
    public final long asyncRejected;
    public final long avgAsyncMs;
    // Queue / job health
    public final int    queueDepth;
    public final int    activeJobs;
    public final long   avgJobMs;
    public final double jobFailureRate;
    public final Map<String, Long>                    jobRejectionsByPriority;
    public final List<Map.Entry<String, Integer>>     aiDailyUsageTop10;
    public final FeedbackService.FeedbackMetrics      feedback;

    public MetricsSnapshot(
        long totalRequests, long avgRequestTimeMs, double cacheHitRate,
        long aiRequests, long avgAILatencyMs, double aiErrorRate, long totalErrors,
        long prReviewsTotal, long prReviewsPassed, long prReviewsFailed,
        long prReviewsTimeout, long avgPRLatencyMs,
        long p50, long p90, long p99,
        Map<String, Long> topRepos, Map<String, Long> failureCauses,
        long asyncTotal, long asyncDone, long asyncFailed,
        long asyncTimeout, long asyncRejected, long avgAsyncMs,
        int queueDepth, int activeJobs, long avgJobMs, double jobFailureRate,
        Map<String, Long> jobRejectionsByPriority,
        List<Map.Entry<String, Integer>> aiDailyUsageTop10,
        FeedbackService.FeedbackMetrics feedback) {
      this.totalRequests    = totalRequests;
      this.avgRequestTimeMs = avgRequestTimeMs;
      this.cacheHitRate     = cacheHitRate;
      this.aiRequests       = aiRequests;
      this.avgAILatencyMs   = avgAILatencyMs;
      this.aiErrorRate      = aiErrorRate;
      this.totalErrors      = totalErrors;
      this.prReviewsTotal   = prReviewsTotal;
      this.prReviewsPassed  = prReviewsPassed;
      this.prReviewsFailed  = prReviewsFailed;
      this.prReviewsTimeout = prReviewsTimeout;
      this.avgPRLatencyMs   = avgPRLatencyMs;
      this.p50LatencyMs     = p50;
      this.p90LatencyMs     = p90;
      this.p99LatencyMs     = p99;
      this.topRepos         = topRepos;
      this.failureCauses    = failureCauses;
      this.asyncTotal       = asyncTotal;
      this.asyncDone        = asyncDone;
      this.asyncFailed      = asyncFailed;
      this.asyncTimeout     = asyncTimeout;
      this.asyncRejected    = asyncRejected;
      this.avgAsyncMs       = avgAsyncMs;
      this.queueDepth              = queueDepth;
      this.activeJobs              = activeJobs;
      this.avgJobMs                = avgJobMs;
      this.jobFailureRate          = jobFailureRate;
      this.jobRejectionsByPriority = jobRejectionsByPriority;
      this.aiDailyUsageTop10       = aiDailyUsageTop10;
      this.feedback                = feedback;
    }

    // Derived helpers used by the dashboard
    public double passRate() {
      return prReviewsTotal > 0 ? (double) prReviewsPassed / prReviewsTotal * 100 : 0;
    }
    public double timeoutRate() {
      return prReviewsTotal > 0 ? (double) prReviewsTimeout / prReviewsTotal * 100 : 0;
    }
  }
}
