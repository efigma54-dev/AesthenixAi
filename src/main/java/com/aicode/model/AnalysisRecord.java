package com.aicode.model;

import java.time.LocalDateTime;

/**
 * Represents a persisted code analysis result.
 * Used for history tracking, trend analysis, and dashboard metrics.
 *
 * Currently stored in-memory (ReportService).
 * To persist to a database, add spring-data-jpa + a datasource and
 * annotate with @Entity / @Table.
 */
public class AnalysisRecord {

    private Long          id;
    private String        filePath;
    private String        repositoryName;
    private String        branchName;
    private String        commitHash;
    private Double        score;
    private Integer       totalIssues;
    private Integer       criticalIssues;
    private Integer       highIssues;
    private Integer       mediumIssues;
    private Integer       lowIssues;
    private Integer       suggestionCount;
    private Long          analysisTimeMs;
    private Long          aiTimeMs;
    private LocalDateTime analyzedAt;
    private String        codeSnapshot;
    private String        issuesJson;
    private String        suggestionsJson;
    private String        analysisSource;
    private String        triggeredBy;

    public AnalysisRecord() {}

    // ── Getters & Setters ──────────────────────────────────────
    public Long          getId()               { return id; }
    public void          setId(Long v)         { this.id = v; }
    public String        getFilePath()         { return filePath; }
    public void          setFilePath(String v) { this.filePath = v; }
    public String        getRepositoryName()   { return repositoryName; }
    public void          setRepositoryName(String v) { this.repositoryName = v; }
    public String        getBranchName()       { return branchName; }
    public void          setBranchName(String v) { this.branchName = v; }
    public String        getCommitHash()       { return commitHash; }
    public void          setCommitHash(String v) { this.commitHash = v; }
    public Double        getScore()            { return score; }
    public void          setScore(Double v)    { this.score = v; }
    public Integer       getTotalIssues()      { return totalIssues; }
    public void          setTotalIssues(Integer v) { this.totalIssues = v; }
    public Integer       getCriticalIssues()   { return criticalIssues; }
    public void          setCriticalIssues(Integer v) { this.criticalIssues = v; }
    public Integer       getHighIssues()       { return highIssues; }
    public void          setHighIssues(Integer v) { this.highIssues = v; }
    public Integer       getMediumIssues()     { return mediumIssues; }
    public void          setMediumIssues(Integer v) { this.mediumIssues = v; }
    public Integer       getLowIssues()        { return lowIssues; }
    public void          setLowIssues(Integer v) { this.lowIssues = v; }
    public Integer       getSuggestionCount()  { return suggestionCount; }
    public void          setSuggestionCount(Integer v) { this.suggestionCount = v; }
    public Long          getAnalysisTimeMs()   { return analysisTimeMs; }
    public void          setAnalysisTimeMs(Long v) { this.analysisTimeMs = v; }
    public Long          getAiTimeMs()         { return aiTimeMs; }
    public void          setAiTimeMs(Long v)   { this.aiTimeMs = v; }
    public LocalDateTime getAnalyzedAt()       { return analyzedAt; }
    public void          setAnalyzedAt(LocalDateTime v) { this.analyzedAt = v; }
    public String        getCodeSnapshot()     { return codeSnapshot; }
    public void          setCodeSnapshot(String v) { this.codeSnapshot = v; }
    public String        getIssuesJson()       { return issuesJson; }
    public void          setIssuesJson(String v) { this.issuesJson = v; }
    public String        getSuggestionsJson()  { return suggestionsJson; }
    public void          setSuggestionsJson(String v) { this.suggestionsJson = v; }
    public String        getAnalysisSource()   { return analysisSource; }
    public void          setAnalysisSource(String v) { this.analysisSource = v; }
    public String        getTriggeredBy()      { return triggeredBy; }
    public void          setTriggeredBy(String v) { this.triggeredBy = v; }
}
