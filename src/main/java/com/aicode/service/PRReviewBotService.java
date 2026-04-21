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
 * Produces:
 *   - Structured summary comment (Conversation tab) with score, collapsible
 *     per-file sections, issue breakdown, suggestions, and improved code block
 *   - Inline PR comments on changed lines for CRITICAL/HIGH issues (Files tab)
 *   - Check Run annotations (Checks tab — requires GitHub App for full support)
 */
@Service
public class PRReviewBotService {

    private static final Logger log = LoggerFactory.getLogger(PRReviewBotService.class);

    private static final String GH_API         = "https://api.github.com";
    private static final int    SCORE_GATE      = 70;
    private static final int    BATCH_SIZE      = 50;
    private static final int    MAX_ANNOTATIONS = 200;
    private static final int    MAX_INLINE      = 5;    // inline comments per PR
    private static final long   RATE_DELAY_MS   = 200;
    private static final String BOT_VERSION     = "v1.0";

    private final RestTemplate       restTemplate = new RestTemplate();
    private final ObjectMapper       mapper       = new ObjectMapper();
    private final AnalysisPipeline   pipeline;
    private final DiffAnalysisEngine diffEngine;

    private final Set<String> processedDeliveries = ConcurrentHashMap.newKeySet();

    @Value("${github.token:}")
    private String githubToken;

