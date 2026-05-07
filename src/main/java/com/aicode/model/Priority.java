package com.aicode.model;

/**
 * Job priority for the async review queue.
 *
 * Ordinal order matters — higher ordinal = higher priority in PriorityBlockingQueue comparator.
 * Assignment rules:
 *   HIGH   — PR review webhook jobs (PRReviewBotService)
 *   MEDIUM — manual /api/review/async submissions (CodeReviewController)
 *   LOW    — repository scan jobs (RepoScanService)
 */
public enum Priority {
    LOW,
    MEDIUM,
    HIGH;

    /** Human-readable label for logging and API responses. */
    public String label() {
        return name();
    }
}
