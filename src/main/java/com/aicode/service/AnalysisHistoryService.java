package com.aicode.service;

import com.aicode.analysis.AggregatorService;
import com.aicode.model.AnalysisRecord;
import com.aicode.model.Issue;
import com.aicode.model.Suggestion;
import com.aicode.repository.AnalysisRecordRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Manages analysis history and trend analysis.
 *
 * Features:
 *   - Persist analysis results for audit trail
 *   - Track score trends over time
 *   - Detect regressions (score dropped > 10%)
 *   - Generate improvement metrics
 *
 * Interview signal:
 *   "I built a versioning system for code quality metrics,
 *    enabling trend analysis and regression detection."
 */
@Service
public class AnalysisHistoryService {

    private static final Logger log = LoggerFactory.getLogger(AnalysisHistoryService.class);

    private final AnalysisRecordRepository recordRepository;
    private final ObjectMapper mapper = new ObjectMapper();

    public AnalysisHistoryService(AnalysisRecordRepository recordRepository) {
        this.recordRepository = recordRepository;
    }

    /**
     * Records an analysis result to the database.
     * Called after every analysis completion.
     */
    public AnalysisRecord recordAnalysis(String filePath, String repoName, String branchName,
                                         String commitHash, double score, int totalIssues,
                                         AggregatorService.AggregationReport report,
                                         List<Issue> issues, List<Suggestion> suggestions,
                                         long analysisTimeMs, long aiTimeMs,
                                         String source, String triggeredBy, String codeSnapshot) {
        try {
            AnalysisRecord record = new AnalysisRecord();
            record.setFilePath(filePath);
            record.setRepositoryName(repoName);
            record.setBranchName(branchName);
            record.setCommitHash(commitHash);
            record.setScore(score);
            record.setTotalIssues(totalIssues);
            record.setCriticalIssues(report.criticalCount);
            record.setHighIssues(report.highCount);
            record.setMediumIssues(report.mediumCount);
            record.setLowIssues(report.lowCount);
            record.setSuggestionCount(suggestions.size());
            record.setAnalysisTimeMs(analysisTimeMs);
            record.setAiTimeMs(aiTimeMs);
            record.setAnalyzedAt(LocalDateTime.now());
            record.setCodeSnapshot(truncate(codeSnapshot, 5000));
            record.setIssuesJson(mapper.writeValueAsString(issues));
            record.setSuggestionsJson(mapper.writeValueAsString(suggestions));
            record.setAnalysisSource(source);
            record.setTriggeredBy(triggeredBy);

            AnalysisRecord saved = recordRepository.save(record);
            log.info("Analysis recorded: id={}, file={}, score={:.1f}", saved.getId(), filePath, score);
            return saved;

        } catch (Exception e) {
            log.error("Failed to record analysis: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Analyzes trends for a file over the past N days.
     */
    public TrendAnalysis getTrend(String filePath, int days) {
        LocalDateTime since = LocalDateTime.now().minus(days, ChronoUnit.DAYS);
        List<AnalysisRecord> history = recordRepository.findByFilePathOrderByAnalyzedAtDesc(filePath);

        if (history.isEmpty()) {
            return new TrendAnalysis(filePath, 0, 0.0, 0, "NO_DATA");
        }

        // Latest vs oldest in range
        AnalysisRecord latest = history.get(0);
        AnalysisRecord oldest = history.stream()
                                       .filter(r -> r.getAnalyzedAt().isAfter(since))
                                       .reduce((a, b) -> b)  // get last (oldest)
                                       .orElse(history.get(history.size() - 1));

        double scoreChange = latest.getScore() - oldest.getScore();
        int issueChange = latest.getTotalIssues() - oldest.getTotalIssues();
        String trend = scoreChange > 5 ? "IMPROVED"
                     : scoreChange < -5 ? "REGRESSED"
                     : "STABLE";

        log.info("Trend for {}: {} days, score delta={}, issue delta={}", 
                 filePath, days, scoreChange, issueChange);

        return new TrendAnalysis(filePath, history.size(), scoreChange, (int) issueChange, trend);
    }

    /**
     * Detects if the latest analysis shows regression vs historical average.
     */
    public boolean isRegressionDetected(String filePath) {
        AnalysisRecord latest = recordRepository.findByFilePathOrderByAnalyzedAtDesc(filePath)
                .stream().findFirst().orElse(null);
        if (latest == null) return false;

        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minus(30, ChronoUnit.DAYS);
        List<AnalysisRecord> history = recordRepository.findByFilePathOrderByAnalyzedAtDesc(filePath);
        
        List<AnalysisRecord> recent = history.stream()
                                             .filter(r -> r.getAnalyzedAt().isAfter(thirtyDaysAgo))
                                             .toList();

        if (recent.size() < 2) return false;

        double avgScore = recent.stream()
                                .mapToDouble(AnalysisRecord::getScore)
                                .average()
                                .orElse(100.0);

        double regression = avgScore - latest.getScore();
        boolean isRegression = regression > 10.0;

        if (isRegression) {
            log.warn("REGRESSION DETECTED: {} — score dropped from {:.1f} to {:.1f}",
                     filePath, avgScore, latest.getScore());
        }

        return isRegression;
    }

    /**
     * Gets analysis history for a file.
     */
    public List<AnalysisRecord> getFileHistory(String filePath, int limit) {
        List<AnalysisRecord> allHistory = recordRepository.findByFilePathOrderByAnalyzedAtDesc(filePath);
        return allHistory.stream().limit(limit).toList();
    }

    /**
     * Gets repository-wide metrics.
     */
    public RepositoryMetrics getRepositoryMetrics(String repoName) {
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minus(7, ChronoUnit.DAYS);
        List<AnalysisRecord> recent = recordRepository.findByRepositoryNameAndAnalyzedAtBetween(
            repoName, sevenDaysAgo, LocalDateTime.now()
        );

        if (recent.isEmpty()) {
            return new RepositoryMetrics(repoName, 0, 0.0, 0, 0, 0);
        }

        double avgScore = recent.stream()
                                .mapToDouble(AnalysisRecord::getScore)
                                .average()
                                .orElse(0.0);

        int totalAnalyses = recent.size();
        int totalIssues = recent.stream()
                                .mapToInt(AnalysisRecord::getTotalIssues)
                                .sum();
        int criticalCount = recent.stream()
                                  .mapToInt(AnalysisRecord::getCriticalIssues)
                                  .sum();
        int highCount = recent.stream()
                              .mapToInt(AnalysisRecord::getHighIssues)
                              .sum();

        return new RepositoryMetrics(repoName, totalAnalyses, avgScore, criticalCount, highCount, totalIssues);
    }

    // ──── Helper methods ────────────────────────────────────

    private String truncate(String text, int maxLen) {
        if (text == null) return null;
        return text.length() > maxLen ? text.substring(0, maxLen) : text;
    }

    // ──── Result types ────────────────────────────────────

    public static class TrendAnalysis {
        public final String filePath;
        public final int sampleCount;
        public final double scoreChange;
        public final int issueChange;
        public final String trend;  // IMPROVED, REGRESSED, STABLE, NO_DATA

        public TrendAnalysis(String fp, int sc, double sd, int ic, String tr) {
            this.filePath = fp;
            this.sampleCount = sc;
            this.scoreChange = sd;
            this.issueChange = ic;
            this.trend = tr;
        }
    }

    public static class RepositoryMetrics {
        public final String repositoryName;
        public final int analysisCount;
        public final double avgScore;
        public final int criticalIssues;
        public final int highIssues;
        public final int totalIssues;

        public RepositoryMetrics(String rn, int ac, double sc, int ci, int hi, int ti) {
            this.repositoryName = rn;
            this.analysisCount = ac;
            this.avgScore = sc;
            this.criticalIssues = ci;
            this.highIssues = hi;
            this.totalIssues = ti;
        }
    }
}
