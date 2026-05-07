package com.aicode.model;

import java.time.Instant;

/**
 * Wraps a ReviewRequest with its assigned priority and submit time.
 *
 * Implements Comparable so PriorityBlockingQueue orders jobs correctly:
 *   - Higher priority (HIGH > MEDIUM > LOW) is dispatched first.
 *   - Ties within the same priority are broken by earlier submitTime (FIFO).
 */
public record PrioritizedJob(
        ReviewRequest request,
        Priority      priority,
        Instant       submitTime
) implements Comparable<PrioritizedJob> {

    @Override
    public int compareTo(PrioritizedJob other) {
        // Higher ordinal = higher priority → reverse ordinal comparison puts HIGH first
        int cmp = other.priority().ordinal() - this.priority().ordinal();
        if (cmp != 0) return cmp;
        // FIFO within same priority: earlier submitTime comes first
        return this.submitTime().compareTo(other.submitTime());
    }
}
