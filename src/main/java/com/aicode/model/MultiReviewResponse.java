package com.aicode.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class MultiReviewResponse {
    private int averageScore;
    private int totalIssues;
    private int filesAnalyzed;
    private List<FileResult> files;

    @Data
    @Builder
    public static class FileResult {
        private String name;
        private int score;
        private List<Issue> issues;
        private List<String> suggestions;
        private String improvedCode;
    }
}
