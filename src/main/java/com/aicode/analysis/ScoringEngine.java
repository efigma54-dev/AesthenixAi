package com.aicode.analysis;

import com.aicode.config.ScoringConfig;
import com.aicode.model.Issue;
import org.springframework.stereotype.Service;

/**
 * Configurable scoring engine.
 * Combines rule-based and AI-based scores using weights from ScoringConfig.
 */
@Service
public class ScoringEngine {

    private final ScoringConfig config;

    public ScoringEngine(ScoringConfig config) {
        this.config = config;
    }

    public double calculateScore(AnalysisPipeline.MergedResult merged,
                                 AnalysisPipeline.ParsedCode parsed) {
        double ruleScore = calculateRuleScore(merged);
        double aiScore   = calculateAIScore(merged);
        double final_    = (ruleScore * config.getRuleWeight()) + (aiScore * config.getAiWeight());
        return Math.max(0, Math.min(100, final_));
    }

    private double calculateRuleScore(AnalysisPipeline.MergedResult merged) {
        int penalty = 0;
        for (Issue issue : merged.getIssues()) {
            if (isRuleBased(issue)) penalty += penaltyFor(issue);
        }
        return config.getBaseScore() - Math.min(penalty, config.getMaxPenalties());
    }

    private double calculateAIScore(AnalysisPipeline.MergedResult merged) {
        long aiCount = merged.getIssues().stream().filter(i -> !isRuleBased(i)).count();
        double deduction = Math.min(aiCount * config.getAiIssuePenalty(), config.getMaxPenalties());
        return config.getBaseScore() - deduction;
    }

    private int penaltyFor(Issue issue) {
        String title = issue.getTitle();
        if (title == null) return 5;
        return switch (title) {
            case "Long Method"                  -> config.getLongMethodPenalty();
            case "Deeply Nested Loops"          -> config.getNestedLoopPenalty();
            case "Naming Convention Violation"  -> config.getNamingConventionPenalty();
            default                             -> 5;
        };
    }

    private boolean isRuleBased(Issue issue) {
        String t = issue.getTitle();
        return "Long Method".equals(t)
            || "Deeply Nested Loops".equals(t)
            || "Naming Convention Violation".equals(t);
    }

    public ScoringConfig getConfig() { return config; }
}
