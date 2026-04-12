package com.aicode.analysis;

import com.aicode.model.Issue;
import com.aicode.model.Suggestion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Aggregates results from multiple analysis sources (rules, AI, graph analysis).
 *
 * Pipeline stage:
 *   Rule Results + AI Results + Graph Results → [Aggregator] → Merged Issues + Suggestions
 *
 * Responsibilities:
 *   - Deduplication (same issue reported by multiple sources)
 *   - Sorting (by severity, line number)
 *   - Weighting (AI > Rules for same severity)
 *   - Filtering (optionally remove low-confidence issues)
 *
 * Interview signal:
 *   "I designed an aggregation layer that intelligently merges multiple analysis sources,
 *    avoiding duplicate reports and prioritizing high-signal issues."
 */
@Service
public class AggregatorService {

    private static final Logger log = LoggerFactory.getLogger(AggregatorService.class);

    /**
     * Merges issues from multiple sources with deduplication.
     */
    public List<Issue> mergeIssues(List<Issue> ruleIssues, List<Issue> aiIssues, List<Issue> graphIssues) {
        long startMs = System.currentTimeMillis();

        // Combine all issues
        List<Issue> combined = new ArrayList<>();
        combined.addAll(ruleIssues);
        combined.addAll(aiIssues);
        combined.addAll(graphIssues);

        // Deduplicate similar issues
        Map<String, Issue> deduplicated = deduplicateIssues(combined);

        // Sort by severity (desc) then line (asc)
        List<Issue> sorted = deduplicated.values().stream()
                                        .sorted(Comparator.comparingInt(Issue::getSeverity).reversed()
                                                         .thenComparingInt(Issue::getLine))
                                        .collect(Collectors.toList());

        long durationMs = System.currentTimeMillis() - startMs;
        log.info("Aggregated {} issues in {}ms (rules={}, ai={}, graph={})",
                 sorted.size(), durationMs, ruleIssues.size(), aiIssues.size(), graphIssues.size());

        return sorted;
    }

    /**
     * Deduplicates issues by normalizing their signatures.
     * Same issue type + line number = deduplicated.
     */
    private Map<String, Issue> deduplicateIssues(List<Issue> issues) {
        Map<String, Issue> dedupMap = new LinkedHashMap<>();

        for (Issue issue : issues) {
            // Create a signature: type + line
            String signature = normalizeSignature(issue.getType(), issue.getLine());

            // If already seen, merge by taking the higher severity
            if (dedupMap.containsKey(signature)) {
                Issue existing = dedupMap.get(signature);
                if (issue.getSeverity() > existing.getSeverity()) {
                    dedupMap.put(signature, issue);  // upgrade severity
                }
            } else {
                dedupMap.put(signature, issue);
            }
        }

        log.debug("Deduplicated {} → {} unique issues", issues.size(), dedupMap.size());
        return dedupMap;
    }

    private String normalizeSignature(String type, int line) {
        return String.format("%s:%d", type.toLowerCase(), line);
    }

    /**
     * Merges suggestions from multiple sources, removing duplicates.
     */
    public List<Suggestion> mergeSuggestions(List<Suggestion> ruleSuggestions,
                                              List<Suggestion> aiSuggestions) {
        Set<String> seen = new HashSet<>();
        List<Suggestion> merged = new ArrayList<>();

        // Add AI suggestions first (higher priority)
        for (Suggestion s : aiSuggestions) {
            if (seen.add(normalizeSuggestion(s.getMessage()))) {
                merged.add(s);
            }
        }
        for (Suggestion s : ruleSuggestions) {
            if (seen.add(normalizeSuggestion(s.getMessage()))) {
                merged.add(s);
            }
        }

        log.debug("Aggregated suggestions: ai={}, rules={}, final={}", 
                  aiSuggestions.size(), ruleSuggestions.size(), merged.size());

        return merged;
    }

    private String normalizeSuggestion(String text) {
        if (text == null || text.isEmpty()) return "";
        String lower = text.toLowerCase().replaceAll("[^a-z0-9]", "");
        return lower.substring(0, Math.min(30, lower.length()));
    }

    /**
     * Filters low-confidence issues below a severity threshold.
     */
    public List<Issue> filterBySeverity(List<Issue> issues, int minSeverity) {
        List<Issue> filtered = issues.stream()
                                     .filter(i -> i.getSeverity() >= minSeverity)
                                     .collect(Collectors.toList());
        log.debug("Filtered {} → {} issues (min severity: {})", issues.size(), filtered.size(), minSeverity);
        return filtered;
    }

    /**
     * Groups issues by severity level for UI display.
     */
    public Map<String, List<Issue>> groupBySeverity(List<Issue> issues) {
        Map<String, List<Issue>> grouped = new LinkedHashMap<>();
        grouped.put("CRITICAL", new ArrayList<>());  // 8-10
        grouped.put("HIGH", new ArrayList<>());       // 5-7
        grouped.put("MEDIUM", new ArrayList<>());     // 3-4
        grouped.put("LOW", new ArrayList<>());        // 1-2

        for (Issue issue : issues) {
            int sev = issue.getSeverity();
            if (sev >= 8) {
                grouped.get("CRITICAL").add(issue);
            } else if (sev >= 5) {
                grouped.get("HIGH").add(issue);
            } else if (sev >= 3) {
                grouped.get("MEDIUM").add(issue);
            } else {
                grouped.get("LOW").add(issue);
            }
        }

        return grouped;
    }

    /**
     * Generates a summary report of aggregated results.
     */
    public AggregationReport createReport(List<Issue> issues, List<Suggestion> suggestions,
                                          double score, long analysisTimeMs) {
        Map<String, List<Issue>> bySeverity = groupBySeverity(issues);
        int criticalCount = bySeverity.get("CRITICAL").size();
        int highCount = bySeverity.get("HIGH").size();
        int mediumCount = bySeverity.get("MEDIUM").size();
        int lowCount = bySeverity.get("LOW").size();

        return new AggregationReport(
            issues.size(),
            suggestions.size(),
            score,
            analysisTimeMs,
            criticalCount,
            highCount,
            mediumCount,
            lowCount,
            bySeverity
        );
    }

    // ──── Report type ────────────────────────────────────

    public static class AggregationReport {
        public final int totalIssues;
        public final int totalSuggestions;
        public final double score;
        public final long analysisTimeMs;
        public final int criticalCount;
        public final int highCount;
        public final int mediumCount;
        public final int lowCount;
        public final Map<String, List<Issue>> issuesBySeverity;

        public AggregationReport(int ti, int ts, double s, long t, int crit, int high, int med, int low,
                                  Map<String, List<Issue>> ibs) {
            this.totalIssues = ti;
            this.totalSuggestions = ts;
            this.score = s;
            this.analysisTimeMs = t;
            this.criticalCount = crit;
            this.highCount = high;
            this.mediumCount = med;
            this.lowCount = low;
            this.issuesBySeverity = ibs;
        }

        @Override
        public String toString() {
            return String.format(
                "AggregationReport[score=%.1f, issues=%d (critical:%d high:%d med:%d low:%d), " +
                "suggestions=%d, time=%dms]",
                score, totalIssues, criticalCount, highCount, mediumCount, lowCount,
                totalSuggestions, analysisTimeMs
            );
        }
    }
}
