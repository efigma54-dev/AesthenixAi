package com.aicode.service;

import com.aicode.model.FeedbackRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Tracks user feedback on code reviews.
 * 
 * Strategic metrics:
 * - thumbsUpRatio: review quality
 * - contextBreakdown: where feedback comes from (vscode, github, web)
 * - trendOverTime: improving or declining?
 * 
 * This is CRITICAL for product improvement and trust building.
 */
@Service
public class FeedbackService {
    
    private static final Logger log = LoggerFactory.getLogger(FeedbackService.class);
    
    // Counters by feedback type
    private final AtomicLong thumbsUp = new AtomicLong(0);
    private final AtomicLong thumbsDown = new AtomicLong(0);
    private final AtomicLong helpful = new AtomicLong(0);
    private final AtomicLong notHelpful = new AtomicLong(0);
    private final AtomicLong incorrect = new AtomicLong(0);
    private final AtomicLong falsePositive = new AtomicLong(0);
    
    // Breakdown by context (vscode, github_pr, web_app)
    private final Map<String, AtomicLong> contextCounts = new ConcurrentHashMap<>();
    
    // Store recent feedback for analysis (last 1000)
    private final Map<String, FeedbackEntry> recentFeedback = new ConcurrentHashMap<>();
    private static final int MAX_RECENT = 1000;
    
    public void recordFeedback(FeedbackRequest request) {
        log.info("Feedback received: type={} reviewId={} context={}", 
            request.getType(), request.getReviewId(), request.getContext());
        
        // Increment counters
        switch (request.getType()) {
            case THUMBS_UP -> thumbsUp.incrementAndGet();
            case THUMBS_DOWN -> thumbsDown.incrementAndGet();
            case HELPFUL -> helpful.incrementAndGet();
            case NOT_HELPFUL -> notHelpful.incrementAndGet();
            case INCORRECT -> incorrect.incrementAndGet();
            case FALSE_POSITIVE -> falsePositive.incrementAndGet();
        }
        
        // Track context
        String context = request.getContext() != null ? request.getContext() : "unknown";
        contextCounts.computeIfAbsent(context, k -> new AtomicLong(0)).incrementAndGet();
        
        // Store recent feedback
        FeedbackEntry entry = new FeedbackEntry(
            request.getReviewId(),
            request.getType(),
            request.getContext(),
            request.getComment(),
            Instant.now()
        );
        
        recentFeedback.put(request.getReviewId(), entry);
        
        // Prune if too large
        if (recentFeedback.size() > MAX_RECENT) {
            // Remove oldest entries (simple approach: remove first 100)
            recentFeedback.keySet().stream()
                .limit(100)
                .forEach(recentFeedback::remove);
        }
    }
    
    public FeedbackMetrics getMetrics() {
        long totalPositive = thumbsUp.get() + helpful.get();
        long totalNegative = thumbsDown.get() + notHelpful.get() + incorrect.get() + falsePositive.get();
        long total = totalPositive + totalNegative;
        
        double thumbsUpRatio = total > 0 ? (double) totalPositive / total * 100 : 0.0;
        
        return FeedbackMetrics.builder()
            .thumbsUp(thumbsUp.get())
            .thumbsDown(thumbsDown.get())
            .helpful(helpful.get())
            .notHelpful(notHelpful.get())
            .incorrect(incorrect.get())
            .falsePositive(falsePositive.get())
            .totalPositive(totalPositive)
            .totalNegative(totalNegative)
            .total(total)
            .thumbsUpRatio(thumbsUpRatio)
            .contextBreakdown(getContextBreakdown())
            .build();
    }
    
    private Map<String, Long> getContextBreakdown() {
        Map<String, Long> breakdown = new ConcurrentHashMap<>();
        contextCounts.forEach((context, count) -> breakdown.put(context, count.get()));
        return breakdown;
    }
    
    public record FeedbackEntry(
        String reviewId,
        FeedbackRequest.FeedbackType type,
        String context,
        String comment,
        Instant timestamp
    ) {}
    
    @lombok.Builder
    @lombok.Data
    public static class FeedbackMetrics {
        private long thumbsUp;
        private long thumbsDown;
        private long helpful;
        private long notHelpful;
        private long incorrect;
        private long falsePositive;
        private long totalPositive;
        private long totalNegative;
        private long total;
        private double thumbsUpRatio;
        private Map<String, Long> contextBreakdown;
    }
}
