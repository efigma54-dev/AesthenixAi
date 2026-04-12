package com.aicode.controller;

import com.aicode.model.*;
import com.aicode.service.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
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

    public CodeReviewController(CodeReviewService codeReviewService,
            GithubAnalysisService githubAnalysisService,
            LocalAIService localAIService,
            ReportService reportService,
            RepoScanService repoScanService,
            PRReviewBotService prReviewBotService,
            MetricsService metricsService) {
        this.codeReviewService = codeReviewService;
        this.githubAnalysisService = githubAnalysisService;
        this.localAIService = localAIService;
        this.reportService = reportService;
        this.repoScanService = repoScanService;
        this.prReviewBotService = prReviewBotService;
        this.metricsService = metricsService;
    }

    // ── Single file review ─────────────────────────────────────
    @PostMapping("/review")
    public ResponseEntity<ReviewResponse> review(@Valid @RequestBody ReviewRequest request,
            HttpServletRequest httpRequest) {
        log.info("POST /api/review — {} chars", request.getSanitizedCode().length());
        long start = System.currentTimeMillis();

        ReviewResponse result = codeReviewService.reviewCode(request.getSanitizedCode());

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

    // ── Direct AI endpoint (raw Ollama response) ───────────────
    @PostMapping("/review/ai")
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

    // ── Metrics ────────────────────────────────────────────────
    @GetMapping("/metrics")
    public ResponseEntity<MetricsService.MetricsSnapshot> metrics() {
        return ResponseEntity.ok(metricsService.getMetrics());
    }

    // ── GitHub PR webhook ──────────────────────────────────────
    @PostMapping("/github/webhook")
    public ResponseEntity<String> githubWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "X-GitHub-Event", defaultValue = "ping") String eventType) {
        log.info("GitHub webhook received: event={}", eventType);

        if ("ping".equals(eventType)) {
            // GitHub sends a ping when the webhook is first configured
            return ResponseEntity.ok("{\"message\":\"AESTHENIXAI webhook active\"}");
        }

        if ("pull_request".equals(eventType)) {
            CompletableFuture.runAsync(() -> {
                try {
                    prReviewBotService.processPRWebhook(payload, null);
                } catch (Exception e) {
                    log.error("PR webhook failed", e);
                }
            });
        }
        return ResponseEntity.ok("OK");
    }

    // Browser-accessible webhook probe (GET returns info, not 405)
    @GetMapping("/github/webhook")
    public ResponseEntity<String> webhookProbe() {
        return ResponseEntity.ok(
                "{\"status\":\"active\",\"endpoint\":\"POST /api/github/webhook\"," +
                        "\"events\":[\"pull_request\"],\"bot\":\"AESTHENIXAI\"}");
    }

    // ── Health ─────────────────────────────────────────────────
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("AI Code Reviewer is running");
    }
}
