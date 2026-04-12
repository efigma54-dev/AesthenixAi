package com.aicode.analysis.rules;

import com.aicode.analysis.Rule;
import com.aicode.model.Issue;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.TryStmt;

import java.util.ArrayList;
import java.util.List;

/**
 * Flags methods that have no exception handling at all.
 * Also detects empty catch blocks (swallowed exceptions).
 */
public class ExceptionHandlingRule implements Rule {

    @Override
    public List<Issue> check(CompilationUnit cu, String filePath) {
        List<Issue> issues = new ArrayList<>();

        // Empty catch blocks — silently swallowing exceptions
        for (TryStmt t : cu.findAll(TryStmt.class)) {
            t.getCatchClauses().forEach(c -> {
                if (c.getBody().getStatements().isEmpty()) {
                    int line = c.getBegin().map(p -> p.line).orElse(0);
                    issues.add(new Issue("Empty Catch Block",
                        "Empty catch block silently swallows the exception. Log or rethrow it.",
                        filePath, line, "Bug", getSeverity()));
                }
            });
        }

        // Methods with no try/catch at all (only flag non-trivial methods)
        for (MethodDeclaration m : cu.findAll(MethodDeclaration.class)) {
            if (m.getBody().isEmpty()) continue;
            int lines = m.getEnd().map(e -> e.line).orElse(0)
                      - m.getBegin().map(b -> b.line).orElse(0);
            if (lines > 5 && m.findAll(TryStmt.class).isEmpty()) {
                issues.add(new Issue("No Exception Handling",
                    "Method '" + m.getNameAsString() + "' has no exception handling. Add try/catch for risky operations.",
                    filePath, m.getBegin().map(p -> p.line).orElse(0), "Security", getSeverity()));
            }
        }

        return issues;
    }

    @Override public String getName()        { return "ExceptionHandlingRule"; }
    @Override public int    getSeverity()    { return 7; }
    @Override public String getDescription() { return "Missing or empty exception handling"; }
}
