package com.aicode.service;

import com.aicode.model.RepoScanRequest;
import com.aicode.model.RepoScanResponse;
import com.aicode.model.RepoScanResponse.FileResult;
import com.aicode.model.Issue;
import com.aicode.model.Suggestion;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service
public class RepoScanService {

  private final GitHubService gitHubService;
  private final CodeReviewService codeReviewService;
  private final ExecutorService analysisExecutor = Executors.newFixedThreadPool(3); // Limit concurrency

  public RepoScanService(GitHubService gitHubService, CodeReviewService codeReviewService) {
    this.gitHubService = gitHubService;
    this.codeReviewService = codeReviewService;
  }

  public RepoScanResponse scanRepository(String repoUrl, String token) {
    try {
      // Step 1: Fetch all Java files from the repository
      List<GitHubService.GitHubFile> javaFiles = gitHubService.fetchJavaFiles(repoUrl, token);

      if (javaFiles.isEmpty()) {
        return new RepoScanResponse(extractRepoName(repoUrl), "No Java files found in repository");
      }

      // Step 2: Analyze files concurrently
      List<CompletableFuture<FileResult>> analysisFutures = javaFiles.stream()
          .map(file -> analyzeFileAsync(file, token))
          .collect(Collectors.toList());

      // Wait for all analyses to complete
      List<FileResult> fileResults = analysisFutures.stream()
          .map(CompletableFuture::join)
          .collect(Collectors.toList());

      // Step 3: Aggregate results
      return aggregateResults(extractRepoName(repoUrl), javaFiles.size(), fileResults);

    } catch (Exception e) {
      return new RepoScanResponse(extractRepoName(repoUrl), e.getMessage());
    }
  }

  private CompletableFuture<FileResult> analyzeFileAsync(GitHubService.GitHubFile file, String token) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        // Fetch file content
        String content = gitHubService.fetchFileContent(file.getDownloadUrl(), token);

        // Analyze the code
        var reviewResult = codeReviewService.reviewCode(content);

        return new FileResult(
            file.getName(),
            file.getPath(),
            reviewResult.getScore(),
            reviewResult.getIssues(),
            reviewResult.getSuggestions().stream()
                .map(s -> new com.aicode.model.Suggestion(s))
                .collect(Collectors.toList()));

      } catch (Exception e) {
        return new FileResult(file.getName(), file.getPath(), "Failed to analyze: " + e.getMessage());
      }
    }, analysisExecutor);
  }

  private RepoScanResponse aggregateResults(String repoName, int totalFiles, List<FileResult> fileResults) {
    // Separate successful and failed analyses
    List<FileResult> successfulResults = fileResults.stream()
        .filter(result -> "completed".equals(result.getStatus()))
        .collect(Collectors.toList());

    int analyzedFiles = successfulResults.size();

    // Calculate overall score (weighted average)
    double overallScore = successfulResults.stream()
        .mapToDouble(FileResult::getScore)
        .average()
        .orElse(0.0);

    // Sort files by score (worst first)
    successfulResults.sort(Comparator.comparingDouble(FileResult::getScore));

    // Aggregate all issues and suggestions
    List<Issue> allIssues = successfulResults.stream()
        .flatMap(result -> result.getIssues().stream())
        .collect(Collectors.toList());

    List<Suggestion> allSuggestions = successfulResults.stream()
        .flatMap(result -> result.getSuggestions().stream())
        .collect(Collectors.toList());

    RepoScanResponse response = new RepoScanResponse(
        repoName, totalFiles, analyzedFiles, overallScore,
        successfulResults, allIssues, allSuggestions);

    return response;
  }

  private String extractRepoName(String repoUrl) {
    // Extract repo name from URL: https://github.com/owner/repo -> owner/repo
    String[] parts = repoUrl.replace("https://github.com/", "").split("/");
    if (parts.length >= 2) {
      return parts[0] + "/" + parts[1];
    }
    return "unknown-repo";
  }

  public void shutdown() {
    analysisExecutor.shutdown();
  }
}