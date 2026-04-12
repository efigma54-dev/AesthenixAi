package com.aicode.analysis;

import com.aicode.model.Issue;
import com.github.javaparser.ast.CompilationUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages and executes pluggable code analysis rules.
 *
 * Rules are registered at startup. New rules can be added without touching
 * existing code — just implement Rule and call registerRule().
 */
@Service
public class RuleEngine {

    private static final Logger log = LoggerFactory.getLogger(RuleEngine.class);

    private final List<Rule> rules = new ArrayList<>();

    public RuleEngine() {
        // Built-in rules — ordered by severity (highest first)
        registerRule(new com.aicode.analysis.rules.NestedLoopRule());
        registerRule(new com.aicode.analysis.rules.LongMethodRule());
        registerRule(new com.aicode.analysis.rules.ExceptionHandlingRule());
        registerRule(new com.aicode.analysis.rules.GodClassRule());
        registerRule(new com.aicode.analysis.rules.NamingConventionRule());
    }

    public void registerRule(Rule rule) {
        rules.add(rule);
        log.debug("Registered rule: {} (severity={})", rule.getName(), rule.getSeverity());
    }

    public void unregisterRule(String name) {
        rules.removeIf(r -> r.getName().equals(name));
    }

    /** Run all rules and collect every issue found. */
    public RuleResult run(CompilationUnit cu, String filePath) {
        List<Issue> issues = new ArrayList<>();
        int totalSeverity = 0;

        for (Rule rule : rules) {
            try {
                List<Issue> found = rule.check(cu, filePath);
                issues.addAll(found);
                if (!found.isEmpty()) totalSeverity += rule.getSeverity();
            } catch (Exception e) {
                log.warn("Rule {} failed on {}: {}", rule.getName(), filePath, e.getMessage());
            }
        }

        log.debug("RuleEngine: {} issues found in {}", issues.size(), filePath);
        return new RuleResult(issues, totalSeverity);
    }

    public List<Rule> getRules() { return new ArrayList<>(rules); }

    public static class RuleResult {
        private final List<Issue> issues;
        private final int totalSeverity;

        public RuleResult(List<Issue> issues, int totalSeverity) {
            this.issues = issues;
            this.totalSeverity = totalSeverity;
        }

        public List<Issue> getIssues()    { return issues; }
        public int getTotalSeverity()     { return totalSeverity; }
        public int getIssueCount()        { return issues.size(); }
    }
}
