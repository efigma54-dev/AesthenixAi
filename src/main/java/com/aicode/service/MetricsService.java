package com.aicode.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Service for collecting and logging system metrics and observability data.
 * Tracks request times, AI latency, cache hit rates, and error rates.
 */
@Service
public class MetricsService {

  private static final Logger log = LoggerFactory.getLogger(MetricsService.class);

  // Request metrics
  private final AtomicLong totalRequests = new AtomicLong(0);
  private final AtomicLong totalRequestTime = new AtomicLong(0);
  private final AtomicLong cacheHits = new AtomicLong(0);
  private final AtomicLong cacheMisses = new AtomicLong(0);

  // AI metrics
  private final AtomicLong aiRequests = new AtomicLong(0);
  private final AtomicLong aiErrors = new AtomicLong(0);
  private final AtomicLong aiTotalLatency = new AtomicLong(0);

  // Error metrics
  private final AtomicLong totalErrors = new AtomicLong(0);

  /**
   * Record a completed analysis request.
   */
  public void recordAnalysis(long durationMs, boolean wasCached) {
    totalRequests.incrementAndGet();
    totalRequestTime.addAndGet(durationMs);

    if (wasCached) {
      cacheHits.incrementAndGet();
    } else {
      cacheMisses.incrementAndGet();
    }

    log.info("Analysis completed - duration: {}ms, cached: {}, avg_duration: {}ms",
        durationMs, wasCached, getAverageRequestTime());
  }

  /**
   * Record AI service call metrics.
   */
  public void recordAICall(long latencyMs, boolean success) {
    aiRequests.incrementAndGet();
    aiTotalLatency.addAndGet(latencyMs);

    if (!success) {
      aiErrors.incrementAndGet();
    }

    log.debug("AI call - latency: {}ms, success: {}, error_rate: {:.2f}%",
        latencyMs, success, getAIErrorRate());
  }

  /**
   * Record an error.
   */
  public void recordError(String errorType, String message) {
    totalErrors.incrementAndGet();
    log.warn("Error recorded - type: {}, message: {}", errorType, message);
  }

  /**
   * Get current metrics snapshot.
   */
  public MetricsSnapshot getMetrics() {
    return new MetricsSnapshot(
        totalRequests.get(),
        getAverageRequestTime(),
        getCacheHitRate(),
        aiRequests.get(),
        getAverageAILatency(),
        getAIErrorRate(),
        totalErrors.get());
  }

  private long getAverageRequestTime() {
    long requests = totalRequests.get();
    return requests > 0 ? totalRequestTime.get() / requests : 0;
  }

  private double getCacheHitRate() {
    long total = cacheHits.get() + cacheMisses.get();
    return total > 0 ? (double) cacheHits.get() / total * 100 : 0;
  }

  private long getAverageAILatency() {
    long requests = aiRequests.get();
    return requests > 0 ? aiTotalLatency.get() / requests : 0;
  }

  private double getAIErrorRate() {
    long requests = aiRequests.get();
    return requests > 0 ? (double) aiErrors.get() / requests * 100 : 0;
  }

  /**
   * Snapshot of current metrics.
   */
  public static class MetricsSnapshot {
    private final long totalRequests;
    private final long avgRequestTime;
    private final double cacheHitRate;
    private final long aiRequests;
    private final long avgAILatency;
    private final double aiErrorRate;
    private final long totalErrors;

    public MetricsSnapshot(long totalRequests, long avgRequestTime, double cacheHitRate,
        long aiRequests, long avgAILatency, double aiErrorRate, long totalErrors) {
      this.totalRequests = totalRequests;
      this.avgRequestTime = avgRequestTime;
      this.cacheHitRate = cacheHitRate;
      this.aiRequests = aiRequests;
      this.avgAILatency = avgAILatency;
      this.aiErrorRate = aiErrorRate;
      this.totalErrors = totalErrors;
    }

    public long getTotalRequests() {
      return totalRequests;
    }

    public long getAvgRequestTime() {
      return avgRequestTime;
    }

    public double getCacheHitRate() {
      return cacheHitRate;
    }

    public long getAiRequests() {
      return aiRequests;
    }

    public long getAvgAILatency() {
      return avgAILatency;
    }

    public double getAiErrorRate() {
      return aiErrorRate;
    }

    public long getTotalErrors() {
      return totalErrors;
    }
  }
}