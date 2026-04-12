package com.aicode.analysis.rules;

import com.aicode.analysis.Rule;
import com.aicode.model.Issue;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;

import java.util.ArrayList;
import java.util.List;

/**
 * Flags methods longer than 30 lines.
 * Long methods are hard to read, test, and maintain.
 */
public class LongMethodRule implements Rule {

    private static final int MAX_LINES = 30;

    @Override
    public List<Issue> check(CompilationUnit cu, String filePath) {
        List<Issue> issues = new ArrayList<>();
        for (MethodDeclaration m : cu.findAll(MethodDeclaration.class)) {
            if (m.getBegin().isPresent() && m.getEnd().isPresent()) {
                int len = m.getEnd().get().line - m.getBegin().get().line + 1;
                if (len > MAX_LINES) {
                    issues.add(new Issue(
                        "Long Method",
                        String.format("Method '%s' is %d lines. Break it into smaller methods (max %d).",
                            m.getNameAsString(), len, MAX_LINES),
                        filePath, m.getBegin().get().line, "Maintainability", getSeverity()));
                }
            }
        }
        return issues;
    }

    @Override public String getName()        { return "LongMethodRule"; }
    @Override public int    getSeverity()    { return 6; }
    @Override public String getDescription() { return "Methods longer than " + MAX_LINES + " lines"; }
}
