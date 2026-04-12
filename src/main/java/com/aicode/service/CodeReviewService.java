package com.aicode.service;

import com.aicode.analysis.AnalysisPipeline;
import com.aicode.model.ReviewResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Orchestrates the full analysis pipeline and returns a ReviewResponse.
 * Results are cached by code hash (Caffeine, 30-min TTL).
 */
@Service
public class CodeReviewService {

    private static final Logger log = LoggerFactory.getLogger(CodeReviewService.class);

    private final AnalysisPipeline analysisPipeline;

    public CodeReviewService(AnalysisPipeline analysisPipeline) {
        this.analysisPipeline = analysisPipeline;
    }

    @Cacheable(value = "reviews", key = "#code.hashCode()")
    public ReviewResponse reviewCode(String code) {
        log.info("Reviewing code ({} chars) via analysis pipeline", code.length());
        long start = System.currentTimeMillis();

        AnalysisPipeline.AnalysisResult result = analysisPipeline.analyze(code, "unknown");

        List<String> suggestions = result.getSuggestions().stream()
                .map(s -> s.getMessage())
                .collect(Collectors.toList());

        log.info("Review complete in {}ms — score={}, issues={}",
                System.currentTimeMillis() - start,
                (int) Math.round(result.getScore()),
                result.getIssues().size());

        return ReviewResponse.builder()
                .score((int) Math.round(result.getScore()))
                .issues(result.getIssues())
                .suggestions(suggestions)
                .improvedCode(result.getImprovedCode())
                .parsedInfo(null)
                .build();
    }
}
