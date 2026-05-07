package com.aicode.service;

import com.aicode.analysis.AnalysisPipeline;
import com.aicode.analysis.DiffAnalysisEngine;
import com.aicode.model.Issue;
import com.aicode.model.Severity;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
/**
 * GitHub PR Review Bot.
 *
 * Auth strategy:
 *   - GitHub App token  → Check Runs (Checks tab), annotations
 *   - PAT (fallback)    → PR comments, raw file fetch
 *
 * Produces:
 *   - Structured summary comment (Conversation tab)
 *   - Inline PR comments on changed lines for CRITICAL/HIGH issues (Files tab)
 *   - Check Run with annotations (Checks tab) — requires GitHub App
 */
@Service
public class PRReviewBotService {

    private static final Logger log = LoggerFactory.getLogger(PRReviewBotService.class);

    private static final String GH_API                  = "https://api.github.com";
    private static final int    BATCH_SIZE               = 50;
    private static final int    MAX_ANNOTATIONS          = 200;
    private static final int    MAX_INLINE               = 5;
    private static final long   RATE_DELAY_MS            = 200;
    private static final String BOT_VERSION              = "v1.0";
    private static final int    ANALYSIS_TIMEOUT_SECONDS = 300;  // 5 min ceiling per PR
    private static final int    MAX_PATCH_LINES          = 2000; // skip huge diffs

    @Value("${analysis.per-file-timeout-seconds:30}")
    private int AI_PER_FILE_TIMEOUT_S;

    private final RestTemplate          restTemplate = new RestTemplate();
    private final ObjectMapper          mapper       = new ObjectMapper();
    private final AnalysisPipeline      pipeline;
    private final DiffAnalysisEngine    diffEngine;
    private final GitHubAppAuthService  appAuth;
    private final Executor              prBotExecutor;
    private final MetricsService        metricsService;

    // Idempotency: track processed delivery IDs to prevent duplicate reviews
    // ConcurrentHashMap.newKeySet() is thread-safe; bounded by eviction below
    private final Set<String> processedDeliveries = ConcurrentHashMap.newKeySet();

    @Value("${github.token:}")
    private String githubToken;

    @Value("${pr-bot.score-gate:70}")
    private int SCORE_GATE;

    public PRReviewBotService(AnalysisPipeline pipeline, DiffAnalysisEngine diffEngine,
                               GitHubAppAuthService appAuth,
                               @org.springframework.beans.factory.annotation.Qualifier("prBotExecutor")
                               Executor prBotExecutor,
                               MetricsService metricsService) {
        this.pipeline       = pipeline;
        this.diffEngine     = diffEngine;
        this.appAuth        = appAuth;
        this.prBotExecutor  = prBotExecutor;
        this.metricsService = metricsService;
    }

    // ── Webhook entry point ────────────────────────────────────

