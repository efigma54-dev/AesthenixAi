package com.aicode.model;

import java.util.List;

public class RepoScanResponse {
    private String repoName;
    private int totalFiles;
    private int analyzedFiles;
    private double overallScore;
    private List<FileResult> files;
    private List<Issue> allIssues;
    private List<Suggestion> allSuggestions;
    private String status;
    private String errorMessage;

    public RepoScanResponse() {}

    public RepoScanResponse(String repoName, int totalFiles, int analyzedFiles,
            double overallScore, List<FileResult> files,
            List<Issue> allIssues, List<Suggestion> allSuggestions) {
        this.repoName = repoName; this.totalFiles = totalFiles;
        this.analyzedFiles = analyzedFiles; this.overallScore = overallScore;
        this.files = files; this.allIssues = allIssues;
        this.allSuggestions = allSuggestions; this.status = "completed";
    }

    public RepoScanResponse(String repoName, String errorMessage) {
        this.repoName = repoName; this.status = "error"; this.errorMessage = errorMessage;
    }

    public String getRepoName()                    { return repoName; }
    public void setRepoName(String v)              { this.repoName = v; }
    public int getTotalFiles()                     { return totalFiles; }
    public void setTotalFiles(int v)               { this.totalFiles = v; }
    public int getAnalyzedFiles()                  { return analyzedFiles; }
    public void setAnalyzedFiles(int v)            { this.analyzedFiles = v; }
    public double getOverallScore()                { return overallScore; }
    public void setOverallScore(double v)          { this.overallScore = v; }
    public List<FileResult> getFiles()             { return files; }
    public void setFiles(List<FileResult> v)       { this.files = v; }
    public List<Issue> getAllIssues()               { return allIssues; }
    public void setAllIssues(List<Issue> v)        { this.allIssues = v; }
    public List<Suggestion> getAllSuggestions()    { return allSuggestions; }
    public void setAllSuggestions(List<Suggestion> v) { this.allSuggestions = v; }
    public String getStatus()                      { return status; }
    public void setStatus(String v)                { this.status = v; }
    public String getErrorMessage()                { return errorMessage; }
    public void setErrorMessage(String v)          { this.errorMessage = v; }

    public static class FileResult {
        private String name, path, status, errorMessage;
        private double score;
        private List<Issue> issues;
        private List<Suggestion> suggestions;

        public FileResult() {}
        public FileResult(String name, String path, double score,
                List<Issue> issues, List<Suggestion> suggestions) {
            this.name = name; this.path = path; this.score = score;
            this.issues = issues; this.suggestions = suggestions; this.status = "completed";
        }
        public FileResult(String name, String path, String errorMessage) {
            this.name = name; this.path = path; this.status = "error"; this.errorMessage = errorMessage;
        }

        public String getName()                    { return name; }
        public void setName(String v)              { this.name = v; }
        public String getPath()                    { return path; }
        public void setPath(String v)              { this.path = v; }
        public double getScore()                   { return score; }
        public void setScore(double v)             { this.score = v; }
        public List<Issue> getIssues()             { return issues; }
        public void setIssues(List<Issue> v)       { this.issues = v; }
        public List<Suggestion> getSuggestions()   { return suggestions; }
        public void setSuggestions(List<Suggestion> v) { this.suggestions = v; }
        public String getStatus()                  { return status; }
        public void setStatus(String v)            { this.status = v; }
        public String getErrorMessage()            { return errorMessage; }
        public void setErrorMessage(String v)      { this.errorMessage = v; }
    }
}
