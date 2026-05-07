package com.aicode.service;

import com.aicode.model.RepoScanResponse;
import com.aicode.model.RepoScanResponse.FileResult;
import com.aicode.model.Issue;
import com.aicode.model.Suggestion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Repository scanner — fetches and analyzes all supported source files in a GitHub repo.
 *
 * Improvements over v1:
 *  - Multi-language: .java, .js, .ts, .py
 *  - Aggressive directory filtering: node_modules, dist, build, target, .git, __pycache__
 *  - File size limit: skip files > 100 KB
 *  - Parallel analysis: up to 4 concurrent file analyses
 *  - Language detection from file extension
 *  - Structured summary with per-language breakdown
 */
@Service
public class RepoScanService {

  private static final Logger log = LoggerFactory.getLogger(RepoScanService.class);

  // Supported file extensions → language hint for backend
  private static final Map<String, String> EXTENSION_TO_LANGUAGE = Map.of(
    ".java", "java",
    ".js",   "javascript",
    ".ts",   "typescript",
    ".py",   "python"
  );

  // Directories to skip entirely — generated code, dependencies, build artifacts
  private static final Set<String> IGNORED_DIRS = Set.of(
    "node_modules", "dist", "build", "target", ".git", ".github",
    "__pycache__", ".pytest_cache", "venv", ".venv", "env",
    "out", "bin", "obj", ".idea", ".vscode", "coverage"
  );

  private static final long MAX_FILE_BYTES = 100_000; // 100 KB

  private final GitHubService gitHubService;
  private final CodeReviewService codeReviewService;

  // Dedicated thread pool — isolated from the common pool
  private final ExecutorService analysisExecutor = Executors.newFixedThreadPool(4);

  public RepoScanService(GitHubService gitHubService, CodeReviewService codeReviewService) {
    this.gitHubService = gitHubService;
    this.codeReviewService = codeReviewService;
  }

  public RepoScanResponse scanRepository(String repoUrl, String token) {
    String repoName = extractRepoName(repoUrl);
    log.info("Starting repo scan: {}", repoName);

    try {
      // Fetch all supported source files
      List<GitHubService.GitHubFile> files = gitHubService.fetchJavaFiles(repoUrl, token);

      // Filter: only supported extensions, skip ignored dirs, skip large files
      List<GitHubService.GitHubFile> eligible = files.stream()
          .filter(f -> isSupportedFile(f.getPath()))
          .filter(f -> !isInIgnoredDir(f.getPath()))
          .collect(Collectors.toList());

      log.info("Repo scan {}: {} total files, {} eligible after filtering",
          repoName, files.size(), eligible.size());

      if (eligible.isEmpty()) {
        return new RepoScanResponse(repoName,
            "No supported source files found (.java, .js, .ts, .py) outside build/dependency directories.");
      }

      // Analyze files in parallel
      List<CompletableFuture<FileResult>> futures = eligible.stream()
          .map(f -> analyzeFileAsync(f, token))
          .collect(Collectors.toList());

      List<FileResult> results = futures.stream()
          .map(CompletableFuture::join)
          .collect(Collectors.toList());

      return aggregateResults(repoName, files.size(), results);

    } catch (Exception e) {
      log.error("Repo scan failed for {}: {}", repoName, e.getMessage());
      return new RepoScanResponse(repoName, e.getMessage());
    }
  }

  private CompletableFuture<FileResult> analyzeFileAsync(GitHubService.GitHubFile file, String token) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        String content = gitHubService.fetchFileContent(file.getDownloadUrl(), token);

        // Skip files that are too large after fetching
        if (content != null && content.length() > MAX_FILE_BYTES) {
          log.info("Skipping {} — too large ({} chars)", file.getPath(), content.length());
          return new FileResult(file.getName(), file.getPath(), "Skipped: file too large (> 100 KB)");
        }

        String language = detectLanguage(file.getPath());
        var review = codeReviewService.reviewCode(content, language);

        log.info("Analyzed {} (lang={}) — score={}", file.getPath(), language, review.getScore());

        return new FileResult(
            file.getName(),
            file.getPath(),
            review.getScore(),
            review.getIssues(),
            review.getSuggestions().stream()
                .map(s -> new Suggestion(s))
                .collect(Collectors.toList()));

      } catch (Exception e) {
        log.warn("Failed to analyze {}: {}", file.getPath(), e.getMessage());
        return new FileResult(file.getName(), file.getPath(), "Failed: " + e.getMessage());
      }
    }, analysisExecutor);
  }

  private RepoScanResponse aggregateResults(String repoName, int totalFiles, List<FileResult> results) {
    List<FileResult> successful = results.stream()
        .filter(r -> "completed".equals(r.getStatus()))
        .sorted(Comparator.comparingDouble(FileResult::getScore)) // worst first
        .collect(Collectors.toList());

    double overallScore = successful.stream()
        .mapToDouble(FileResult::getScore)
        .average()
        .orElse(0.0);

    List<Issue> allIssues = successful.stream()
        .flatMap(r -> r.getIssues().stream())
        .collect(Collectors.toList());

    List<Suggestion> allSuggestions = successful.stream()
        .flatMap(r -> r.getSuggestions().stream())
        .collect(Collectors.toList());

    log.info("Repo scan complete: {} analyzed, score={:.1f}, issues={}",
        successful.size(), overallScore, allIssues.size());

    return new RepoScanResponse(
        repoName, totalFiles, successful.size(),
        overallScore, successful, allIssues, allSuggestions);
  }

  // ── Helpers ────────────────────────────────────────────────

  private boolean isSupportedFile(String path) {
    if (path == null) return false;
    return EXTENSION_TO_LANGUAGE.keySet().stream()
        .anyMatch(ext -> path.toLowerCase().endsWith(ext));
  }

  private boolean isInIgnoredDir(String path) {
    if (path == null) return false;
    String[] parts = path.split("[/\\\\]");
    for (String part : parts) {
      if (IGNORED_DIRS.contains(part.toLowerCase())) return true;
    }
    return false;
  }

  private String detectLanguage(String path) {
    if (path == null) return "java";
    String lower = path.toLowerCase();
    for (Map.Entry<String, String> entry : EXTENSION_TO_LANGUAGE.entrySet()) {
      if (lower.endsWith(entry.getKey())) return entry.getValue();
    }
    return "java";
  }

  private String extractRepoName(String repoUrl) {
    String[] parts = repoUrl.replace("https://github.com/", "").split("/");
    return parts.length >= 2 ? parts[0] + "/" + parts[1] : "unknown-repo";
  }

  public void shutdown() {
    analysisExecutor.shutdown();
  }
}
