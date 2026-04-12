package com.aicode.service;

import com.aicode.model.ParsedCodeInfo;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class CodeAnalysisService {

    private final JavaParser javaParser = new JavaParser();

    public ParsedCodeInfo analyze(String code) {
        ParseResult<CompilationUnit> result = javaParser.parse(code);

        if (result.getResult().isEmpty()) {
            // Return minimal info if parsing fails (e.g. snippet without class wrapper)
            return ParsedCodeInfo.builder()
                    .methodCount(0)
                    .loopCount(0)
                    .nestedLoopCount(0)
                    .cyclomaticComplexity(1)
                    .methodNames(List.of())
                    .hasExceptionHandling(false)
                    .longMethodCount(0)
                    .build();
        }

        CompilationUnit cu = result.getResult().get();

        List<MethodDeclaration> methods = cu.findAll(MethodDeclaration.class);
        List<String> methodNames = methods.stream()
                .map(m -> m.getNameAsString())
                .toList();

        int loopCount = countLoops(cu);
        int nestedLoopCount = countNestedLoops(cu);
        int complexity = computeCyclomaticComplexity(cu);
        boolean hasExceptionHandling = !cu.findAll(TryStmt.class).isEmpty();
        int longMethodCount = (int) methods.stream()
                .filter(m -> m.getEnd().isPresent() && m.getBegin().isPresent()
                        && (m.getEnd().get().line - m.getBegin().get().line) > 30)
                .count();

        return ParsedCodeInfo.builder()
                .methodCount(methods.size())
                .loopCount(loopCount)
                .nestedLoopCount(nestedLoopCount)
                .cyclomaticComplexity(complexity)
                .methodNames(methodNames)
                .hasExceptionHandling(hasExceptionHandling)
                .longMethodCount(longMethodCount)
                .build();
    }

    private int countLoops(CompilationUnit cu) {
        return cu.findAll(ForStmt.class).size()
                + cu.findAll(ForEachStmt.class).size()
                + cu.findAll(WhileStmt.class).size()
                + cu.findAll(DoStmt.class).size();
    }

    private int countNestedLoops(CompilationUnit cu) {
        AtomicInteger count = new AtomicInteger(0);

        cu.findAll(ForStmt.class).forEach(outer ->
                countInnerLoops(outer.getBody(), count));
        cu.findAll(ForEachStmt.class).forEach(outer ->
                countInnerLoops(outer.getBody(), count));
        cu.findAll(WhileStmt.class).forEach(outer ->
                countInnerLoops(outer.getBody(), count));

        return count.get();
    }

    private void countInnerLoops(Statement body, AtomicInteger count) {
        if (!body.findAll(ForStmt.class).isEmpty()
                || !body.findAll(ForEachStmt.class).isEmpty()
                || !body.findAll(WhileStmt.class).isEmpty()
                || !body.findAll(DoStmt.class).isEmpty()) {
            count.incrementAndGet();
        }
    }

    /**
     * Basic cyclomatic complexity: 1 + number of decision points
     * Decision points: if, else-if, for, while, do, case, catch, &&, ||
     */
    private int computeCyclomaticComplexity(CompilationUnit cu) {
        int complexity = 1;
        complexity += cu.findAll(IfStmt.class).size();
        complexity += cu.findAll(ForStmt.class).size();
        complexity += cu.findAll(ForEachStmt.class).size();
        complexity += cu.findAll(WhileStmt.class).size();
        complexity += cu.findAll(DoStmt.class).size();
        complexity += cu.findAll(SwitchEntry.class).size();
        complexity += cu.findAll(CatchClause.class).size();
        return complexity;
    }
}
