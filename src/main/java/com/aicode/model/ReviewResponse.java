package com.aicode.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ReviewResponse {
    private int score;
    private List<Issue> issues;
    private List<String> suggestions;
    private String improvedCode;
    private ParsedCodeInfo parsedInfo;

    /** True when AI analysis was bypassed (daily limit reached, failover mode, or AI disabled). */
    private boolean aiSkipped;

    /** Human-readable reason why AI was skipped, or null when AI ran normally. */
    private String aiSkipReason;
}
