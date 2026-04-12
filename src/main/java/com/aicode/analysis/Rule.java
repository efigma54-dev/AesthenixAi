package com.aicode.analysis;

import com.aicode.model.Issue;
import com.github.javaparser.ast.CompilationUnit;

import java.util.List;

/**
 * Interface for pluggable code analysis rules.
 *
 * Each rule is self-contained and independently testable.
 * Register new rules via RuleEngine.registerRule().
 */
public interface Rule {

    /**
     * Analyse the compilation unit and return all issues found.
     * Return an empty list (never null) when no issues are detected.
     */
    List<Issue> check(CompilationUnit cu, String filePath);

    /** Unique rule identifier — used for enable/disable by name. */
    String getName();

    /** Severity 1–10 (10 = most severe). Used by ScoringEngine. */
    int getSeverity();

    /** Human-readable description of what this rule checks. */
    default String getDescription() { return getName(); }
}