package com.aicode.service;

import com.aicode.model.GithubReviewResponse;
import com.aicode.model.ReviewResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class GithubAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(GithubAnalysisService.class);
    private static final Pattern REPO_PATTERN = Pattern.compile("github\\.com/([^/]+)/([^/\\s?#]+)");

    private final WebClient githubClient;
    private final CodeReviewService codeReviewService;
    private final ObjectMapper objectMapper;
    private final Executor taskExecutor; // Spring's configured thread pool
    private final int maxFiles;

    public GithubAnalysisService(
            CodeReviewService codeReviewService,
            ObjectMapper objectMapper,
            Executor taskExecutor,
            @Value("${github.token:}") String githubToken,
            @Value("${github.max-files:15}") int maxFiles) {

        this.codeReviewService = codeReviewService;
        this.objectMapper = objectMapper;
        this.taskExecutor = taskExecutor;
        this.maxFiles = maxFiles;

        WebClient.Builder builder = WebClient.builder()
                .baseUrl("https://api.github.com")
                .defaultHeader("Accept", "application/vnd.github.v3+json")
                .defaultHeader("User-Agent", "ai-code-reviewer");

        if (githubToken != null && !githubToken.isBlank())
            builder.defaultHeader("Authorization", "Bearer " + githubToken);

        this.githubClient = builder.build();
    }

    public GithubReviewResponse analyzeRepo(String repoUrl) {
        String[] ownerRepo = parseOwnerRepo(repoUrl);
        String owner = ownerRepo[0];
        String repo = ownerRepo[1];
        log.info("Analyzing GitHub repo: {}/{}", owner, repo);

        // Fetch metadata
        String description = "";
        try {
            String metaBody = githubClient.get()
                    .uri("/repos/{owner}/{repo}", owner, repo)
                    .retrieve().bodyToMono(String.class).block();
            JsonNode meta = objectMapper.readTree(metaBody);
            description = meta.path("description").asText("");
        } catch (Exception e) {
            log.warn("Could not fetch repo metadata: {}", e.getMessage());
        }

        // Collect .java files
        List<JavaFile> javaFiles = new ArrayList<>();
        collectJavaFiles(owner, repo, "", javaFiles);

        if (javaFiles.isEmpty())
            throw new RuntimeException("No .java files found in: " + owner + "/" + repo);

        log.info("Found {} .java files — analyzing in parallel", javaFiles.size());

        // ── Parallel analysis via CompletableFuture ────────────
        // Timeout per-file = local AI request timeout (10s) + buffer; total cap = 5
        // minutes
        long perFileTimeoutSec = 35;

        List<CompletableFuture<GithubReviewResponse.FileReviewResult>> futures = javaFiles.stream()
                .map(file -> CompletableFuture
                        .supplyAsync(() -> analyzeFile(file), taskExecutor)
                        .orTimeout(perFileTimeoutSec, java.util.concurrent.TimeUnit.SECONDS)
                        .exceptionally(ex -> {
                            log.warn("File {} timed out or failed: {}", file.path(), ex.getMessage());
                            return null;
                        }))
                .toList();

        // Wait for all futures — each already has its own timeout via orTimeout()
        List<GithubReviewResponse.FileReviewResult> results = futures.stream()
                .map(f -> {
                    try {
                        return f.join();
                    } catch (Exception e) {
                        log.warn("File analysis failed: {}", e.getMessage());
                        return null;
                    }
                })
                .filter(r -> r != null)
                .toList();

        int totalScore = results.stream().mapToInt(GithubReviewResponse.FileReviewResult::getScore).sum();
        int totalIssues = results.stream().mapToInt(r -> r.getIssues() != null ? r.getIssues().size() : 0).sum();
        int avgScore = results.isEmpty() ? 0 : totalScore / results.size();

        log.info("Repo analysis complete — files={}, avgScore={}, issues={}", results.size(), avgScore, totalIssues);

        return GithubReviewResponse.builder()
                .repoName(owner + "/" + repo)
                .description(description)
                .averageScore(avgScore)
                .totalIssues(totalIssues)
                .filesAnalyzed(results.size())
                .files(results)
                .build();
    }

    // ── Per-file analysis (runs on thread pool) ────────────────
    private GithubReviewResponse.FileReviewResult analyzeFile(JavaFile file) {
        try {
            String code = fetchRawFile(file.downloadUrl());
            ReviewResponse review = codeReviewService.reviewCode(code);
            return GithubReviewResponse.FileReviewResult.builder()
                    .name(file.path())
                    .score(review.getScore())
                    .issues(review.getIssues())
                    .suggestions(review.getSuggestions())
                    .build();
        } catch (Exception e) {
            log.warn("Skipping file {} — {}", file.path(), e.getMessage());
            throw e; // let the caller handle via null filter
        }
    }

    // ── Helpers ────────────────────────────────────────────────
    private String[] parseOwnerRepo(String url) {
        Matcher m = REPO_PATTERN.matcher(url);
        if (!m.find())
            throw new RuntimeException("Invalid GitHub URL: " + url);
        return new String[] { m.group(1), m.group(2).replaceAll("\\.git$", "") };
    }

    private void collectJavaFiles(String owner, String repo, String path, List<JavaFile> collected) {
        if (collected.size() >= maxFiles)
            return;
        try {
            String body = githubClient.get()
                    .uri("/repos/{owner}/{repo}/contents/{path}", owner, repo, path)
                    .retrieve().bodyToMono(String.class).block();

            JsonNode items = objectMapper.readTree(body);
            if (!items.isArray())
                return;

            for (JsonNode item : items) {
                if (collected.size() >= maxFiles)
                    break;
                String type = item.path("type").asText();
                String name = item.path("name").asText();

                if ("file".equals(type) && name.endsWith(".java")) {
                    collected.add(new JavaFile(item.path("path").asText(), item.path("download_url").asText()));
                } else if ("dir".equals(type) && !name.startsWith(".")) {
                    collectJavaFiles(owner, repo, item.path("path").asText(), collected);
                }
            }
        } catch (WebClientResponseException e) {
            if (e.getStatusCode().value() == 403)
                throw new RuntimeException("GitHub rate limit exceeded. Set GITHUB_TOKEN to increase limit.");
        } catch (Exception e) {
            log.debug("Skipping directory {}: {}", path, e.getMessage());
        }
    }

    private String fetchRawFile(String downloadUrl) {
        return WebClient.create().get().uri(downloadUrl)
                .retrieve().bodyToMono(String.class).block();
    }

    private record JavaFile(String path, String downloadUrl) {
    }
}
