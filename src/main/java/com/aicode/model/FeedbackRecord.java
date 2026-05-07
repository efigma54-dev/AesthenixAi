package com.aicode.model;

import java.time.Instant;

/**
 * A single thumbs-up/thumbs-down feedback entry for an analysis result.
 *
 * issueId is a deterministic identifier: SHA-256(issueType + ":" + lineNumber).
 * If not provided by the client, it defaults to SHA-256(issueType + ":0").
 */
public record FeedbackRecord(
        String  jobId,
        String  issueId,    // deterministic: SHA-256(issueType + ":" + lineNumber)
        String  issueType,  // e.g. "Performance", "Security", "Style"
        boolean helpful,
        Instant timestamp
) {}
