package com.aicode.model;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Builder
public class ReportResponse {
    private String  reportId;
    private String  shareUrl;
    private Instant createdAt;
    private int     score;
    private List<Issue>  issues;
    private List<String> suggestions;
    private String  improvedCode;
    private ParsedCodeInfo parsedInfo;
}
