package com.aicode.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class GithubReviewResponse {
    private String repoName;
    private String description;
    private int averageScore;
    private int totalIssues;
    private int filesAnalyzed;
    private List<FileReviewResult> files;

    @Data
    @Builder
    public static class FileReviewResult {
        private String name;
        private int score;
        private List<Issue> issues;
        private List<String> suggestions;
    }
}