    public PRReviewBotService(AnalysisPipeline pipeline, DiffAnalysisEngine diffEngine) {
        this.pipeline  = pipeline;
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

        List<Annotation>   rawAnnotations = new ArrayList<>();
        List<InlineComment> inlineComments = new ArrayList<>();
        List<FileResult>   fileResults    = new ArrayList<>();
        int totalScore = 0, fileCount = 0;
        int criticalCount = 0, warningCount = 0, infoCount = 0;

        for (PRFile file : files) {
            if (!file.filename.endsWith(".java") || file.patch.isBlank()) continue;

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

            AnalysisPipeline.AnalysisResult result = pipeline.analyze(codeToAnalyze, file.filename);
            totalScore += result.getScore();
            fileCount++;

            List<Integer> changedLines = patch.getLineNumbers();

            for (Issue issue : result.getIssues()) {
                int line = resolveLineNumber(issue.getLine(), changedLines);
                if (line <= 0) continue;

                Severity sev = Severity.fromIssueType(issue.getType());

                // Annotation for Checks tab
                rawAnnotations.add(new Annotation(
                    file.filename, line, line,
                    toAnnotationLevel(sev),
                    String.format("[%s] %s", issue.getType(), issue.getMessage()),
                    sev.getEmoji() + " " + sev.getLabel()
                ));

                // Inline comment for Files tab (CRITICAL + HIGH only, capped)
                if (sev.getLevel() >= Severity.HIGH.getLevel()
                        && inlineComments.size() < MAX_INLINE) {
                    inlineComments.add(new InlineComment(
                        file.filename, line, headSha,
                        String.format("%s **%s** — %s\n\n> Detected by AESTHENIXAI static analysis",
                            sev.getEmoji(), issue.getType(), issue.getMessage())
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
                result.getImprovedCode()
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
            String summaryBody = buildSummaryComment(avgScore, pass, fileResults,
                criticalCount, warningCount, infoCount, annotations.size(), durationMs);
            postSummaryComment(repo, prNumber, summaryBody);
            postInlineComments(repo, prNumber, inlineComments);
        }
        postCheckRunWithAnnotations(repo, headSha, avgScore, pass, annotations, durationMs);

        return new ReviewOutcome(avgScore, pass, annotations.size());
    }

    // ── Clean summary comment ──────────────────────────────────

    private String buildSummaryComment(int score, boolean pass,
                                        List<FileResult> files,
                                        int critical, int warning, int info,
                                        int annotationCount, long durationMs) {
        StringBuilder sb = new StringBuilder();

        // Score header
        String scoreEmoji = score >= 75 ? "🟢" : score >= 50 ? "🟡" : "🔴";
        sb.append("## 🤖 AESTHENIXAI Code Review ").append(BOT_VERSION).append("\n\n");
        sb.append(String.format("**Score: %s %d/100** — %s\n\n",
            scoreEmoji, score,
            pass ? "Quality gate passed ✅" : "Quality gate failed ❌"));

        // Issue breakdown bar
        if (critical + warning + info > 0) {
            sb.append("> ");
            if (critical > 0) sb.append(String.format("🚨 **%d Critical**&nbsp;&nbsp;", critical));
            if (warning  > 0) sb.append(String.format("⚠️ **%d Warning**&nbsp;&nbsp;", warning));
            if (info     > 0) sb.append(String.format("ℹ️ **%d Info**", info));
            sb.append("\n\n");
        }

        sb.append("---\n\n");

        // Per-file collapsible sections
        for (FileResult f : files) {
            String icon      = f.score >= 75 ? "✅" : f.score >= 50 ? "⚠️" : "🔴";
            String shortName = f.filename.substring(f.filename.lastIndexOf('/') + 1);

            sb.append(String.format("<details>\n<summary>%s <code>%s</code> — <b>%d/100</b></summary>\n\n",
                icon, shortName, f.score));

            // Issues grouped by severity
            List<Issue> crits = f.issues.stream()
                .filter(i -> Severity.fromIssueType(i.getType()).getLevel() >= Severity.CRITICAL.getLevel())
                .toList();
            List<Issue> warns = f.issues.stream()
                .filter(i -> {
                    int l = Severity.fromIssueType(i.getType()).getLevel();
                    return l >= Severity.HIGH.getLevel() && l < Severity.CRITICAL.getLevel();
                }).toList();
            List<Issue> infos = f.issues.stream()
                .filter(i -> Severity.fromIssueType(i.getType()).getLevel() < Severity.HIGH.getLevel())
                .toList();

            if (!crits.isEmpty()) {
                sb.append("**🚨 Critical Issues**\n");
                crits.forEach(i -> sb.append(String.format("- Line %d: %s\n", i.getLine(), i.getMessage())));
                sb.append("\n");
            }
            if (!warns.isEmpty()) {
                sb.append("**⚠️ Warnings**\n");
                warns.forEach(i -> sb.append(String.format("- Line %d: %s\n", i.getLine(), i.getMessage())));
                sb.append("\n");
            }
            if (!infos.isEmpty()) {
                sb.append("**ℹ️ Info**\n");
                infos.stream().limit(3).forEach(i -> sb.append(String.format("- Line %d: %s\n", i.getLine(), i.getMessage())));
                sb.append("\n");
            }

            // Suggestions
            if (!f.suggestions.isEmpty()) {
                sb.append("**💡 Suggestions**\n");
                f.suggestions.stream().limit(3).forEach(s -> sb.append("- ").append(s).append("\n"));
                sb.append("\n");
            }

            // Improved code block (collapsible)
            if (f.improvedCode != null && !f.improvedCode.isBlank()) {
                sb.append("<details>\n<summary>✨ View improved code</summary>\n\n```java\n");
                String[] lines = f.improvedCode.split("\n");
                int limit = Math.min(lines.length, 40);
                for (int i = 0; i < limit; i++) sb.append(lines[i]).append("\n");
                if (lines.length > 40) sb.append("// ... (truncated)\n");
                sb.append("```\n\n</details>\n");
            }

            sb.append("</details>\n\n");
        }

        // Footer
        sb.append("---\n");
        sb.append(String.format(
            "*%d file%s · %d annotation%s · %dms · JavaParser + Ollama qwen3.5:9b*\n",
            files.size(), files.size() != 1 ? "s" : "",
            annotationCount, annotationCount != 1 ? "s" : "",
            durationMs));

        return sb.toString();
    }

    // ── Inline PR comments ─────────────────────────────────────

    private void postInlineComments(String repo, int prNumber, List<InlineComment> comments) {
        if (githubToken.isBlank() || comments.isEmpty()) return;
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
                    new HttpEntity<>(mapper.writeValueAsString(body), githubHeaders()), String.class);
                log.info("Inline comment posted: {}:{}", c.path, c.line);
            } catch (Exception e) {
                log.warn("Inline comment failed {}:{} — {}", c.path, c.line, e.getMessage());
            }
        }
    }

    // ── GitHub Check Runs ──────────────────────────────────────

    private void postCheckRunWithAnnotations(String repo, String headSha,
                                              int score, boolean pass,
                                              List<Annotation> annotations,
                                              long durationMs) {
        if (githubToken.isBlank()) return;

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
                }).toList();

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
            if (!githubToken.isBlank()) h.set("Authorization", "token " + githubToken);

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

    // ── Inner types ────────────────────────────────────────────

    private record PRFile(String filename, String patch, String sha, String rawUrl) {}
    private record Annotation(String path, int startLine, int endLine,
                               String level, String message, String title) {}
    private record InlineComment(String path, int line, String commitId, String body) {}
    private record FileResult(String filename, int score, List<Issue> issues,
                               List<String> suggestions, String improvedCode) {}
    private record ReviewOutcome(int avgScore, boolean pass, int annotationCount) {}
}
