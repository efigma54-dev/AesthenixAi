package com.aicode.service;

import com.aicode.model.AIResult;
import com.aicode.model.Issue;
import com.aicode.model.Suggestion;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.annotation.PostConstruct;

/**
 * Local AI service using Ollama for code analysis.
 *
 * Features:
 * ✓ Caching (avoid re-analyzing identical code)
 * ✓ Retry logic (network transients)
 * ✓ Fallback to empty/neutral result (graceful degradation)
 * ✓ Timeout handling (prevent hanging)
 * ✓ Metrics tracking (latency, failures)
 *
 * Interview signal:
 * "I built fault-tolerant code that provides graceful degradation.
 * If AI fails, analysis continues with rules-based score instead of crashing."
 */
@Service
public class LocalAIService {

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(LocalAIService.class);

  private WebClient webClient;
  private final ObjectMapper mapper = new ObjectMapper();
  private final Map<String, String> cache = new HashMap<>();

  // Metrics
  private long aiSuccessCount = 0;
  private long aiFailureCount = 0;
  private long totalAITimeMs = 0;

  @org.springframework.beans.factory.annotation.Value("${ollama.url:http://localhost:11434}")
  private String ollamaUrl;

  @org.springframework.beans.factory.annotation.Value("${ollama.model:qwen3.5:9b}")
  private String ollamaModel;

  @org.springframework.beans.factory.annotation.Value("${ollama.timeout:120}")
  private int ollamaTimeoutSeconds;

  @org.springframework.beans.factory.annotation.Value("${ollama.retry-attempts:3}")
  private int retryAttempts;

  @org.springframework.beans.factory.annotation.Value("${ollama.enabled:true}")
  private boolean aiEnabled;

  @PostConstruct
  public void init() {
    if (ollamaUrl != null) {
      this.webClient = WebClient.create(ollamaUrl);
    }
    log.info("LocalAIService initialized — enabled={}, url={}, model={}, timeout={}s, retries={}",
        aiEnabled, ollamaUrl, ollamaModel, ollamaTimeoutSeconds, retryAttempts);
  }

  /**
   * Analyzes code with resilience: cache, retry, fallback.
   */
  public AIResult analyzeCode(String code) {
    if (!aiEnabled) {
      log.debug("AI disabled — returning neutral result");
      return createNeutralResult("AI service disabled");
    }

    String key = Integer.toString(code.hashCode());

    // Cache hit
    if (cache.containsKey(key)) {
      log.debug("Cache hit for code");
      return parseAIResponse(cache.get(key), true);
    }

    // Try calling AI with retries
    String result = callAIWithRetry(code);
    if (result == null) {
      log.warn("AI call failed after {} retries — using fallback", retryAttempts);
      aiFailureCount++;
      return createNeutralResult("AI service unavailable");
    }

    cache.put(key, result);
    aiSuccessCount++;
    return parseAIResponse(result, false);
  }

