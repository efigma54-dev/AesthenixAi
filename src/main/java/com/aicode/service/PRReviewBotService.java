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

/**
 * GitHub PR Review Bot — token-based, no GitHub App required.
 *
 * Flow:
 *   1. Webhook received (opened / synchronize)
 *   2. Fetch changed files + parse unified diff patches
 *   3. Analyze only added lines (diff-based)
 *   4. Build Check Run annotations (Files tab — like SonarQube)
 *   5. Post summary comment (Conversation tab)
 *   6. Complete Check Run → "success" or "failure" (Checks tab)
 *
 * Requires: GITHUB_TOKEN env var with repo + checks + pull_requests scopes.
 */
@Service
public class PRReviewBotService {

    private static final Logger log = LoggerFactory.getLogger(PRReviewBotService.class);

    private static final String GH_API          = "https://api.github.com";
    private static final int    SCORE_GATE       = 70;
    private static final int    BATCH_SIZE       = 50;
    private static final int    MAX_ANNOTATIONS  = 200;
    private static final long   RATE_DELAY_MS    = 200;
    private static final String BOT_VERSION      = "v1.0";

    private final RestTemplate       restTemplate = new RestTemplate();
    private final ObjectMapper       mapper       = new ObjectMapper();
    private final AnalysisPipeline   pipeline;
    private final DiffAnalysisEngine diffEngine;

    private final Set<String> processedDeliveries = ConcurrentHashMap.newKeySet();

    @Value("${github.token:}")
    private String githubToken;

    public PRReviewBotService(AnalysisPipeline pipeline, DiffAnalysisEngine diffEngine) {
        this.pipeline   = pipeline;
        this.diffEngine = diffEngine;
    }

    // ── Webhook entry point ────────────────────────────────────

    public void processPRWebhook(String payload, String deliveryId) {
        if (deliveryId != null && !processedDeliveries.add(deliveryId)) {
            log.info("Skipping duplicate delivery: {}", deliveryId);
            return;
        }

        try {
            JsonNode root   = mapper.readTree(payload);
            String   action = root.path("action").asText();
            if (!"opened".equals(action) && !"synchronize".equals(action)) return;

            JsonNode pr = root.path("pull_request");
            if (pr.isMissingNode()) return;

            String repo     = root.path("repository").path("full_name").asText();
            String headSha  = pr.path("head").path("sha").asText();
            int    prNumber = pr.path("number").asInt();
            String prUrl    = pr.path("url").asText();

            log.info("PR #{} in {} (action={}, sha={})", prNumber, repo, action, headSha.substring(0, 7));

            List<PRFile>  files   = fetchPRFiles(prUrl);
            ReviewOutcome outcome = analyzeAndAnnotate(repo, prNumber, headSha, files);

            log.info("PR #{} done — score={}, pass={}, annotations={}",
                prNumber, outcome.avgScore, outcome.pass, outcome.annotationCount);

        } catch (Exception e) {
            log.error("PR webhook failed: {}", e.getMessage(), e);
        }
    }

    // ── Core analysis ──────────────────────────────────────────

    private ReviewOutcome analyzeAndAnnotate(String repo, int prNumber,
                                              String headSha, List<PRFile> files) {
        long startMs = System.currentTimeMillis();

        List<Annotation>  rawAnnotations = new ArrayList<>();
        int totalScore = 0, fileCount = 0;
        int criticalCount = 0, warningCount = 0, infoCount = 0;
        StringBuilder summary = new StringBuilder(
            "## 🤖 AESTHENIXAI Code Review " + BOT_VERSION + "\n\n");

        for (PRFile file : files) {
            if (!file.filename.endsWith(".java") || file.patch.isBlank()) continue;

            DiffAnalysisEngine.PatchResult patch = diffEngine.parsePatch(file.patch, file.filename);
            if (patch.getAddedLines().size() < 3) continue;

            // Prefer full file content (parseable Java) over diff fragments
            String fullContent = fetchRawContent(file.rawUrl);
            String codeToAnalyze = fullContent.isBlank()
                ? diffEngine.extractAddedCode(file.patch)   // fallback: diff only
                : fullContent;

            if (codeToAnalyze.isBlank()) continue;

            log.debug("Analyzing {} ({} chars, {})",
                file.filename, codeToAnalyze.length(),
                fullContent.isBlank() ? "diff-only" : "full-file");

            AnalysisPipeline.AnalysisResult result = pipeline.analyze(codeToAnalyze, file.filename);
            totalScore += result.getScore();
            fileCount++;

            List<Integer> changedLines = patch.getLineNumbers();

            for (Issue issue : result.getIssues()) {
                int line = resolveLineNumber(issue.getLine(), changedLines);
                if (line <= 0) continue;

                Severity sev = Severity.fromIssueType(issue.getType());
                rawAnnotations.add(new Annotation(
                    file.filename, line, line,
                    toAnnotationLevel(sev),
                    String.format("[%s] %s", issue.getType(), issue.getMessage()),
                    sev.getEmoji() + " " + sev.getLabel()
                ));

                if (sev.getLevel() >= Severity.CRITICAL.getLevel()) criticalCount++;
                else if (sev.getLevel() >= Severity.HIGH.getLevel()) warningCount++;
                else infoCount++;
            }

            String icon = result.getScore() >= 75 ? "✅" : result.getScore() >= 50 ? "⚠️" : "🔴";
            summary.append(String.format("### %s `%s` — **%.0f/100**\n",
                icon, file.filename, result.getScore()));

            result.getIssues().stream()
                  .sorted(Comparator.comparingInt(i -> -Severity.fromIssueType(i.getType()).getLevel()))
                  .limit(5)
                  .forEach(i -> {
                      Severity s = Severity.fromIssueType(i.getType());
                      summary.append(String.format("- %s **%s** (line %d): %s\n",
                          s.getEmoji(), i.getType(), i.getLine(), i.getMessage()));
                  });

            result.getSuggestions().stream().limit(3)
                  .forEach(s -> summary.append("- 💡 ").append(s.getMessage()).append("\n"));
            summary.append("\n");
        }

        // Deduplicate
        Set<String> seen = new HashSet<>();
        List<Annotation> deduped = rawAnnotations.stream()
            .filter(a -> seen.add(a.path + ":" + a.startLine + ":" + a.message))
            .toList();

        // Hard cap
        List<Annotation> annotations = deduped.size() > MAX_ANNOTATIONS
            ? deduped.subList(0, MAX_ANNOTATIONS) : deduped;

        int     avgScore   = fileCount > 0 ? totalScore / fileCount : 0;
        boolean pass       = avgScore >= SCORE_GATE;
        long    durationMs = System.currentTimeMillis() - startMs;

        summary.append("---\n");
        summary.append(String.format(
            "**Score: %d/100** | %d file%s | %d annotation%s\n",
            avgScore, fileCount, fileCount != 1 ? "s" : "",
            annotations.size(), annotations.size() != 1 ? "s" : ""));

        if (criticalCount + warningCount + infoCount > 0)
            summary.append(String.format(
                "**Issues:** 🚨 Critical: %d | ⚠️ Warning: %d | ℹ️ Info: %d\n",
                criticalCount, warningCount, infoCount));

        summary.append(pass
            ? "✅ **Quality gate passed.**\n"
            : String.format("❌ **Quality gate failed** — score %d < threshold %d.\n",
                avgScore, SCORE_GATE));
        summary.append(String.format("*Processed in %dms*\n", durationMs));

        if (fileCount > 0) postSummaryComment(repo, prNumber, summary.toString());
        postCheckRunWithAnnotations(repo, headSha, avgScore, pass, annotations, durationMs);

        return new ReviewOutcome(avgScore, pass, annotations.size());
    }

