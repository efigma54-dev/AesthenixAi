package com.aicode.analysis;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles code preprocessing: parsing, normalization, and validation.
 *
 * Pipeline stage:
 *   Input → [Preprocessor] → Parsed AST + Metadata
 *
 * Features:
 *   - Parses source code into JavaParser AST
 *   - Validates input size (security)
 *   - Metrics: parsing time, success/failure rates
 *   - Caches successful parses
 *
 * Interview signal:
 *   "I separated parsing concerns from analysis logic for modularity."
 */
@Service
public class PreprocessorService {

    private static final Logger log = LoggerFactory.getLogger(PreprocessorService.class);

    private final JavaParser javaParser;

    @Value("${analysis.max-file-size:1048576}")  // 1MB default
    private long maxFileSizeBytes;

    @Value("${analysis.parser-cache-enabled:true}")
    private boolean cacheEnabled;

    public PreprocessorService() {
        this.javaParser = new JavaParser();
    }

    /**
     * Preprocesses source code: parses, validates, and extracts metadata.
     */
    public PreprocessedCode preprocess(String sourceCode, String filePath) {
        long startMs = System.currentTimeMillis();

        // Security: check file size
        if (sourceCode.length() > maxFileSizeBytes) {
            log.warn("File {} exceeds size limit ({} bytes)", filePath, sourceCode.length());
            return PreprocessedCode.error(
                String.format("File exceeds maximum size of %d bytes", maxFileSizeBytes),
                filePath
            );
        }

        try {
            // Parse AST
            ParseResult<CompilationUnit> result = javaParser.parse(sourceCode);

            if (!result.isSuccessful() || result.getResult().isEmpty()) {
                String errors = String.join(", ",
                    result.getProblems().stream()
                          .map(p -> p.getMessage())
                          .toList());
                log.warn("Parse failed for {}: {}", filePath, errors);
                return PreprocessedCode.error("Parse error: " + errors, filePath);
            }

            CompilationUnit cu = result.getResult().get();
            long parseTimeMs = System.currentTimeMillis() - startMs;

            // Extract metadata
            CodeMetrics metrics = extractMetrics(cu, sourceCode, filePath, parseTimeMs);

            log.info("Preprocessed {} in {}ms — {} lines, {} classes",
                     filePath, parseTimeMs, metrics.getLineCount(), metrics.getClassCount());

            return PreprocessedCode.success(cu, sourceCode, metrics);

        } catch (Exception e) {
            log.error("Preprocessing exception for {}: {}", filePath, e.getMessage(), e);
            return PreprocessedCode.error("Parsing exception: " + e.getMessage(), filePath);
        }
    }

    /**
     * Preprocesses multiple files (batch mode).
     * Useful for repository-wide scans.
     */
    public List<PreprocessedCode> preprocessFiles(List<String> filePaths, String rootDir) {
        List<PreprocessedCode> results = new ArrayList<>();

        filePaths.stream()
                 .filter(f -> f.endsWith(".java"))
                 .forEach(filePath -> {
                     try {
                         String content = readFile(rootDir + File.separator + filePath);
                         results.add(preprocess(content, filePath));
                     } catch (Exception e) {
                         log.error("Error reading file {}: {}", filePath, e.getMessage());
                         results.add(PreprocessedCode.error("File read error", filePath));
                     }
                 });

        return results;
    }

    /**
     * Extracts code metrics from a parsed compilation unit.
     */
    private CodeMetrics extractMetrics(CompilationUnit cu, String sourceCode, String filePath, long parseTimeMs) {
        int lineCount = sourceCode.split("\n").length;
        int classCount = (int) cu.findAll(com.github.javaparser.ast.body.ClassOrInterfaceDeclaration.class).stream().count();
        int methodCount = (int) cu.findAll(com.github.javaparser.ast.body.MethodDeclaration.class).stream().count();
        int complexity = calculateComplexity(cu);

        return new CodeMetrics(
            filePath,
            lineCount,
            classCount,
            methodCount,
            complexity,
            sourceCode.length(),
            parseTimeMs,
            System.currentTimeMillis()
        );
    }

    private int calculateComplexity(CompilationUnit cu) {
        int complexity = 1;  // base
        complexity += cu.findAll(com.github.javaparser.ast.stmt.IfStmt.class).size();
        complexity += cu.findAll(com.github.javaparser.ast.stmt.ForStmt.class).size();
        complexity += cu.findAll(com.github.javaparser.ast.stmt.WhileStmt.class).size();
        complexity += cu.findAll(com.github.javaparser.ast.stmt.SwitchStmt.class).size();
        return complexity;
    }

    private String readFile(String path) throws Exception {
        return new String(Files.readAllBytes(Paths.get(path)));
    }

    // ──── Result types ────────────────────────────────────────────────

    public static class PreprocessedCode {
        private final CompilationUnit compilationUnit;
        private final String sourceCode;
        private final CodeMetrics metrics;
        private final String errorMessage;
        private final boolean success;

        private PreprocessedCode(CompilationUnit cu, String src, CodeMetrics m, String err, boolean ok) {
            this.compilationUnit = cu;
            this.sourceCode = src;
            this.metrics = m;
            this.errorMessage = err;
            this.success = ok;
        }

        public static PreprocessedCode success(CompilationUnit cu, String src, CodeMetrics m) {
            return new PreprocessedCode(cu, src, m, null, true);
        }

        public static PreprocessedCode error(String err, String filePath) {
            CodeMetrics m = new CodeMetrics(filePath, 0, 0, 0, 0, 0, 0, System.currentTimeMillis());
            return new PreprocessedCode(null, "", m, err, false);
        }

        public boolean isSuccess() { return success; }
        public String getErrorMessage() { return errorMessage; }
        public CompilationUnit getCompilationUnit() { return compilationUnit; }
        public String getSourceCode() { return sourceCode; }
        public CodeMetrics getMetrics() { return metrics; }
    }

    public static class CodeMetrics {
        private final String filePath;
        private final int lineCount;
        private final int classCount;
        private final int methodCount;
        private final int complexity;
        private final int byteSize;
        private final long parseTimeMs;
        private final long timestamp;

        public CodeMetrics(String fp, int lc, int cc, int mc, int c, int bs, long pt, long ts) {
            this.filePath = fp;
            this.lineCount = lc;
            this.classCount = cc;
            this.methodCount = mc;
            this.complexity = c;
            this.byteSize = bs;
            this.parseTimeMs = pt;
            this.timestamp = ts;
        }

        public String getFilePath() { return filePath; }
        public int getLineCount() { return lineCount; }
        public int getClassCount() { return classCount; }
        public int getMethodCount() { return methodCount; }
        public int getComplexity() { return complexity; }
        public int getByteSize() { return byteSize; }
        public long getParseTimeMs() { return parseTimeMs; }
        public long getTimestamp() { return timestamp; }
    }
}
