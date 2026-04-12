package com.aicode.service;

import com.aicode.model.ParsedCodeInfo;
import org.springframework.stereotype.Service;

@Service
public class ScoringService {

    public int calculateScore(ParsedCodeInfo info) {
        int score = 100;

        // Nested loops are a performance red flag
        score -= info.getNestedLoopCount() * 10;

        // High cyclomatic complexity
        if (info.getCyclomaticComplexity() > 15) {
            score -= 20;
        } else if (info.getCyclomaticComplexity() > 10) {
            score -= 15;
        } else if (info.getCyclomaticComplexity() > 5) {
            score -= 5;
        }

        // Long methods (methods > 30 lines)
        score -= info.getLongMethodCount() * 10;

        // No exception handling at all
        if (!info.isHasExceptionHandling() && info.getMethodCount() > 0) {
            score -= 20;
        }

        // Too many loops relative to methods (possible bad design)
        if (info.getMethodCount() > 0 && info.getLoopCount() > info.getMethodCount() * 3) {
            score -= 10;
        }

        // Bad naming (simple heuristic: methods with numbers or underscores)
        long badNames = info.getMethodNames().stream()
                .filter(name -> name.contains("_") || name.matches(".*\\d.*"))
                .count();
        score -= (int) badNames * 5;

        return Math.max(0, Math.min(100, score));
    }
}
