package com.aicode.service;

import com.aicode.model.Priority;
import com.aicode.model.PrioritizedJob;
import com.aicode.model.ReviewRequest;
import com.aicode.model.ReviewResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Async job queue for code review requests.
 *
 * Flow:
 *   1. POST /api/review/async  → returns { jobId, status: "queued" }
 *   2. GET  /api/review/async/{jobId} → returns { status, result? }
 *
 * Safety:
 *   - Configurable queue threshold (default 100); HIGH-priority jobs bypass the cap
 *   - Jobs expire after 10 minutes (cleaned up every 5 min by scheduler)
 *   - Per-job 60s timeout — stuck workers are cancelled automatically
 *   - Deduplication — same code hash reuses existing job
 *   - Priority ordering: HIGH > MEDIUM > LOW; FIFO within same priority
 */
@Service
public class ReviewJobService implements ReviewQueueService {

    private static final Logger log = LoggerFactory.getLogger(ReviewJobService.class);

    private static final long JOB_TTL_MS    = 10 * 60 * 1000; // 10 minutes
    private static final int  JOB_TIMEOUT_S = 60;              // per-job timeout
    private static final int  QUEUE_ALERT_THRESHOLD = 40;      // WARN when depth exceeds this

    @Value("${queue.threshold:100}")
    private int queueThreshold;

    private final CodeReviewService codeReviewService;
    private final MetricsService    metricsService;
    private final Executor          prBotExecutor;
    private FeatureFlagService      featureFlagService;

    // jobId → JobEntry — status polling
    private final ConcurrentHashMap<String, JobEntry> jobs = new ConcurrentHashMap<>(64);

    // codeHash → jobId — deduplication: same code reuses existing job
    private final ConcurrentHashMap<String, String> hashToJob = new ConcurrentHashMap<>(64);

    // Priority-ordered pending queue — PrioritizedJob.compareTo puts HIGH first
    private final PriorityBlockingQueue<PrioritizedJob> pendingQueue =
            new PriorityBlockingQueue<>(16);

    // Count of jobs currently in QUEUED/RUNNING state
    private final AtomicInteger activeCount = new AtomicInteger(0);

    public ReviewJobService(CodeReviewService codeReviewService,
                            MetricsService metricsService,
                            @Qualifier("prBotExecutor") Executor prBotExecutor) {
        this.codeReviewService = codeReviewService;
        this.metricsService    = metricsService;
        this.prBotExecutor     = prBotExecutor;
    }

    @org.springframework.beans.factory.annotation.Autowired
    public void setFeatureFlagService(FeatureFlagService featureFlagService) {
        this.featureFlagService = featureFlagService;
    }

    // ── ReviewQueueService interface ───────────────────────────

