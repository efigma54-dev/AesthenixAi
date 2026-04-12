package com.aicode.analysis.rules;

import com.aicode.analysis.Rule;
import com.aicode.model.Issue;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;

import java.util.ArrayList;
import java.util.List;

/**
 * Detects "God Classes" — classes that do too much.
 * Thresholds: > 10 methods OR > 15 fields OR > 300 lines.
 */
public class GodClassRule implements Rule {

    private static final int MAX_METHODS = 10;
    private static final int MAX_FIELDS  = 15;
    private static final int MAX_LINES   = 300;

    @Override
    public List<Issue> check(CompilationUnit cu, String filePath) {
        List<Issue> issues = new ArrayList<>();

        for (ClassOrInterfaceDeclaration c : cu.findAll(ClassOrInterfaceDeclaration.class)) {
            int methods = c.findAll(MethodDeclaration.class).size();
            int fields  = c.findAll(FieldDeclaration.class).size();
            int lines   = c.getEnd().map(e -> e.line).orElse(0)
                        - c.getBegin().map(b -> b.line).orElse(0);

            List<String> reasons = new ArrayList<>();
            if (methods > MAX_METHODS) reasons.add(methods + " methods (max " + MAX_METHODS + ")");
            if (fields  > MAX_FIELDS)  reasons.add(fields  + " fields (max "  + MAX_FIELDS  + ")");
            if (lines   > MAX_LINES)   reasons.add(lines   + " lines (max "   + MAX_LINES   + ")");

            if (!reasons.isEmpty()) {
                issues.add(new Issue("God Class",
                    "Class '" + c.getNameAsString() + "' is too large: " + String.join(", ", reasons)
                    + ". Split into smaller, focused classes.",
                    filePath, c.getBegin().map(p -> p.line).orElse(0), "Maintainability", getSeverity()));
            }
        }

        return issues;
    }

    @Override public String getName()        { return "GodClassRule"; }
    @Override public int    getSeverity()    { return 7; }
    @Override public String getDescription() { return "Classes with too many responsibilities"; }
}