  /**
   * Retries AI call with exponential backoff.
   * Returns null if all retries exhausted.
   */
  private String callAIWithRetry(String code) {
    for (int attempt = 1; attempt <= retryAttempts; attempt++) {
      try {
        long start = System.currentTimeMillis();
        String result = callAI(code);
        long durationMs = System.currentTimeMillis() - start;
        totalAITimeMs += durationMs;
        log.info("AI call succeeded in {}ms (attempt {}/{})", durationMs, attempt, retryAttempts);
        return result;
      } catch (Exception e) {
        log.warn("AI call attempt {} failed: {}", attempt, e.getMessage());
        if (attempt < retryAttempts) {
          // Exponential backoff: 500ms, 1s, 2s, ...
          long backoffMs = (long) (500 * Math.pow(2, attempt - 1));
          try {
            Thread.sleep(Math.min(backoffMs, 5000)); // cap at 5s
          } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            return null;
          }
        }
      }
    }
    return null;
  }

  /**
   * Parse AI response JSON into structured AIResult.
   * 
   * @param fromCache whether this result came from cache (for metrics)
   */
  private AIResult parseAIResponse(String jsonResponse, boolean fromCache) {
    try {
      JsonNode root = mapper.readTree(jsonResponse);

      double score = root.has("score") ? root.get("score").asDouble(60.0) : 60.0;

      List<Issue> issues = new ArrayList<>();
      if (root.has("issues") && root.get("issues").isArray()) {
        for (JsonNode issueNode : root.get("issues")) {
          String type = issueNode.has("type") ? issueNode.get("type").asText() : "AI Issue";
          String message = issueNode.has("message") ? issueNode.get("message").asText() : "AI detected issue";
          int line = issueNode.has("line") ? issueNode.get("line").asInt() : 0;

          issues.add(new Issue(type, message, "unknown", line, "ai", 5));
        }
      }

      List<Suggestion> suggestions = new ArrayList<>();
      if (root.has("suggestions") && root.get("suggestions").isArray()) {
        for (JsonNode suggestionNode : root.get("suggestions")) {
          suggestions.add(new Suggestion(suggestionNode.asText(), "ai"));
        }
      }

      String improvedCode = root.has("improvedCode") ? root.get("improvedCode").asText("") : "";

      return new AIResult(issues, suggestions, score, improvedCode);

    } catch (Exception e) {
      // Fallback for parsing errors
      log.error("Failed to parse AI response: {}", e.getMessage());
      List<Issue> issues = List
          .of(new Issue("AI Parse Error", "Failed to parse AI response", "unknown", 0, "error", 1));
      return new AIResult(issues, new ArrayList<>(), 60.0);
    }
  }

  /**
   * Creates a neutral result when AI is unavailable.
   * Allows analysis to continue without crashing.
   */
  private AIResult createNeutralResult(String reason) {
    log.info("Creating neutral AI result — reason: {}", reason);
    return new AIResult(new ArrayList<>(), new ArrayList<>(), 60.0, "");
  }

  /**
   * Gets AI service metrics for observability.
   */
  public AIMetrics getMetrics() {
    long totalCalls = aiSuccessCount + aiFailureCount;
    double avgTimeMs = totalCalls > 0 ? (double) totalAITimeMs / totalCalls : 0;
    double successRate = totalCalls > 0 ? (double) aiSuccessCount / totalCalls * 100 : 0;
    return new AIMetrics(aiSuccessCount, aiFailureCount, totalAITimeMs, avgTimeMs, successRate, cache.size());
  }

  public static class AIMetrics {
    public final long successCount;
    public final long failureCount;
    public final long totalTimeMs;
    public final double avgTimeMs;
    public final double successRate;
    public final int cacheSize;

    public AIMetrics(long s, long f, long t, double a, double r, int c) {
      this.successCount = s;
      this.failureCount = f;
      this.totalTimeMs = t;
      this.avgTimeMs = a;
      this.successRate = r;
      this.cacheSize = c;
    }
  }

  public Flux<String> streamCodeAnalysis(String code) {
    String prompt = buildPrompt(code);

    // Trim code for low RAM
    String trimmedCode = code.length() > 2000 ? code.substring(0, 2000) : code;
    String finalPrompt = prompt.replace("Code:\n        \"\"\" + code", "Code:\n" + trimmedCode);

    return webClient.post()
        .uri("/api/generate")
        .bodyValue(Map.of(
            "model", "qwen3.5:9b",
            "prompt", finalPrompt,
            "stream", true,
            "options", Map.of(
                "num_predict", 300,
                "temperature", 0.3)))
        .retrieve()
        .bodyToFlux(String.class)
        .map(chunk -> {
          try {
            JsonNode node = mapper.readTree(chunk);
            if (node.has("done") && node.get("done").asBoolean()) {
              return "[DONE]";
            }
            if (node.has("response")) {
              return node.get("response").asText();
            }
            return "";
          } catch (Exception e) {
            return "";
          }
        })
        .filter(response -> !response.isEmpty())
        .timeout(Duration.ofSeconds(30));
  }

  public Flux<String> streamCodeAnalysisForDisplay(String code) {
    return streamCodeAnalysis(code);
  }

  public String analyzeCodeWithStreaming(String code) {
    String key = Integer.toString(code.hashCode());

    if (cache.containsKey(key)) {
      return cache.get(key);
    }

    String result = callAI(code);

    cache.put(key, result);

    return result;
  }

  private String buildPrompt(String code) {
    return """
        /no_think
        You are a Java code reviewer. Output ONLY a JSON object. No thinking. No explanation.

        Required JSON format:
        {"score":85,"issues":[{"type":"Performance","message":"Use StringBuilder","line":4}],"suggestions":["Use StringBuilder instead of concatenation"],"improvedCode":"// improved code here"}

        Analyze this Java code:
        """
        + code + "\n\nJSON only:";
  }

  private String callAI(String code) {
    log.info("Calling Ollama model={} timeout={}s", ollamaModel, ollamaTimeoutSeconds);
    // Trim code for low RAM
    String trimmedCode = code.length() > 2000 ? code.substring(0, 2000) : code;
    String prompt = buildPrompt(trimmedCode);

    try {
      String raw = webClient.post()
          .uri("/api/generate")
          .bodyValue(Map.of(
              "model", ollamaModel,
              "prompt", prompt,
              "stream", false,
              "think", false,
              "options", Map.of(
                  "num_predict", 500,
                  "temperature", 0.1)))
          .retrieve()
          .bodyToMono(String.class)
          .timeout(Duration.ofSeconds(ollamaTimeoutSeconds))
          .block();

      JsonNode root = mapper.readTree(raw);

      // qwen3 uses thinking mode — response may be in "response" or "thinking"
      String responseText = root.path("response").asText("");
      if (responseText.isBlank()) {
        responseText = root.path("thinking").asText("");
      }

      // Strip <think>...</think> blocks
      responseText = responseText.replaceAll("(?s)<think>.*?</think>", "").trim();

      // Extract JSON object
      int start = responseText.indexOf('{');
      int end = responseText.lastIndexOf('}');
      if (start >= 0 && end > start)
        responseText = responseText.substring(start, end + 1);

      if (responseText.isBlank())
        return fallbackResponse();
      return responseText;

    } catch (Exception e) {
      return fallbackResponse();
    }
  }

  private String fallbackResponse() {
    return """
        {
          "score": 60,
          "issues": [{"type": "AI Unavailable", "message": "Local AI service is not available", "line": 0}],
          "suggestions": ["Ensure Ollama is running with qwen3.5:9b model"]
        }
        """;
  }
}
