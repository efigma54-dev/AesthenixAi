package com.aicode.service;

import com.aicode.model.PrioritizedJob;

/**
 * Abstraction over the async review job queue.
 *
 * All components that need to submit or query jobs depend on this interface,
 * not on the concrete ReviewJobService. This allows the in-memory implementation
 * to be replaced (e.g. with a Redis-backed queue) without changing callers.
 */
public interface ReviewQueueService {

    /**
     * Submit a prioritized job for async processing.
     *
     * @param job the job to enqueue, including priority and submit time
     * @return the jobId string if accepted, or null if rejected (caller should return HTTP 429)
     */
    String submit(PrioritizedJob job);

    /**
     * Get the current status entry for a job by ID.
     *
     * @param jobId the job identifier returned by submit()
     * @return the JobEntry, or null if the job does not exist or has expired
     */
    ReviewJobService.JobEntry get(String jobId);

    /**
     * Returns the number of jobs currently in QUEUED or RUNNING state.
     * Used for queue-cap enforcement and dashboard display.
     */
    int getActiveCount();

    /**
     * Returns the number of jobs currently in QUEUED state (not yet dispatched to a worker).
     * Used for failover threshold checks and observability alerts.
     */
    int getQueueDepth();
}
