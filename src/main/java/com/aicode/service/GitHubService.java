package com.aicode.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service
public class GitHubService {

  private final RestTemplate restTemplate = new RestTemplate();
  private final ObjectMapper mapper = new ObjectMapper();
  private final ExecutorService executor = Executors.newFixedThreadPool(5);

  public static class GitHubFile {
    private String name;
    private String path;
    private String downloadUrl;
    private long size;

    public GitHubFile(String name, String path, String downloadUrl, long size) {
      this.name = name;
      this.path = path;
      this.downloadUrl = downloadUrl;
      this.size = size;
    }

    // Getters and setters
    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getPath() {
      return path;
    }

    public void setPath(String path) {
      this.path = path;
    }

    public String getDownloadUrl() {
      return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
      this.downloadUrl = downloadUrl;
    }

    public long getSize() {
      return size;
    }

    public void setSize(long size) {
      this.size = size;
    }
  }

  public List<GitHubFile> fetchJavaFiles(String repoUrl, String token) throws Exception {
    // Parse repo URL: https://github.com/owner/repo
    String[] parts = repoUrl.replace("https://github.com/", "").split("/");
    if (parts.length < 2) {
      throw new IllegalArgumentException("Invalid GitHub repository URL");
    }

    String owner = parts[0];
    String repo = parts[1];

    // GitHub API URL for repository tree
    String apiUrl = String.format("https://api.github.com/repos/%s/%s/git/trees/main?recursive=1", owner, repo);

    HttpHeaders headers = new HttpHeaders();
    headers.set("Accept", "application/vnd.github.v3+json");
    headers.set("User-Agent", "AI-Code-Reviewer/1.0");
    if (token != null && !token.isEmpty()) {
      headers.set("Authorization", "token " + token);
    }

    HttpEntity<String> entity = new HttpEntity<>(headers);

    try {
      ResponseEntity<String> response = restTemplate.exchange(apiUrl, HttpMethod.GET, entity, String.class);
      JsonNode root = mapper.readTree(response.getBody());

      List<GitHubFile> javaFiles = new ArrayList<>();

      if (root.has("tree")) {
        for (JsonNode node : root.get("tree")) {
          String path = node.get("path").asText();
          String type = node.get("type").asText();

          // Only include blob (file) entries that are Java files
          if ("blob".equals(type) && path.endsWith(".java") && !path.contains("/test/")) {
            String downloadUrl = String.format("https://raw.githubusercontent.com/%s/%s/main/%s", owner, repo, path);
            long size = node.has("size") ? node.get("size").asLong() : 0;

            // Skip very large files (> 1MB)
            if (size < 1024 * 1024) {
              javaFiles.add(new GitHubFile(
                  path.substring(path.lastIndexOf('/') + 1),
                  path,
                  downloadUrl,
                  size));
            }
          }
        }
      }

      return javaFiles;

    } catch (HttpClientErrorException e) {
      if (e.getStatusCode().value() == 404) {
        throw new Exception("Repository not found or not accessible");
      } else if (e.getStatusCode().value() == 403) {
        throw new Exception("Rate limit exceeded or repository is private (provide token)");
      } else {
        throw new Exception("GitHub API error: " + e.getMessage());
      }
    }
  }

  public String fetchFileContent(String downloadUrl, String token) throws Exception {
    HttpHeaders headers = new HttpHeaders();
    headers.set("User-Agent", "AI-Code-Reviewer/1.0");
    if (token != null && !token.isEmpty()) {
      headers.set("Authorization", "token " + token);
    }

    HttpEntity<String> entity = new HttpEntity<>(headers);

    try {
      ResponseEntity<String> response = restTemplate.exchange(downloadUrl, HttpMethod.GET, entity, String.class);
      return response.getBody();
    } catch (Exception e) {
      throw new Exception("Failed to fetch file content: " + e.getMessage());
    }
  }

  public CompletableFuture<String> fetchFileContentAsync(String downloadUrl, String token) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        return fetchFileContent(downloadUrl, token);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }, executor);
  }

  public void shutdown() {
    executor.shutdown();
  }
}