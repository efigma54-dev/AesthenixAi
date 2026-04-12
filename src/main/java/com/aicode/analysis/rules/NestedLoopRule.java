package com.aicode.analysis.rules;

import com.aicode.analysis.Rule;
import com.aicode.model.Issue;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.stmt.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Detects nested loops (O(n²) or worse) which hurt performance.
 */
public class NestedLoopRule implements Rule {

    @Override
    public List<Issue> check(CompilationUnit cu, String filePath) {
        List<Issue> issues = new ArrayList<>();

        // Check every for-loop for inner loops
        for (ForStmt outer : cu.findAll(ForStmt.class)) {
            if (hasInnerLoop(outer.getBody())) {
                int line = outer.getBegin().map(p -> p.line).orElse(0);
                issues.add(new Issue("Nested Loop",
                    "Nested loop detected — O(n²) or worse. Consider extracting inner logic to a method.",
                    filePath, line, "Performance", getSeverity()));
            }
        }
        for (ForEachStmt outer : cu.findAll(ForEachStmt.class)) {
            if (hasInnerLoop(outer.getBody())) {
                int line = outer.getBegin().map(p -> p.line).orElse(0);
                issues.add(new Issue("Nested Loop",
                    "Nested for-each loop detected. Consider a stream or helper method.",
                    filePath, line, "Performance", getSeverity()));
            }
        }
        for (WhileStmt outer : cu.findAll(WhileStmt.class)) {
            if (hasInnerLoop(outer.getBody())) {
                int line = outer.getBegin().map(p -> p.line).orElse(0);
                issues.add(new Issue("Nested Loop",
                    "Nested while loop detected. Refactor to reduce complexity.",
                    filePath, line, "Performance", getSeverity()));
            }
        }

        return issues;
    }

    private boolean hasInnerLoop(Statement body) {
        return !body.findAll(ForStmt.class).isEmpty()
            || !body.findAll(ForEachStmt.class).isEmpty()
            || !body.findAll(WhileStmt.class).isEmpty()
            || !body.findAll(DoStmt.class).isEmpty();
    }

    @Override public String getName()        { return "NestedLoopRule"; }
    @Override public int    getSeverity()    { return 8; }
    @Override public String getDescription() { return "Nested loops (O(n²) complexity)"; }
}
