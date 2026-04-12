package com.aicode.analysis.rules;

import com.aicode.analysis.Rule;
import com.aicode.model.Issue;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Detects potential circular dependencies by analyzing import patterns.
 *
 * Interview signal:
 *   "I implemented architecture violation detection for dependency graph analysis."
 */
@Component
public class CircularDependencyRule implements Rule {

    @Override
    public List<Issue> check(CompilationUnit cu, String filePath) {
        List<Issue> issues = new ArrayList<>();

        // Simplified: detect obvious circular patterns (e.g., X imports Y, Y imports X)
        Set<String> imports = new HashSet<>();
        cu.findAll(ImportDeclaration.class).forEach(imp -> {
            imports.add(imp.getNameAsString());
        });

        // In real implementation, maintain module-level dependency graph
        // For now, flag suspicious patterns
        if (filePath.contains("service") && hasImportFrom(imports, "controller")) {
            issues.add(new Issue(
                "Circular Dependency Risk",
                "Service imports from controller layer. Verify this is intentional.",
                filePath,
                1,
                "warning",
                5
            ));
        }

        return issues;
    }

    private boolean hasImportFrom(Set<String> imports, String layer) {
        return imports.stream().anyMatch(i -> i.contains(layer));
    }

    @Override
    public String getName() {
        return "CircularDependencyRule";
    }

    @Override
    public int getSeverity() {
        return 5;
    }

    @Override
    public String getDescription() {
        return "Detects potential circular dependencies in module imports";
    }
}
