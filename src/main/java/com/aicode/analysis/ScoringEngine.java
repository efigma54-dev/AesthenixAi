package com.aicode.analysis;

import com.aicode.config.ScoringConfig;
import com.aicode.model.Issue;
import com.aicode.model.Severity;
import org.springframework.stereotype.Service;

/**
 * Configurable scoring engine.
 *
 * Severity-weighted penalties:
 *   CRITICAL → -10 each
 *   HIGH     → -7  each
 *   MEDIUM   → -5  each
 *   LOW      → -2  each
 *   INFO     → -1  each
 *
 * Combines rule-based and AI-based scores using weights from ScoringConfig.
 */
@Service
public class ScoringEngine {

    // Severity penalty map (points deducted per issue)
    private static final int PENALTY_CRITICAL = 10;
    private static final int PENALTY_HIGH     = 7;
    private static final int PENALTY_MEDIUM   = 5;
    private static final int PENALTY_LOW      = 2;
    private static final int PENALTY_INFO     = 1;

    private final ScoringConfig config;

    public ScoringEngine(ScoringConfig config) {
        this.config = config;
    }

    public double calculateScore(AnalysisPipeline.MergedResult merged,
                                 AnalysisPipeline.ParsedCode parsed) {
        double ruleScore = calculateRuleScore(merged);
        double aiScore   = calculateAIScore(merged);
        double combined  = (ruleScore * config.getRuleWeight()) + (aiScore * config.getAiWeight());
        return Math.max(0, Math.min(100, combined));
    }

    /**
     * Rule-based score: deduct severity-weighted penalties for each issue.
     * Caps total deduction at maxPenalties to avoid unfair 0 scores.
     */
    private double calculateRuleScore(AnalysisPipeline.MergedResult merged) {
        int penalty = 0;
        for (Issue issue : merged.getIssues()) {
            if (isRuleBased(issue)) {
                penalty += severityPenaltyFor(issue);
            }
        }
        return config.getBaseScore() - Math.min(penalty, config.getMaxPenalties());
    }

    /**
     * AI-based score: deduct severity-weighted penalties for AI-detected issues.
     */
    private double calculateAIScore(AnalysisPipeline.MergedResult merged) {
        int penalty = 0;
        for (Issue issue : merged.getIssues()) {
            if (!isRuleBased(issue)) {
                penalty += severityPenaltyFor(issue);
            }
        }
        return config.getBaseScore() - Math.min(penalty, config.getMaxPenalties());
    }

    /**
     * Returns severity-weighted penalty for any issue.
     * Falls back to rule-specific config values for known rule types,
     * otherwise uses severity level.
     */
    private int severityPenaltyFor(Issue issue) {
        // Named rule overrides (from config) take priority
        String title = issue.getTitle();
        if (title != null) {
            switch (title) {
                case "Long Method":                 return config.getLongMethodPenalty();
                case "Deeply Nested Loops":         return config.getNestedLoopPenalty();
                case "Naming Convention Violation": return config.getNamingConventionPenalty();
                case "God Class":                   return config.getGodClassPenalty();
                case "Circular Dependency":         return config.getCircularDependencyPenalty();
                case "Exception Handling":          return config.getExceptionHandlingPenalty();
            }
        }
        // Fall back to severity-based penalty
        return severityToPenalty(Severity.fromIssueType(issue.getType()));
    }

    /** Maps a Severity level to a flat penalty value. */
    public static int severityToPenalty(Severity sev) {
        return switch (sev) {
            case CRITICAL -> PENALTY_CRITICAL;
            case HIGH     -> PENALTY_HIGH;
            case MEDIUM   -> PENALTY_MEDIUM;
            case LOW      -> PENALTY_LOW;
            default       -> PENALTY_INFO;
        };
    }

    private boolean isRuleBased(Issue issue) {
        String t = issue.getTitle();
        return "Long Method".equals(t)
            || "Deeply Nested Loops".equals(t)
            || "Naming Convention Violation".equals(t)
            || "God Class".equals(t)
            || "Circular Dependency".equals(t)
            || "Exception Handling".equals(t);
    }

    public ScoringConfig getConfig() { return config; }
}