    /**
     * Submit a prioritized job.
     *
     * Admission rules:
     *   - HIGH priority: always accepted (bypasses threshold check)
     *   - MEDIUM/LOW: rejected with null when activeCount >= queueThreshold
     *
     * Returns the jobId if accepted, null if rejected (caller returns HTTP 429).
     */
    @Override
    public String submit(PrioritizedJob job) {
        ReviewRequest request = job.request();
        Priority priority = job.priority();

        // Threshold check — HIGH priority bypasses it
        if (priority != Priority.HIGH && activeCount.get() >= queueThreshold) {
            log.warn("Job queue full ({} active, threshold={}) — rejecting {} job",
                    activeCount.get(), queueThreshold, priority);
            metricsService.recordAsyncJob(JobStatus.REJECTED, 0);
            metricsService.recordPriorityRejection(priority);
            return null;
        }

        // Deduplication: reuse existing job for identical code
        String codeHash = Integer.toHexString(request.getSanitizedCode().hashCode());
        String existingId = hashToJob.get(codeHash);
        if (existingId != null && jobs.containsKey(existingId)) {
            JobEntry existing = jobs.get(existingId);
            if (existing != null && !existing.isTerminal()) {
                log.info("job={} dedup — reusing existing job for same code hash", existingId);
                return existingId;
            }
        }

        String jobId = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        jobs.put(jobId, new JobEntry(jobId, JobStatus.QUEUED, null, null, Instant.now()));
        hashToJob.put(codeHash, jobId);
        activeCount.incrementAndGet();
        pendingQueue.offer(job);

        int depth = pendingQueue.size();
        log.info("job={} queued priority={} lang={} chars={} active={} queueDepth={}",
                jobId, priority, request.getLanguage(),
                request.getSanitizedCode().length(), activeCount.get(), depth);

        // Queue depth alert
        if (depth > QUEUE_ALERT_THRESHOLD) {
            log.warn("Queue depth alert: {} jobs queued (threshold={})", depth, QUEUE_ALERT_THRESHOLD);
            metricsService.recordQueueAlert(depth);
        }

        long submitMs = System.currentTimeMillis();

        CompletableFuture
            .supplyAsync(() -> {
                jobs.put(jobId, new JobEntry(jobId, JobStatus.RUNNING, null, null, Instant.now()));
                return codeReviewService.reviewCode(request.getSanitizedCode(), request.getLanguage());
            }, prBotExecutor)
            .orTimeout(JOB_TIMEOUT_S, TimeUnit.SECONDS)
            .whenComplete((result, ex) -> {
                long ms = System.currentTimeMillis() - submitMs;
                activeCount.decrementAndGet();
                hashToJob.remove(codeHash);
                pendingQueue.removeIf(j -> j.request() == request);

                if (ex == null) {
                    log.info("job={} done priority={} lang={} score={} ms={}",
                            jobId, priority, request.getLanguage(), result.getScore(), ms);
                    jobs.put(jobId, new JobEntry(jobId, JobStatus.DONE, result, null, Instant.now()));
                    metricsService.recordAsyncJob(JobStatus.DONE, ms);
                } else {
                    boolean isTimeout = ex instanceof java.util.concurrent.TimeoutException
                        || (ex.getCause() instanceof java.util.concurrent.TimeoutException);
                    String errMsg = isTimeout
                        ? "Analysis timed out after " + JOB_TIMEOUT_S + "s"
                        : ex.getMessage();
                    log.error("job={} {} ms={} error={}", jobId,
                        isTimeout ? "timeout" : "failed", ms, errMsg);
                    jobs.put(jobId, new JobEntry(jobId,
                        isTimeout ? JobStatus.TIMEOUT : JobStatus.FAILED,
                        null, errMsg, Instant.now()));
                    metricsService.recordAsyncJob(
                        isTimeout ? JobStatus.TIMEOUT : JobStatus.FAILED, ms);
                }
            });

        return jobId;
    }

    /** Get the current status of a job. Returns null if not found or expired. */
    @Override
    public JobEntry get(String jobId) {
        return jobs.get(jobId);
    }

    /** Returns current number of active (queued/running) jobs. */
    @Override
    public int getActiveCount() { return activeCount.get(); }

    /** Returns number of jobs currently in QUEUED state (pending dispatch). */
    @Override
    public int getQueueDepth() { return pendingQueue.size(); }

    /**
     * Scheduled cleanup — runs every 5 minutes.
     * Removes jobs older than TTL to prevent unbounded memory growth.
     */
    @Scheduled(fixedRate = 5 * 60 * 1000)
    public void cleanupExpiredJobs() {
        long cutoff = System.currentTimeMillis() - JOB_TTL_MS;
        int before = jobs.size();
        jobs.entrySet().removeIf(e -> e.getValue().createdAt().toEpochMilli() < cutoff);
        int removed = before - jobs.size();
        if (removed > 0)
            log.info("Job cleanup: removed {} expired jobs, {} remaining, {} active",
                removed, jobs.size(), activeCount.get());

        // ── Failover monitoring ────────────────────────────────
        if (featureFlagService != null) {
            int depth = getQueueDepth();
            if (depth >= queueThreshold && featureFlagService.isAiEnabled()) {
                featureFlagService.setAiEnabled(false);
                metricsService.recordFailoverEvent(true, Instant.now());
                log.warn("Failover mode activated — queue depth {} >= threshold {}", depth, queueThreshold);
            } else if (depth < queueThreshold * 0.5 && !featureFlagService.isAiEnabled()) {
                featureFlagService.setAiEnabled(true);
                metricsService.recordFailoverEvent(false, Instant.now());
                log.info("Failover mode recovered — queue depth {} below 50% of threshold {}", depth, queueThreshold);
            }
        }
    }

    // ── Types ──────────────────────────────────────────────────

    public enum JobStatus {
        QUEUED, RUNNING, DONE, FAILED, TIMEOUT, REJECTED;

        /** True when the job has reached a terminal state (no more transitions). */
        public boolean isTerminal() {
            return this == DONE || this == FAILED || this == TIMEOUT;
        }

        /** String value sent to the frontend. */
        public String value() { return name().toLowerCase(); }
    }

    public record JobEntry(
        String         jobId,
        JobStatus      status,
        ReviewResponse result,      // non-null when status=DONE
        String         error,       // non-null when status=FAILED or TIMEOUT
        Instant        createdAt
    ) {
        public boolean isTerminal() { return status.isTerminal(); }
    }
}