    public void processPRWebhook(String payload, String deliveryId) {
        if (!deduplicateDelivery(deliveryId)) return;
        try {
            JsonNode root   = mapper.readTree(payload);
            String   action = root.path("action").asText();

            // Only process relevant actions — ignore closed, labeled, edited, etc.
            if (!"opened".equals(action) && !"synchronize".equals(action)
                    && !"reopened".equals(action)) {
                log.debug("Ignoring PR action: {}", action);
                return;
            }

            JsonNode pr = root.path("pull_request");
            if (pr.isMissingNode()) return;

            String repo     = root.path("repository").path("full_name").asText();
            String headSha  = pr.path("head").path("sha").asText();
            int    prNumber = pr.path("number").asInt();
            String prUrl    = pr.path("url").asText();
            long   startMs  = System.currentTimeMillis();

            // Split owner/repo for GitHub App installation token lookup
            String[] parts = repo.split("/", 2);
            String owner    = parts.length == 2 ? parts[0] : repo;
            String repoName = parts.length == 2 ? parts[1] : repo;

            log.info("PR #{} in {} (action={}, sha={})", prNumber, repo, action,
                headSha.length() >= 7 ? headSha.substring(0, 7) : headSha);

            // Signal GitHub that the check is in progress immediately
            postCheckRunInProgress(repo, owner, repoName, headSha);

            List<PRFile> files = fetchPRFiles(prUrl);

            // Hard ceiling on dedicated executor — never saturates common pool
            try {
                ReviewOutcome outcome = CompletableFuture
                    .supplyAsync(() -> analyzeAndAnnotate(repo, owner, repoName, prNumber, headSha, files),
                                 prBotExecutor)
                    .orTimeout(ANALYSIS_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .join();
                long ms = System.currentTimeMillis() - startMs;
                metricsService.recordPRReview(ms, outcome.pass, false, repo, null);
                log.info("delivery={} repo={} pr={} action={} status=done score={} pass={} annotations={} ms={}",
                    deliveryId, repo, prNumber, action,
                    outcome.avgScore, outcome.pass, outcome.annotationCount, ms);
            } catch (Exception ex) {
                long ms = System.currentTimeMillis() - startMs;
                Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                boolean isTimeout = cause instanceof TimeoutException;
                metricsService.recordPRReview(ms, false, isTimeout, repo,
                    isTimeout ? MetricsService.FailureCause.TIMEOUT : MetricsService.FailureCause.OTHER);
                log.error("delivery={} repo={} pr={} status={} ms={}",
                    deliveryId, repo, prNumber,
                    isTimeout ? "timeout" : "error", ms);
                postCheckRunTimeout(repo, owner, repoName, headSha, isTimeout);
            }

        } catch (Exception e) {
            log.error("PR webhook failed: {}", e.getMessage(), e);
        }
    }

    // ── Handle check_suite events (GitHub App trigger) ─────────

    /**
     * Handles check_suite events from GitHub Apps.
     * These events contain pull_requests array with PR info.
     */
    public void handleCheckSuite(String payload) {
        try {
            log.info("handleCheckSuite: starting");

            // Check if GitHub App is configured
            if (!appAuth.isEnabled()) {
                log.warn("GitHub App not configured — cannot handle check_suite");
                return;
            }
            
            JsonNode root = mapper.readTree(payload);
            JsonNode checkSuite = root.path("check_suite");
            if (checkSuite.isMissingNode()) {
                log.info("check_suite event missing check_suite node");
                return;
            }

            JsonNode prs = checkSuite.path("pull_requests");
            if (!prs.isArray() || prs.isEmpty()) {
                log.info("check_suite has no pull_requests");
                return;
            }

            JsonNode firstPr = prs.get(0);
            String prUrl = firstPr.path("url").asText();
            if (prUrl.isBlank()) {
                log.info("check_suite PR URL is empty");
                return;
            }

            // Extract PR number from URL: https://api.github.com/repos/owner/repo/pulls/123
            String[] parts = prUrl.split("/");
            int prNumber = Integer.parseInt(parts[parts.length - 1]);

            log.info("check_suite triggered analysis for PR #{}", prNumber);

            // Get repo info from root (not checkSuite.repository)
            JsonNode repoNode = root.path("repository");
            String repo = repoNode.path("full_name").asText();
            String[] repoParts = repo.split("/", 2);
            String owner = repoParts.length == 2 ? repoParts[0] : repo;
            String repoName = repoParts.length == 2 ? repoParts[1] : repo;

            log.info("Repo: {}/{}", owner, repoName);

            // Get PR head SHA using App token
            String prApiUrl = String.format("https://api.github.com/repos/%s/pulls/%d", repo, prNumber);
            log.info("Fetching PR details from: {}", prApiUrl);
            
            ResponseEntity<String> prRes = restTemplate.exchange(
                prApiUrl, HttpMethod.GET,
                new HttpEntity<>(appAuth.installationHeaders(owner, repoName)), String.class);
            JsonNode prNode = mapper.readTree(prRes.getBody());
            String headSha = prNode.path("head").path("sha").asText();

            log.info("PR head SHA: {}", headSha);

            // Get PR files using App token
            List<PRFile> files = fetchPRFilesWithAppToken(prUrl, owner, repoName);
            log.info("Fetched {} files", files.size());
            
            analyzeAndAnnotate(repo, owner, repoName, prNumber, headSha, files);

        } catch (Exception e) {
            log.error("check_suite handling failed: {}", e.getMessage(), e);
        }
    }

    // ── Idempotency helper ─────────────────────────────────────

    /**
     * Returns true if this delivery should be processed (first time seen).
     * Returns false if it's a duplicate — GitHub retries on timeout/5xx.
     * Evicts oldest entries when set exceeds 1000 to prevent memory leak.
     */
    private boolean deduplicateDelivery(String deliveryId) {
        if (deliveryId == null || deliveryId.isBlank()) return true; // no ID = always process
        if (!processedDeliveries.add(deliveryId)) {
            log.info("Skipping duplicate delivery: {}", deliveryId);
            return false;
        }
        // Evict oldest 200 entries when set grows large
        if (processedDeliveries.size() > 1000) {
            processedDeliveries.stream().limit(200).forEach(processedDeliveries::remove);
        }
        return true;
    }

    // ── Helper to fetch PR files with App token ────────────────

    private List<PRFile> fetchPRFilesWithAppToken(String prUrl, String owner, String repoName) {
        try {
            rateLimit();
            ResponseEntity<String> res = restTemplate.exchange(
                prUrl + "/files", HttpMethod.GET,
                new HttpEntity<>(appAuth.installationHeaders(owner, repoName)), String.class);
            List<PRFile> out = new ArrayList<>();
            for (JsonNode f : mapper.readTree(res.getBody()))
                out.add(new PRFile(
                    f.path("filename").asText(),
                    f.path("patch").asText(""),
                    f.path("sha").asText(""),
                    f.path("raw_url").asText("")
                ));
            return out;
        } catch (Exception e) {
            log.error("Failed to fetch PR files: {}", e.getMessage());
            return List.of();
        }
    }

    // ── Core analysis ──────────────────────────────────────────

    private ReviewOutcome analyzeAndAnnotate(String repo, String owner, String repoName,
                                              int prNumber, String headSha, List<PRFile> files) {
        long startMs = System.currentTimeMillis();

        List<Annotation>   rawAnnotations = new ArrayList<>();
        List<InlineComment> inlineComments = new ArrayList<>();
        List<FileResult>   fileResults    = new ArrayList<>();
        int totalScore = 0, fileCount = 0;
        int criticalCount = 0, warningCount = 0, infoCount = 0;
        boolean aiUsed = false;  // track whether AI ran for any file

        for (PRFile file : files) {
            // Support Java, JS, TS, Python — skip everything else
            String language = detectLanguage(file.filename);
            if (language == null) continue;

            // Null/empty patch means binary file or no diff — skip safely
            if (file.patch == null || file.patch.isBlank()) {
                log.debug("Skipping {} — no patch", file.filename);
                continue;
            }
            // Skip very large patches — they're usually generated code or migrations
            long patchLines = file.patch.lines().count();
            if (patchLines > MAX_PATCH_LINES) {
                log.info("Skipping {} — patch too large ({} lines > {})", file.filename, patchLines, MAX_PATCH_LINES);
                continue;
            }

            DiffAnalysisEngine.PatchResult patch = diffEngine.parsePatch(file.patch, file.filename);
            if (patch.getAddedLines().size() < 3) continue;

            // Fetch full file content for accurate analysis
            String fullContent  = fetchRawContent(file.rawUrl);
            String codeToAnalyze = fullContent.isBlank()
                ? diffEngine.extractAddedCode(file.patch)
                : fullContent;
            if (codeToAnalyze.isBlank()) continue;

            // Strip non-ASCII (em-dashes etc.) that break JavaParser
            codeToAnalyze = codeToAnalyze.replaceAll("[^\\x00-\\x7F]", " ");

            log.info("Analyzing {} ({} chars, {})",
                file.filename, codeToAnalyze.length(),
                fullContent.isBlank() ? "diff-only" : "full-file");

            // Per-file AI timeout — if AI takes > 30s, fall back to rule-only result
            // This prevents one slow file from blocking the entire PR review
            final String codeForAnalysis = codeToAnalyze;
            final String langForAnalysis = language;
            AnalysisPipeline.AnalysisResult result;
            boolean fileUsedAI = false;
            try {
                result = java.util.concurrent.CompletableFuture
                    .supplyAsync(() -> pipeline.analyze(codeForAnalysis, langForAnalysis))
                    .orTimeout(AI_PER_FILE_TIMEOUT_S, TimeUnit.SECONDS)
                    .join();
                // Check if AI actually contributed (non-empty improved code or AI-sourced issues)
                fileUsedAI = result.getImprovedCode() != null && !result.getImprovedCode().isBlank();
            } catch (Exception timeoutEx) {
                log.warn("AI timed out for {} after {}s — using rule-only result",
                    file.filename, AI_PER_FILE_TIMEOUT_S);
                // Fall back to rule-only: re-run pipeline with same language (rules still run)
                result = pipeline.analyze(codeForAnalysis, langForAnalysis);
            }
            if (fileUsedAI) aiUsed = true;
            totalScore += result.getScore();
            fileCount++;

            List<Integer> changedLines = patch.getLineNumbers();

            for (Issue issue : result.getIssues()) {
                int line = resolveLineNumber(issue.getLine(), changedLines);
                if (line <= 0) continue;

                Severity sev = Severity.fromIssueType(issue.getType());

                // Annotation for Checks tab — include suggestion for one-click apply
                String fixSnippet = buildFixSnippet(issue.getType(), issue.getMessage());
                rawAnnotations.add(new Annotation(
                    file.filename, line, line,
                    toAnnotationLevel(sev),
                    String.format("[%s] %s", issue.getType(), issue.getMessage()),
                    sev.getEmoji() + " " + sev.getLabel(),
                    fixSnippet   // shown as "Apply suggestion" button in GitHub Files tab
                ));

                // Inline comment for Files tab (CRITICAL + HIGH only, capped)
                if (sev.getLevel() >= Severity.HIGH.getLevel()
                        && inlineComments.size() < MAX_INLINE) {
                    String suggestion   = buildSuggestionBlock(issue.getType(), null);
                    StringBuilder body  = new StringBuilder();
                    body.append(String.format("%s **%s** — %s\n\n",
                        sev.getEmoji(), issue.getType(), issue.getMessage()));
                    if (!fixSnippet.isBlank()) {
                        body.append("**Suggested fix:**\n```java\n")
                            .append(fixSnippet).append("\n```\n\n");
                    }
                    if (!suggestion.isBlank()) {
                        body.append(suggestion).append("\n\n");
                    }
                    body.append("> Detected by AESTHENIXAI · [Docs](https://github.com/settings/apps/aesthenixai)");
                    inlineComments.add(new InlineComment(
                        file.filename, line, headSha, body.toString()
                    ));
                }

                if (sev.getLevel() >= Severity.CRITICAL.getLevel()) criticalCount++;
                else if (sev.getLevel() >= Severity.HIGH.getLevel()) warningCount++;
                else infoCount++;
            }

            fileResults.add(new FileResult(
                file.filename,
                (int) Math.round(result.getScore()),
                result.getIssues(),
                result.getSuggestions().stream().map(s -> s.getMessage()).toList(),
                result.getImprovedCode(),
                result.isAiSkipped()
            ));
        }

        // Deduplicate + cap annotations
        Set<String> seen = new HashSet<>();
        List<Annotation> annotations = rawAnnotations.stream()
            .filter(a -> seen.add(a.path + ":" + a.startLine + ":" + a.message))
            .limit(MAX_ANNOTATIONS)
            .toList();

        int     avgScore   = fileCount > 0 ? totalScore / fileCount : 0;
        boolean pass       = avgScore >= SCORE_GATE;
        long    durationMs = System.currentTimeMillis() - startMs;

        if (fileCount > 0) {
            boolean anyAiSkipped = fileResults.stream()
                    .anyMatch(f -> f.aiSkipped());
            String summaryBody = buildSummaryComment(avgScore, pass, fileResults,
                criticalCount, warningCount, infoCount, annotations.size(), durationMs, aiUsed, anyAiSkipped, null);
            postSummaryComment(repo, prNumber, summaryBody);
            postInlineComments(repo, prNumber, inlineComments);
        }
        postCheckRunWithAnnotations(repo, owner, repoName, headSha, avgScore, pass, annotations, durationMs);

        return new ReviewOutcome(avgScore, pass, annotations.size());
    }

    // ── Clean summary comment ──────────────────────────────────

    private String buildSummaryComment(int score, boolean pass,
                                        List<FileResult> files,
                                        int critical, int warning, int info,
                                        int annotationCount, long durationMs,
                                        boolean aiUsed, boolean aiSkipped, String reportUrl) {
        StringBuilder sb = new StringBuilder();

        // ── Header: score + quality gate ──────────────────────
        String scoreEmoji = score >= 75 ? "🟢" : score >= 50 ? "🟡" : "🔴";
        sb.append("## 🤖 AESTHENIXAI Code Review\n");
        sb.append(String.format("**Score: %d/100 %s** — Quality gate %s\n\n",
            score, scoreEmoji,
            pass ? "passed ✅" : "failed ❌"));

        // ── Issue counts ───────────────────────────────────────
        sb.append("**Issues:**\n");
        sb.append(String.format("  🔴 %d Critical   ⚠️ %d Warnings   🔵 %d Suggestions\n\n",
            critical, warning, info));

        // ── Top fixes (up to 2, high-confidence labelled) ─────
        List<Issue> topIssues = files.stream()
            .flatMap(f -> f.issues().stream())
            .filter(i -> {
                int lvl = Severity.fromIssueType(i.getType()).getLevel();
                return lvl >= Severity.HIGH.getLevel();
            })
            .limit(2)
            .toList();

        if (!topIssues.isEmpty()) {
            sb.append("**Top fixes:**\n");
            for (int idx = 0; idx < topIssues.size(); idx++) {
                Issue issue = topIssues.get(idx);
                Severity sev = Severity.fromIssueType(issue.getType());
                // High-confidence label for issues with penalty weight >= HIGH level (3+)
                String confidence = sev.getLevel() >= Severity.HIGH.getLevel()
                    ? " (high confidence)" : "";
                String fix = buildFixSnippet(issue.getType(), issue.getMessage());
                String fixSuffix = fix.isBlank() ? "" : " → `" + fix.split("\n")[0] + "`";
                sb.append(String.format("  %d. %s **%s**%s — %s%s\n",
                    idx + 1, sev.getEmoji(), issue.getType(), confidence,
                    issue.getMessage(), fixSuffix));
            }
            sb.append("\n");
        }

        // ── Report URL ─────────────────────────────────────────
        if (reportUrl != null && !reportUrl.isBlank()) {
            sb.append(String.format("📊 View full report → %s\n\n", reportUrl));
        }

        sb.append("---\n\n");

        // ── Per-file collapsible sections ──────────────────────
        for (FileResult f : files) {
            String icon      = f.score() >= 75 ? "✅" : f.score() >= 50 ? "⚠️" : "🔴";
            String shortName = f.filename().substring(f.filename().lastIndexOf('/') + 1);

            sb.append(String.format("<details>\n<summary>%s <code>%s</code> — <b>%d/100</b></summary>\n\n",
                icon, shortName, f.score()));

            List<Issue> crits = f.issues().stream()
                .filter(i -> Severity.fromIssueType(i.getType()).getLevel() >= Severity.CRITICAL.getLevel())
                .toList();
            List<Issue> warns = f.issues().stream()
                .filter(i -> {
                    int l = Severity.fromIssueType(i.getType()).getLevel();
                    return l >= Severity.HIGH.getLevel() && l < Severity.CRITICAL.getLevel();
                }).toList();
            List<Issue> infos = f.issues().stream()
                .filter(i -> Severity.fromIssueType(i.getType()).getLevel() < Severity.HIGH.getLevel())
                .toList();

            final int MAX_SHOWN = 5;

            if (!crits.isEmpty()) {
                sb.append("**🚨 Critical Issues**\n");
                crits.stream().limit(MAX_SHOWN)
                    .forEach(i -> sb.append(String.format("- Line %d: %s\n", i.getLine(), i.getMessage())));
                if (crits.size() > MAX_SHOWN) {
                    sb.append(String.format("\n<details><summary>+%d more critical issues</summary>\n\n",
                        crits.size() - MAX_SHOWN));
                    crits.stream().skip(MAX_SHOWN)
                        .forEach(i -> sb.append(String.format("- Line %d: %s\n", i.getLine(), i.getMessage())));
                    sb.append("\n</details>\n");
                }
                sb.append("\n");
            }
            if (!warns.isEmpty()) {
                sb.append("**⚠️ Warnings**\n");
                warns.stream().limit(MAX_SHOWN)
                    .forEach(i -> sb.append(String.format("- Line %d: %s\n", i.getLine(), i.getMessage())));
                if (warns.size() > MAX_SHOWN) {
                    sb.append(String.format("\n<details><summary>+%d more warnings</summary>\n\n",
                        warns.size() - MAX_SHOWN));
                    warns.stream().skip(MAX_SHOWN)
                        .forEach(i -> sb.append(String.format("- Line %d: %s\n", i.getLine(), i.getMessage())));
                    sb.append("\n</details>\n");
                }
                sb.append("\n");
            }
            if (!infos.isEmpty()) {
                sb.append("**ℹ️ Info**\n");
                infos.stream().limit(MAX_SHOWN)
                    .forEach(i -> sb.append(String.format("- Line %d: %s\n", i.getLine(), i.getMessage())));
                if (infos.size() > MAX_SHOWN) {
                    sb.append(String.format("\n<details><summary>+%d more info items</summary>\n\n",
                        infos.size() - MAX_SHOWN));
                    infos.stream().skip(MAX_SHOWN)
                        .forEach(i -> sb.append(String.format("- Line %d: %s\n", i.getLine(), i.getMessage())));
                    sb.append("\n</details>\n");
                }
                sb.append("\n");
            }

            if (!f.suggestions().isEmpty()) {
                sb.append("**💡 Suggestions**\n");
                f.suggestions().stream().limit(3).forEach(s -> sb.append("- ").append(s).append("\n"));
                sb.append("\n");
            }

            if (f.improvedCode() != null && !f.improvedCode().isBlank()) {
                String lang = detectLanguage(f.filename());
                String fence = lang != null ? lang : "java";
                sb.append("<details>\n<summary>✨ View improved code</summary>\n\n```").append(fence).append("\n");
                String[] lines = f.improvedCode().split("\n");
                int limit = Math.min(lines.length, 40);
                for (int i = 0; i < limit; i++) sb.append(lines[i]).append("\n");
                if (lines.length > 40) sb.append("// ... (truncated)\n");
                sb.append("```\n\n</details>\n");
            }

            sb.append("</details>\n\n");
        }

        // ── Footer ─────────────────────────────────────────────
        sb.append("---\n");

        // Failover notice
        if (aiSkipped) {
            sb.append("⚡ Fast mode — rule-only analysis (AI temporarily disabled due to load)\n\n");
        }

        // Feedback prompt
        sb.append("*Was this review helpful? React with 👍 or 👎*\n\n");

        // Analysis metadata
        String aiMode = aiUsed
            ? "JavaParser + Ollama qwen2.5-coder:7b"
            : "JavaParser rules only";
        sb.append(String.format(
            "*%d file%s · %d annotation%s · %dms · %s*\n",
            files.size(), files.size() != 1 ? "s" : "",
            annotationCount, annotationCount != 1 ? "s" : "",
            durationMs, aiMode));

        return sb.toString();
    }

    // ── Fix snippet generator ──────────────────────────────────

    /**
     * Returns a short (1–3 line) fix snippet for known issue types.
     * Keeps inline comments actionable without being noisy.
     */
    private String buildFixSnippet(String issueType, String message) {
        if (issueType == null) return "";
        return switch (issueType.toLowerCase()) {
            case "performance", "string concatenation" ->
                "// Use StringBuilder for string concatenation in loops\n" +
                "StringBuilder sb = new StringBuilder();\n" +
                "sb.append(value);";
            case "nested loop", "deeply nested loops" ->
                "// Extract inner loop into a separate method\n" +
                "private void processItems(List<Item> items) { ... }";
            case "long method" ->
                "// Break into smaller, focused methods\n" +
                "// Each method should do one thing";
            case "god class" ->
                "// Split responsibilities into separate classes\n" +
                "// Apply Single Responsibility Principle";
            case "no exception handling", "empty catch block" ->
                "try {\n" +
                "    // operation\n" +
                "} catch (SpecificException e) {\n" +
                "    log.error(\"Operation failed\", e);\n" +
                "    throw new ServiceException(\"...\", e);\n" +
                "}";
            case "naming convention" ->
                "// Use camelCase for variables/methods, PascalCase for classes\n" +
                "// e.g.: myVariable, processData(), MyService";
            case "security" ->
                "// Validate and sanitize all inputs\n" +
                "// Use parameterized queries, avoid string interpolation";
            default -> "";
        };
    }

    // ── Inline PR comments with GitHub suggestion blocks ──────

    private void postInlineComments(String repo, int prNumber, List<InlineComment> comments) {
        if (comments.isEmpty()) return;
        // Prefer App token; fall back to PAT
        HttpHeaders headers = resolveHeaders(repo);
        if (headers == null) { log.warn("No auth for inline comments — skipping"); return; }
        for (InlineComment c : comments) {
            try {
                rateLimit();
                String url = String.format("%s/repos/%s/pulls/%d/comments", GH_API, repo, prNumber);
                Map<String, Object> body = new LinkedHashMap<>();
                body.put("body",      c.body);
                body.put("commit_id", c.commitId);
                body.put("path",      c.path);
                body.put("line",      c.line);
                body.put("side",      "RIGHT");
                restTemplate.exchange(url, HttpMethod.POST,
                    new HttpEntity<>(mapper.writeValueAsString(body), headers), String.class);
                log.info("Inline comment posted: {}:{}", c.path, c.line);
            } catch (Exception e) {
                log.warn("Inline comment failed {}:{} — {}", c.path, c.line, e.getMessage());
            }
        }
    }

    /** Returns App installation headers if available, PAT headers otherwise, null if neither. */
    private HttpHeaders resolveHeaders(String repo) {
        if (appAuth.isEnabled()) {
            String[] parts = repo.split("/", 2);
            try { return appAuth.installationHeaders(parts[0], parts.length > 1 ? parts[1] : repo); }
            catch (Exception e) { log.debug("App token unavailable: {}", e.getMessage()); }
        }
        return githubToken.isBlank() ? null : githubHeaders();
    }

    /**
     * Builds a GitHub suggestion block for a given issue.
     * When the fix snippet is a single-line replacement, GitHub renders
     * an "Apply suggestion" button the developer can click directly in the PR.
     *
     * Format:
     *   ```suggestion
     *   fixed code here
     *   ```
     */
    private String buildSuggestionBlock(String issueType, String originalLine) {
        if (issueType == null) return "";
        String fixed = switch (issueType.toLowerCase()) {
            case "performance", "string concatenation" ->
                // Replace string concat with StringBuilder — shown as one-click fix
                originalLine != null && originalLine.contains("+=")
                    ? originalLine.replace("+=", "/* use StringBuilder */ +=")
                    : "StringBuilder sb = new StringBuilder(); // replace string concatenation";
            case "no exception handling", "empty catch block" ->
                "} catch (Exception e) {\n    log.error(\"Unexpected error\", e);\n    throw new RuntimeException(e);\n}";
            case "naming convention" ->
                originalLine != null ? originalLine : "// rename to camelCase";
            default -> "";
        };
        return fixed.isBlank() ? "" : "```suggestion\n" + fixed + "\n```";
    }

    // ── GitHub Check Runs (GitHub App) ────────────────────────

    /**
     * Posts an "in_progress" check run immediately when the webhook arrives.
     * This makes GitHub show a spinner on the PR instead of nothing while AI runs.
     */
    private void postCheckRunInProgress(String repo, String owner, String repoName, String headSha) {
        if (!appAuth.isEnabled()) return;
        try {
            HttpHeaders headers = appAuth.installationHeaders(owner, repoName);
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("name",     "AESTHENIXAI Code Review " + BOT_VERSION);
            body.put("head_sha", headSha);
            body.put("status",   "in_progress");
            body.put("started_at", java.time.Instant.now().toString());
            String url = String.format("%s/repos/%s/check-runs", GH_API, repo);
            restTemplate.exchange(url, HttpMethod.POST,
                new HttpEntity<>(mapper.writeValueAsString(body), headers), String.class);
            log.info("Check run in_progress posted for sha={}", headSha.length() >= 7 ? headSha.substring(0, 7) : headSha);
        } catch (Exception e) {
            log.warn("Could not post in_progress check run: {}", e.getMessage());
        }
    }

    /**
     * Posts a neutral/failure check-run when analysis times out or crashes.
     * Ensures the PR is never stuck in "in_progress" forever.
     */
    private void postCheckRunTimeout(String repo, String owner, String repoName,
                                     String headSha, boolean isTimeout) {
        if (!appAuth.isEnabled()) return;
        try {
            HttpHeaders headers = appAuth.installationHeaders(owner, repoName);
            String msg = isTimeout
                ? String.format("Analysis timed out after %ds. Push a new commit to retry.", ANALYSIS_TIMEOUT_SECONDS)
                : "Analysis encountered an unexpected error. Push a new commit to retry.";
            Map<String, Object> output = new LinkedHashMap<>();
            output.put("title",   isTimeout ? "⏱ Analysis timed out" : "⚠️ Analysis error");
            output.put("summary", msg);
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("name",       "AESTHENIXAI Code Review " + BOT_VERSION);
            body.put("head_sha",   headSha);
            body.put("status",     "completed");
            body.put("conclusion", "neutral");
            body.put("output",     output);
            String url = String.format("%s/repos/%s/check-runs", GH_API, repo);
            restTemplate.exchange(url, HttpMethod.POST,
                new HttpEntity<>(mapper.writeValueAsString(body), headers), String.class);
            log.info("Timeout/error check run posted for sha={}", headSha.length() >= 7 ? headSha.substring(0, 7) : headSha);
        } catch (Exception e) {
            log.warn("Could not post timeout check run: {}", e.getMessage());
        }
    }

    private void postCheckRunWithAnnotations(String repo, String owner, String repoName,
                                              String headSha, int score, boolean pass,
                                              List<Annotation> annotations, long durationMs) {
        String conclusion = pass ? "success" : "failure";
        String title = pass
            ? String.format("✅ Score %d/100 — Quality gate passed", score)
            : String.format("❌ Score %d/100 — Quality gate failed (threshold: %d)", score, SCORE_GATE);

        List<List<Annotation>> batches = partition(annotations, BATCH_SIZE);
        if (batches.isEmpty()) batches = List.of(List.of());

        // Resolve auth: prefer GitHub App token, fall back to PAT
        HttpHeaders headers;
        if (appAuth.isEnabled()) {
            try {
                headers = appAuth.installationHeaders(owner, repoName);
                log.info("Using GitHub App token for check run");
            } catch (Exception e) {
                log.warn("GitHub App token failed ({}), falling back to PAT", e.getMessage());
                headers = githubHeaders();
            }
        } else {
            headers = githubHeaders();
            if (githubToken.isBlank()) {
                log.warn("No GitHub App or PAT configured — skipping check run");
                return;
            }
        }

        for (int i = 0; i < batches.size(); i++) {
            List<Annotation> batch = batches.get(i);
            List<Map<String, Object>> annotationMaps = batch.stream()
                .map(a -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("path",             a.path);
                    m.put("start_line",       a.startLine);
                    m.put("end_line",         a.endLine);
                    m.put("annotation_level", a.level);
                    m.put("message",          a.message);
                    m.put("title",            a.title);
                    // Add one-click suggestion block when a fix is available
                    if (a.suggestion != null && !a.suggestion.isBlank()) {
                        m.put("raw_details", "```suggestion\n" + a.suggestion + "\n```");
                    }
                    return m;
                }).toList();

            Map<String, Object> output = new LinkedHashMap<>();
            output.put("title",   title);
            String gateStatus = pass
                ? "✅ All quality checks passed. This PR is ready to merge."
                : String.format(
                    "❌ Quality gate failed — score %d is below the threshold of %d.\n\n" +
                    "**To unblock this PR:**\n" +
                    "1. Fix the issues listed in the annotations above\n" +
                    "2. Push a new commit — the review will re-run automatically\n\n" +
                    "> To enforce this gate, go to **Settings → Branches → Branch protection rules** " +
                    "and add `AESTHENIXAI Code Review %s` as a required status check.",
                    score, SCORE_GATE, BOT_VERSION);
            output.put("summary", String.format(
                "**Score: %d/100** | **%d issue%s** | Processed in %dms\n\n%s",
                score, annotations.size(), annotations.size() != 1 ? "s" : "",
                durationMs, gateStatus));
            if (!annotationMaps.isEmpty()) output.put("annotations", annotationMaps);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("name",       "AESTHENIXAI Code Review " + BOT_VERSION);
            body.put("head_sha",   headSha);
            body.put("status",     "completed");
            body.put("conclusion", conclusion);
            body.put("output",     output);

            try {
                rateLimit();
                String url = String.format("%s/repos/%s/check-runs", GH_API, repo);
                restTemplate.exchange(url, HttpMethod.POST,
                    new HttpEntity<>(mapper.writeValueAsString(body), headers), String.class);
                log.info("Check run posted: conclusion={}, annotations={} (batch {}/{})",
                    conclusion, batch.size(), i + 1, batches.size());
            } catch (Exception e) {
                log.warn("Check run failed (batch {}/{}): {}", i + 1, batches.size(), e.getMessage());
            }
        }
    }