    // ── GitHub Check Runs with annotations ────────────────────

    private void postCheckRunWithAnnotations(String repo, String headSha,
                                              int score, boolean pass,
                                              List<Annotation> annotations,
                                              long durationMs) {
        if (githubToken.isBlank()) {
            log.warn("No GITHUB_TOKEN — skipping check run");
            return;
        }

        String conclusion = pass ? "success" : "failure";
        String title = pass
            ? String.format("✅ Score %d/100 — Quality gate passed", score)
            : String.format("❌ Score %d/100 — Quality gate failed (threshold: %d)", score, SCORE_GATE);

        List<List<Annotation>> batches = partition(annotations, BATCH_SIZE);
        if (batches.isEmpty()) batches = List.of(List.of());

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
                    return m;
                })
                .toList();

            Map<String, Object> output = new LinkedHashMap<>();
            output.put("title",   title);
            output.put("summary", String.format(
                "**Score: %d/100** | **%d issue%s** | Processed in %dms\n\n%s",
                score, annotations.size(), annotations.size() != 1 ? "s" : "",
                durationMs,
                pass ? "All quality checks passed."
                     : "Fix the issues above and push again to re-run the review."));
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
                    new HttpEntity<>(mapper.writeValueAsString(body), githubHeaders()), String.class);
                log.info("Check run posted: conclusion={}, annotations={} (batch {}/{})",
                    conclusion, batch.size(), i + 1, batches.size());
            } catch (Exception e) {
                log.warn("Check run failed (batch {}/{}): {}", i + 1, batches.size(), e.getMessage());
            }
        }
    }

    // ── GitHub REST helpers ────────────────────────────────────

    private void postSummaryComment(String repo, int prNumber, String body) {
        if (githubToken.isBlank()) return;
        try {
            rateLimit();
            String url = String.format("%s/repos/%s/issues/%d/comments", GH_API, repo, prNumber);
            restTemplate.exchange(url, HttpMethod.POST,
                new HttpEntity<>(mapper.writeValueAsString(Map.of("body", body)), githubHeaders()),
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
                    f.path("raw_url").asText("")   // full file content URL
                ));
            return out;
        } catch (Exception e) {
            log.error("Failed to fetch PR files: {}", e.getMessage());
            return List.of();
        }
    }

    /** Fetch the full file content from raw_url (no auth needed for public repos). */
    private String fetchRawContent(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) return "";
        try {
            // raw_url is unauthenticated for public repos; add token for private
            HttpHeaders h = new HttpHeaders();
            h.set("User-Agent", "AESTHENIXAI-Bot/1.0");
            if (!githubToken.isBlank()) h.set("Authorization", "token " + githubToken);
            ResponseEntity<String> res = restTemplate.exchange(
                rawUrl, HttpMethod.GET, new HttpEntity<>(h), String.class);
            return res.getBody() != null ? res.getBody() : "";
        } catch (Exception e) {
            log.warn("Failed to fetch raw content from {}: {}", rawUrl, e.getMessage());
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

    private record PRFile(String filename, String patch, String sha, String rawUrl) {}
    private record Annotation(String path, int startLine, int endLine,
                               String level, String message, String title) {}
    private record ReviewOutcome(int avgScore, boolean pass, int annotationCount) {}
}
