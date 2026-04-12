package com.aicode.analysis.rules;

import com.aicode.analysis.Rule;
import com.aicode.model.Issue;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.*;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Checks Java naming conventions:
 * - Classes: PascalCase
 * - Methods/variables: camelCase
 * - Constants (static final): UPPER_SNAKE_CASE
 */
public class NamingConventionRule implements Rule {

    private static final Pattern PASCAL  = Pattern.compile("^[A-Z][a-zA-Z0-9]*$");
    private static final Pattern CAMEL   = Pattern.compile("^[a-z][a-zA-Z0-9]*$");
    private static final Pattern UPPER   = Pattern.compile("^[A-Z][A-Z0-9_]*$");

    @Override
    public List<Issue> check(CompilationUnit cu, String filePath) {
        List<Issue> issues = new ArrayList<>();

        for (ClassOrInterfaceDeclaration c : cu.findAll(ClassOrInterfaceDeclaration.class)) {
            if (!PASCAL.matcher(c.getNameAsString()).matches())
                issues.add(new Issue("Naming Convention",
                    "Class '" + c.getNameAsString() + "' should use PascalCase.",
                    filePath, c.getBegin().map(p -> p.line).orElse(0), "Style", getSeverity()));
        }

        for (MethodDeclaration m : cu.findAll(MethodDeclaration.class)) {
            if (!CAMEL.matcher(m.getNameAsString()).matches())
                issues.add(new Issue("Naming Convention",
                    "Method '" + m.getNameAsString() + "' should use camelCase.",
                    filePath, m.getBegin().map(p -> p.line).orElse(0), "Style", getSeverity()));
        }

        for (FieldDeclaration f : cu.findAll(FieldDeclaration.class)) {
            boolean isConst = f.isStatic() && f.isFinal();
            for (VariableDeclarator v : f.getVariables()) {
                String name = v.getNameAsString();
                if (isConst && !UPPER.matcher(name).matches())
                    issues.add(new Issue("Naming Convention",
                        "Constant '" + name + "' should use UPPER_SNAKE_CASE.",
                        filePath, f.getBegin().map(p -> p.line).orElse(0), "Style", getSeverity()));
                else if (!isConst && !CAMEL.matcher(name).matches())
                    issues.add(new Issue("Naming Convention",
                        "Field '" + name + "' should use camelCase.",
                        filePath, f.getBegin().map(p -> p.line).orElse(0), "Style", getSeverity()));
            }
        }

        return issues;
    }

    @Override public String getName()        { return "NamingConventionRule"; }
    @Override public int    getSeverity()    { return 4; }
    @Override public String getDescription() { return "Java naming conventions (PascalCase, camelCase, UPPER_SNAKE_CASE)"; }
}