    // ── GitHub REST helpers ────────────────────────────────────

    private void postSummaryComment(String repo, int prNumber, String body) {
        // Try App token first (doesn't need PAT), fall back to PAT
        HttpHeaders headers = null;
        if (appAuth.isEnabled()) {
            String[] parts = repo.split("/", 2);
            String owner = parts.length == 2 ? parts[0] : repo;
            String repoName = parts.length == 2 ? parts[1] : repo;
            try {
                headers = appAuth.installationHeaders(owner, repoName);
            } catch (Exception e) {
                log.debug("App token unavailable for comment, trying PAT: {}", e.getMessage());
            }
        }
        if (headers == null) {
            if (githubToken.isBlank()) {
                log.warn("No auth available for summary comment — skipping");
                return;
            }
            headers = githubHeaders();
        }
        try {
            rateLimit();
            String url = String.format("%s/repos/%s/issues/%d/comments", GH_API, repo, prNumber);
            restTemplate.exchange(url, HttpMethod.POST,
                new HttpEntity<>(mapper.writeValueAsString(Map.of("body", body)), headers),
                String.class);
            log.info("Summary comment posted for PR #{}", prNumber);
        } catch (Exception e) {
            log.warn("Summary comment failed: {}", e.getMessage());
        }
    }

    private List<PRFile> fetchPRFiles(String prUrl) {
        try {
            rateLimit();
            ResponseEntity<String> res = restTemplate.exchange(
                prUrl + "/files", HttpMethod.GET,
                new HttpEntity<>(githubHeaders()), String.class);
            List<PRFile> out = new ArrayList<>();
            for (JsonNode f : mapper.readTree(res.getBody()))
                out.add(new PRFile(
                    f.path("filename").asText(),
                    f.path("patch").asText(""),
                    f.path("sha").asText(""),
                    f.path("raw_url").asText("")
                ));
            return out;
        } catch (Exception e) {
            log.error("Failed to fetch PR files: {}", e.getMessage());
            return List.of();
        }
    }

