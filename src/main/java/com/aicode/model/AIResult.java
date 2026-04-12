package com.aicode.model;

import java.util.List;

/**
 * Result from the local AI (Ollama) analysis.
 */
public class AIResult {

    private final List<Issue>      issues;
    private final List<Suggestion> suggestions;
    private final double           aiScore;
    private final String           improvedCode;

    public AIResult(List<Issue> issues, List<Suggestion> suggestions,
                    double aiScore, String improvedCode) {
        this.issues       = issues;
        this.suggestions  = suggestions;
        this.aiScore      = aiScore;
        this.improvedCode = improvedCode != null ? improvedCode : "";
    }

    /** Legacy 3-arg constructor — no improved code */
    public AIResult(List<Issue> issues, List<Suggestion> suggestions, double aiScore) {
        this(issues, suggestions, aiScore, "");
    }

    public List<Issue>      getIssues()       { return issues; }
    public List<Suggestion> getSuggestions()  { return suggestions; }
    public double           getAiScore()      { return aiScore; }
    public String           getImprovedCode() { return improvedCode; }
}
