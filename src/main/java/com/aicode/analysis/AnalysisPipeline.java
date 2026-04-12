package com.aicode.analysis;

import com.aicode.model.Issue;
import com.aicode.model.Suggestion;
import com.aicode.service.LocalAIService;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Main analysis pipeline.
 *
 * Stages:
 *   1. Preprocess  — JavaParser parses the code
 *   2. Rule Engine — pluggable static analysis rules
 *   3. AI Engine   — local Ollama model for suggestions + improved code
 *   4. Post-process — merge, deduplicate, sort
 *   5. Score       — configurable weighted scoring
 */
@Service
public class AnalysisPipeline {

    private static final Logger log = LoggerFactory.getLogger(AnalysisPipeline.class);

    private final JavaParser    javaParser;
    private final RuleEngine    ruleEngine;
    private final LocalAIService aiService;
    private final PostProcessor  postProcessor;
    private final ScoringEngine  scoringEngine;

    public AnalysisPipeline(RuleEngine ruleEngine, LocalAIService aiService,
                            PostProcessor postProcessor, ScoringEngine scoringEngine) {
        this.javaParser   = new JavaParser();
        this.ruleEngine   = ruleEngine;
        this.aiService    = aiService;
        this.postProcessor = postProcessor;
        this.scoringEngine = scoringEngine;
    }

    public AnalysisResult analyze(String code, String filePath) {
        long start = System.currentTimeMillis();
        try {
            // Phase 1: parse
            ParsedCode parsed = preprocess(code, filePath);
            if (parsed == null) return errorResult("Failed to parse code", filePath, 0);

            // Phase 2: rules
            RuleEngine.RuleResult ruleResult = ruleEngine.run(parsed.getCompilationUnit(), filePath);

            // Phase 3: AI
            com.aicode.model.AIResult aiResult = aiService.analyzeCode(code);

            // Phase 4: merge
            MergedResult merged = postProcessor.merge(ruleResult, aiResult, parsed);

            // Phase 5: score
            double score = scoringEngine.calculateScore(merged, parsed);

            long ms = System.currentTimeMillis() - start;
            log.info("Pipeline complete in {}ms — score={:.1f}, issues={}", ms, score, merged.getIssues().size());

            return new AnalysisResult(score, merged.getIssues(), merged.getSuggestions(),
                    aiResult.getImprovedCode(), ms, "success");

        } catch (Exception e) {
            long ms = System.currentTimeMillis() - start;
            log.error("Pipeline failed: {}", e.getMessage());
            return errorResult("Analysis failed: " + e.getMessage(), filePath, ms);
        }
    }

    private ParsedCode preprocess(String code, String filePath) {
        try {
            ParseResult<CompilationUnit> r = javaParser.parse(code);
            if (r.isSuccessful() && r.getResult().isPresent())
                return new ParsedCode(r.getResult().get(), code, filePath);
            r.getProblems().forEach(p -> log.warn("Parse problem: {}", p.getMessage()));
            return null;
        } catch (Exception e) {
            log.warn("Parse error: {}", e.getMessage());
            return null;
        }
    }

    private AnalysisResult errorResult(String msg, String filePath, long ms) {
        List<Issue> issues = List.of(new Issue("Analysis Error", msg, filePath, 0, "error", 10));
        return new AnalysisResult(0.0, issues, new ArrayList<>(), "", ms, "error");
    }

    // ── Inner types ────────────────────────────────────────────

    public static class AnalysisResult {
        private final double score;
        private final List<Issue> issues;
        private final List<Suggestion> suggestions;
        private final String improvedCode;
        private final long processingTimeMs;
        private final String status;

        public AnalysisResult(double score, List<Issue> issues, List<Suggestion> suggestions,
                              String improvedCode, long processingTimeMs, String status) {
            this.score           = score;
            this.issues          = issues;
            this.suggestions     = suggestions;
            this.improvedCode    = improvedCode != null ? improvedCode : "";
            this.processingTimeMs = processingTimeMs;
            this.status          = status;
        }

        public double getScore()            { return score; }
        public List<Issue> getIssues()      { return issues; }
        public List<Suggestion> getSuggestions() { return suggestions; }
        public String getImprovedCode()     { return improvedCode; }
        public long getProcessingTimeMs()   { return processingTimeMs; }
        public String getStatus()           { return status; }
    }

    public static class ParsedCode {
        private final CompilationUnit compilationUnit;
        private final String originalCode;
        private final String filePath;

        public ParsedCode(CompilationUnit cu, String code, String path) {
            this.compilationUnit = cu;
            this.originalCode    = code;
            this.filePath        = path;
        }

        public CompilationUnit getCompilationUnit() { return compilationUnit; }
        public String getOriginalCode()             { return originalCode; }
        public String getFilePath()                 { return filePath; }
    }

    public static class MergedResult {
        private final List<Issue> issues;
        private final List<Suggestion> suggestions;

        public MergedResult(List<Issue> issues, List<Suggestion> suggestions) {
            this.issues      = issues;
            this.suggestions = suggestions;
        }

        public List<Issue> getIssues()           { return issues; }
        public List<Suggestion> getSuggestions() { return suggestions; }
    }
}