    private String fetchRawContent(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) return "";
        try {
            // Convert github.com/raw/{sha}/path%2F... → raw.githubusercontent.com/{sha}/path/...
            String url = rawUrl
                .replace("https://github.com/", "https://raw.githubusercontent.com/")
                .replace("/raw/", "/")
                .replace("%2F", "/")
                .replace("%2f", "/");

            HttpHeaders h = new HttpHeaders();
            h.set("User-Agent", "AESTHENIXAI-Bot/1.0");
            h.set("Accept",     "text/plain");
            // Use PAT if available, otherwise try App token
            if (!githubToken.isBlank()) {
                h.set("Authorization", "token " + githubToken);
            } else if (appAuth.isEnabled()) {
                String[] parts = rawUrl.replaceFirst("https://raw.githubusercontent.com/([^/]+)/([^/]+)/.*", "$1/$2").split("/");
                try {
                    String appToken = appAuth.getInstallationTokenForRepo(parts[0], parts.length > 1 ? parts[1] : parts[0]);
                    h.set("Authorization", "token " + appToken);
                } catch (Exception ignored) {}
            }

            ResponseEntity<String> res = restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(h), String.class);
            String body = res.getBody();
            log.info("fetchRawContent: {} status={} length={}",
                url.substring(url.lastIndexOf('/') + 1),
                res.getStatusCode(), body != null ? body.length() : 0);
            return body != null ? body : "";
        } catch (Exception e) {
            log.warn("fetchRawContent failed for {}: {}", rawUrl, e.getMessage());
            return "";
        }
    }

    private HttpHeaders githubHeaders() {
        HttpHeaders h = new HttpHeaders();
        h.set("Accept",     "application/vnd.github.v3+json");
        h.set("User-Agent", "AESTHENIXAI-Bot/1.0");
        h.setContentType(MediaType.APPLICATION_JSON);
        if (!githubToken.isBlank()) h.set("Authorization", "token " + githubToken);
        return h;
    }

    private void rateLimit() {
        try { Thread.sleep(RATE_DELAY_MS); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    private int resolveLineNumber(int issueLine, List<Integer> changedLines) {
        if (changedLines.isEmpty()) return 0;
        if (issueLine <= 0) return changedLines.get(0);
        return changedLines.stream()
                .min(Comparator.comparingInt(l -> Math.abs(l - issueLine)))
                .orElse(changedLines.get(0));
    }

    private <T> List<List<T>> partition(List<T> list, int size) {
        List<List<T>> result = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size)
            result.add(list.subList(i, Math.min(i + size, list.size())));
        return result;
    }

    private String toAnnotationLevel(Severity sev) {
        return switch (sev) {
            case CRITICAL, HIGH -> "failure";
            case MEDIUM         -> "warning";
            default             -> "notice";
        };
    }

    /**
     * Detect language from file extension.
     * Returns null for unsupported files — caller should skip them.
     */
    private String detectLanguage(String filename) {
        if (filename == null) return null;
        String lower = filename.toLowerCase();
        if (lower.endsWith(".java"))       return "java";
        if (lower.endsWith(".js"))         return "javascript";
        if (lower.endsWith(".ts"))         return "typescript";
        if (lower.endsWith(".py"))         return "python";
        return null; // unsupported — skip
    }

    // ── Inner types ────────────────────────────────────────────

    private record PRFile(String filename, String patch, String sha, String rawUrl) {}
    private record Annotation(String path, int startLine, int endLine,
                               String level, String message, String title, String suggestion) {}
    private record InlineComment(String path, int line, String commitId, String body) {}
    private record FileResult(String filename, int score, List<Issue> issues,
                               List<String> suggestions, String improvedCode, boolean aiSkipped) {}
    private record ReviewOutcome(int avgScore, boolean pass, int annotationCount) {}
}
