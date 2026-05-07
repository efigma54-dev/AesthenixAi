package com.aicode.controller;

import com.aicode.model.*;
import com.aicode.service.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class CodeReviewController {

    private static final Logger log = LoggerFactory.getLogger(CodeReviewController.class);

    private final CodeReviewService codeReviewService;
    private final GithubAnalysisService githubAnalysisService;
    private final LocalAIService localAIService;
    private final ReportService reportService;
    private final RepoScanService repoScanService;
    private final PRReviewBotService prReviewBotService;
    private final MetricsService metricsService;
    private final WebhookSignatureService webhookSignatureService;
    private final Executor prBotExecutor;
    private final ReviewQueueService reviewJobService;
    private final WarmUpService warmUpService;
    private final RateLimiterService rateLimiterService;
    private final FeedbackService feedbackService;
    private final SafeFixService safeFixService;

    public CodeReviewController(CodeReviewService codeReviewService,
            GithubAnalysisService githubAnalysisService,
            LocalAIService localAIService,
            ReportService reportService,
            RepoScanService repoScanService,
            PRReviewBotService prReviewBotService,
            MetricsService metricsService,
            WebhookSignatureService webhookSignatureService,
            @Qualifier("prBotExecutor") Executor prBotExecutor,
            ReviewQueueService reviewJobService,
            WarmUpService warmUpService,
            RateLimiterService rateLimiterService,
            FeedbackService feedbackService,
            SafeFixService safeFixService) {
        this.codeReviewService = codeReviewService;
        this.githubAnalysisService = githubAnalysisService;
        this.localAIService = localAIService;
        this.reportService = reportService;
        this.repoScanService = repoScanService;
        this.prReviewBotService = prReviewBotService;
        this.metricsService = metricsService;
        this.webhookSignatureService = webhookSignatureService;
        this.prBotExecutor = prBotExecutor;
        this.reviewJobService = reviewJobService;
        this.warmUpService = warmUpService;
        this.rateLimiterService = rateLimiterService;
        this.feedbackService = feedbackService;
        this.safeFixService = safeFixService;
    }

    // ── Single file review ─────────────────────────────────────
    @PostMapping("/review")
    public ResponseEntity<ReviewResponse> review(@Valid @RequestBody ReviewRequest request,
            HttpServletRequest httpRequest) {
        log.info("POST /api/review — {} chars", request.getSanitizedCode().length());
        long start = System.currentTimeMillis();

        ReviewResponse result = codeReviewService.reviewCode(request.getSanitizedCode(), request.getLanguage());

        long ms = System.currentTimeMillis() - start;
        metricsService.recordAnalysis(ms, false);

        String baseUrl = httpRequest.getScheme() + "://" + httpRequest.getServerName()
                + (httpRequest.getServerPort() != 80 && httpRequest.getServerPort() != 443
                        ? ":" + httpRequest.getServerPort()
                        : "");
        ReportResponse report = reportService.save(result, baseUrl);

        return ResponseEntity.ok()
                .header("X-Report-Id", report.getReportId())
                .header("X-Report-Url", report.getShareUrl())
                .body(result);
    }

    // ── Async review — submit job, poll for result ─────────────
    // POST /api/review/async  → { jobId, status: "queued" }
    // GET  /api/review/async/{jobId} → { jobId, status, result? }
    //
    // Use this instead of /review when you want the UI to feel instant.
    // Frontend polls every 1s until status == "done" or "error".
    @PostMapping("/review/async")
    public ResponseEntity<Map<String, Object>> reviewAsync(
            @Valid @RequestBody ReviewRequest request,
            HttpServletRequest httpRequest) {
        log.info("POST /api/review/async — {} chars lang={}", request.getSanitizedCode().length(),
            request.getLanguage());

        // Gate 1: Warm-up — return 503 during startup window
        if (warmUpService.isInWarmupWindow()) {
            return ResponseEntity.status(503)
                    .header("Retry-After", "5")
                    .body(Map.of("error", "warming_up", "retryAfterSeconds", 5));
        }

        // Gate 2: Per-identity rate limit — fires BEFORE queue to avoid wasting queue slots
        String identity = UserIdentityResolver.resolve(httpRequest, null);
        if (!rateLimiterService.tryConsume(identity)) {
            long retryAfter = rateLimiterService.secondsToWaitForRefill(identity);
            String identityType = identity.startsWith("github:") ? "github"
                    : identity.startsWith("header:") ? "header" : "ip";
            metricsService.recordRateLimitRejection(identityType);
            return ResponseEntity.status(429)
                    .header("Retry-After", String.valueOf(retryAfter))
                    .body(Map.of("error", "rate_limit", "retryAfterSeconds", retryAfter));
        }

        // Gate 3: Queue cap — submit job (null = queue full)
        String jobId = reviewJobService.submit(new PrioritizedJob(request, Priority.MEDIUM, Instant.now()));
        if (jobId == null) {
            return ResponseEntity.status(429)
                    .header("Retry-After", "30")
                    .body(Map.of("error", "queue_full", "retryAfterSeconds", 30));
        }

        return ResponseEntity.accepted().body(Map.of(
            "jobId",      jobId,
            "status",     "queued",
            "priority",   Priority.MEDIUM.label(),
            "queueDepth", reviewJobService.getQueueDepth(),
            "pollUrl",    "/api/review/async/" + jobId
        ));
    }

    @GetMapping("/review/async/{jobId}")
    public ResponseEntity<?> reviewAsyncStatus(@PathVariable String jobId) {
        ReviewJobService.JobEntry job = reviewJobService.get(jobId);
        if (job == null)
            return ResponseEntity.status(404).body(Map.of("error", "Job not found or expired"));

        Map<String, Object> resp = new java.util.LinkedHashMap<>();
        resp.put("jobId",  job.jobId());
        resp.put("status", job.status().value());
        if (job.status() == ReviewJobService.JobStatus.DONE)    resp.put("result", job.result());
        if (job.status() == ReviewJobService.JobStatus.FAILED)  resp.put("error",  job.error());
        if (job.status() == ReviewJobService.JobStatus.TIMEOUT) resp.put("error",  job.error());
        return ResponseEntity.ok(resp);
    }

    // ── Direct AI endpoint (raw Ollama response) ───────────────    @PostMapping("/review/ai")
    public ResponseEntity<?> reviewAI(@RequestBody Map<String, String> request) {
        String code = request.get("code");
        if (code == null || code.isBlank())
            return ResponseEntity.badRequest().body(Map.of("error", "Code is required"));
        com.aicode.model.AIResult result = localAIService.analyzeCode(code);
        return ResponseEntity.ok(result);
    }

    // ── SSE streaming endpoint ─────────────────────────────────
    @GetMapping(value = "/review/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> stream(@RequestParam String code) {
        if (code == null || code.isBlank())
            return Flux.just("Error: Code is required");
        return localAIService.streamCodeAnalysisForDisplay(code);
    }

    // ── Multi-file review ──────────────────────────────────────
    @PostMapping("/review/multi")
    public ResponseEntity<MultiReviewResponse> reviewMulti(@Valid @RequestBody MultiReviewRequest request) {
        log.info("POST /api/review/multi — {} files", request.getFiles().size());

        var executor = Executors.newFixedThreadPool(3);
        List<CompletableFuture<MultiReviewResponse.FileResult>> futures = new ArrayList<>();

        for (MultiReviewRequest.FileEntry entry : request.getFiles()) {
            futures.add(CompletableFuture.supplyAsync(() -> {
                try {
                    ReviewResponse review = codeReviewService.reviewCode(entry.getCode());
                    return MultiReviewResponse.FileResult.builder()
                            .name(entry.getName()).score(review.getScore())
                            .issues(review.getIssues()).suggestions(review.getSuggestions())
                            .improvedCode(review.getImprovedCode()).build();
                } catch (Exception e) {
                    log.warn("Skipping file {} — {}", entry.getName(), e.getMessage());
                    return MultiReviewResponse.FileResult.builder()
                            .name(entry.getName()).score(0).issues(List.of())
                            .suggestions(List.of("Failed: " + e.getMessage())).build();
                }
            }, executor));
        }

        List<MultiReviewResponse.FileResult> results = futures.stream()
                .map(CompletableFuture::join).toList();
        executor.shutdown();

        int total = results.stream().mapToInt(MultiReviewResponse.FileResult::getScore).sum();
        int issues = results.stream().mapToInt(r -> r.getIssues() != null ? r.getIssues().size() : 0).sum();
        int avg = results.isEmpty() ? 0 : total / results.size();

        return ResponseEntity.ok(MultiReviewResponse.builder()
                .averageScore(avg).totalIssues(issues)
                .filesAnalyzed(results.size()).files(results).build());
    }

    // ── GitHub repo review ─────────────────────────────────────
    @PostMapping("/review/github")
    public ResponseEntity<GithubReviewResponse> reviewGithub(@Valid @RequestBody GithubReviewRequest request) {
        log.info("POST /api/review/github — {}", request.getRepoUrl());
        return ResponseEntity.ok(githubAnalysisService.analyzeRepo(request.getRepoUrl()));
    }

    // ── Repo scan ──────────────────────────────────────────────
    @PostMapping("/repo/scan")
    public ResponseEntity<RepoScanResponse> scanRepository(@Valid @RequestBody RepoScanRequest request) {
        log.info("POST /api/repo/scan — {}", request.getRepoUrl());
        return ResponseEntity.ok(repoScanService.scanRepository(request.getRepoUrl(), request.getToken()));
    }

    // ── Shareable report ───────────────────────────────────────
    @GetMapping("/report/{id}")
    public ResponseEntity<?> getReport(@PathVariable String id) {
        ReportResponse report = reportService.get(id);
        if (report == null)
            return ResponseEntity.status(404).body(Map.of("error", "Report not found or expired."));
        return ResponseEntity.ok(report);
    }

    // ── Safe Fixes ─────────────────────────────────────────────
    // POST /api/fix/safe — apply safe, non-breaking fixes
    // This is the HIGHEST ROI feature for launch
    @PostMapping("/fix/safe")
    public ResponseEntity<SafeFixResponse> applySafeFixes(@Valid @RequestBody SafeFixRequest request) {
        log.info("POST /api/fix/safe — {} chars", request.getSanitizedCode().length());
        return ResponseEntity.ok(safeFixService.applySafeFixes(request.getSanitizedCode()));
    }

    // ── Feedback ───────────────────────────────────────────────
    // POST /api/feedback — record user feedback on reviews
    // This is CRITICAL for trust building and product improvement
    @PostMapping("/feedback")
    public ResponseEntity<Map<String, Object>> feedback(@Valid @RequestBody FeedbackRequest request) {
        log.info("POST /api/feedback — type={} reviewId={}", request.getType(), request.getReviewId());
        
        feedbackService.recordFeedback(request);
        
        return ResponseEntity.ok(Map.of(
            "status", "recorded",
            "message", "Thank you for your feedback!"
        ));
    }
    
    // GET /api/feedback/metrics — get feedback metrics
    @GetMapping("/feedback/metrics")
    public ResponseEntity<FeedbackService.FeedbackMetrics> feedbackMetrics() {
        return ResponseEntity.ok(feedbackService.getMetrics());
    }

    // ── Metrics ────────────────────────────────────────────────
    @GetMapping("/metrics")
    public ResponseEntity<MetricsService.MetricsSnapshot> metrics() {
        return ResponseEntity.ok(metricsService.getMetrics());
    }

    // ── Dashboard (human-readable metrics summary) ─────────────
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> dashboard() {
        MetricsService.MetricsSnapshot m = metricsService.getMetrics();
        Map<String, Object> d = new java.util.LinkedHashMap<>();

        // PR bot summary
        Map<String, Object> prBot = new java.util.LinkedHashMap<>();
        prBot.put("total",       m.prReviewsTotal);
        prBot.put("passed",      m.prReviewsPassed);
        prBot.put("failed",      m.prReviewsFailed);
        prBot.put("timeouts",    m.prReviewsTimeout);
        prBot.put("passRate",    String.format("%.1f%%", m.passRate()));
        prBot.put("timeoutRate", String.format("%.1f%%", m.timeoutRate()));
        d.put("prBot", prBot);

        // Latency
        Map<String, Object> latency = new java.util.LinkedHashMap<>();
        latency.put("avgMs", m.avgPRLatencyMs);
        latency.put("p50Ms", m.p50LatencyMs);
        latency.put("p90Ms", m.p90LatencyMs);
        latency.put("p99Ms", m.p99LatencyMs);
        d.put("latency", latency);

        // Failure causes
        d.put("failureCauses", m.failureCauses);

        // Top repos
        d.put("topRepos", m.topRepos);

        // Code review API
        Map<String, Object> api = new java.util.LinkedHashMap<>();
        api.put("totalRequests",  m.totalRequests);
        api.put("avgRequestMs",   m.avgRequestTimeMs);
        api.put("cacheHitRate",   String.format("%.1f%%", m.cacheHitRate));
        api.put("aiRequests",     m.aiRequests);
        api.put("avgAILatencyMs", m.avgAILatencyMs);
        api.put("aiErrorRate",    String.format("%.1f%%", m.aiErrorRate));
        d.put("api", api);

        // Async job queue
        Map<String, Object> async = new java.util.LinkedHashMap<>();
        async.put("total",       m.asyncTotal);
        async.put("done",        m.asyncDone);
        async.put("failed",      m.asyncFailed);
        async.put("timeout",     m.asyncTimeout);
        async.put("rejected",    m.asyncRejected);
        async.put("avgMs",       m.avgAsyncMs);
        async.put("activeNow",   reviewJobService.getActiveCount());
        d.put("asyncJobs", async);

        return ResponseEntity.ok(d);
    }

    // ── GitHub PR webhook ──────────────────────────────────────
    // Both paths accepted — /api/webhooks/github (standard) and /api/github/webhook (legacy)
    @PostMapping({"/webhooks/github", "/github/webhook"})
    public ResponseEntity<String> githubWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "X-GitHub-Event",         defaultValue = "ping") String eventType,
            @RequestHeader(value = "X-GitHub-Delivery",      defaultValue = "")     String deliveryId,
            @RequestHeader(value = "X-Hub-Signature-256",    defaultValue = "")     String signature) {

        try {
            log.info("GitHub webhook: event={} delivery={}", eventType, deliveryId);

            // Verify HMAC signature (no-op if webhook secret not configured)
            if (!webhookSignatureService.isValid(payload, signature.isBlank() ? null : signature)) {
                log.warn("Webhook signature verification failed — rejecting request");
                return ResponseEntity.status(401).body("{\"error\":\"Invalid signature\"}");
            }

            if ("ping".equals(eventType))
                return ResponseEntity.ok("{\"message\":\"AESTHENIXAI webhook active\"}");

            if ("pull_request".equals(eventType)) {
                final String id = deliveryId;
                CompletableFuture.runAsync(() -> {
                    try { prReviewBotService.processPRWebhook(payload, id.isBlank() ? null : id); }
                    catch (Exception e) { log.error("PR webhook processing failed", e); }
                }, prBotExecutor);
                return ResponseEntity.ok("{\"status\":\"pull_request queued\"}");
            }

            if ("check_suite".equals(eventType)) {
                CompletableFuture.runAsync(() -> {
                    try {
                        log.info("Processing check_suite event");
                        prReviewBotService.handleCheckSuite(payload);
                        log.info("check_suite processing completed");
                    } catch (Exception e) {
                        log.error("check_suite handling failed", e);
                    }
                }, prBotExecutor);
                return ResponseEntity.ok("{\"status\":\"check_suite queued\"}");
            }

            log.info("Ignoring unsupported event type: {}", eventType);
            return ResponseEntity.ok("{\"status\":\"ignored\"}");
            
        } catch (Exception e) {
            log.error("Webhook handler crashed", e);
            e.printStackTrace();
            return ResponseEntity.ok("{\"status\":\"error handled\"}");
        }
    }

    // Browser probe — GET returns info instead of 405
    @GetMapping({"/webhooks/github", "/github/webhook"})
    public ResponseEntity<String> webhookProbe() {
        return ResponseEntity.ok(
            "{\"status\":\"active\",\"endpoint\":\"POST /api/webhooks/github\"," +
            "\"events\":[\"pull_request\"],\"bot\":\"AESTHENIXAI\"}");
    }

    // ── Health ─────────────────────────────────────────────────
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("AI Code Reviewer is running");
    }
    
    // GET /api/health/ping — warm-up aware health check for Render
    @GetMapping("/health/ping")
    public ResponseEntity<Map<String, Object>> ping() {
        String status = warmUpService.getStatus();
        long uptime = warmUpService.getUptimeSeconds();
        
        Map<String, Object> response = Map.of(
            "status", status,
            "uptime", uptime
        );
        
        // Return 503 if still warming up and in window
        if ("warming_up".equals(status) && warmUpService.isInWarmupWindow()) {
            return ResponseEntity.status(503)
                .header("Retry-After", "5")
                .body(response);
        }
        
        return ResponseEntity.ok(response);
    }
}
