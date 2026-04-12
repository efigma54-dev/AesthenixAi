package com.aicode.analysis;

import com.aicode.model.Issue;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Multi-language abstraction for code analysis.
 *
 * Design Pattern: Strategy + Factory
 *   Enables adding Python, JavaScript, Go analyzers without core changes.
 *
 * Interview signal:
 *   "I designed a pluggable language analyzer system,
 *    demonstrating extensibility principles used in production tools."
 *
 * Future implementations:
 *   - PythonAnalyzer (using AST or Pylint)
 *   - JavaScriptAnalyzer (using ESLint or Babel)
 *   - GoAnalyzer (using go/analysis)
 */
public interface LanguageAnalyzer {

    /**
     * Analyzes source code and returns issues found.
     */
    List<Issue> analyze(String sourceCode, String filePath);

    /**
     * Gets the language this analyzer supports.
     */
    Language getLanguage();

    /**
     * Gets descriptive name of the analyzer.
     */
    String getName();

    /**
     * Checks if the analyzer is available/enabled.
     */
    default boolean isAvailable() {
        return true;
    }

    enum Language {
        JAVA("java", ".java"),
        PYTHON("python", ".py"),
        JAVASCRIPT("javascript", ".js"),
        TYPESCRIPT("typescript", ".ts"),
        GO("go", ".go");

        public final String name;
        public final String fileExtension;

        Language(String name, String ext) {
            this.name = name;
            this.fileExtension = ext;
        }

        public static Language fromFilePath(String filePath) {
            for (Language lang : Language.values()) {
                if (filePath.endsWith(lang.fileExtension)) {
                    return lang;
                }
            }
            return null;
        }
    }
}

/**
 * Concrete Java analyzer (current implementation).
 */
@Service
class JavaLanguageAnalyzer implements LanguageAnalyzer {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(JavaLanguageAnalyzer.class);
    private final RuleEngine ruleEngine;
    private final PreprocessorService preprocessor;

    public JavaLanguageAnalyzer(RuleEngine ruleEngine, PreprocessorService preprocessor) {
        this.ruleEngine = ruleEngine;
        this.preprocessor = preprocessor;
    }

    @Override
    public List<Issue> analyze(String sourceCode, String filePath) {
        log.debug("Analyzing Java file: {}", filePath);
        
        // Parse
        PreprocessorService.PreprocessedCode parsed = preprocessor.preprocess(sourceCode, filePath);
        if (!parsed.isSuccess()) {
            log.warn("Parse failed for {}: {}", filePath, parsed.getErrorMessage());
            return List.of();
        }

        // Run rules
        RuleEngine.RuleResult result = ruleEngine.run(parsed.getCompilationUnit(), filePath);
        return result.getIssues();
    }

    @Override
    public Language getLanguage() {
        return Language.JAVA;
    }

    @Override
    public String getName() {
        return "Java Analyzer (JavaParser)";
    }
}

/**
 * Factory for creating language-specific analyzers.
 */
@Service
class LanguageAnalyzerFactory {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(LanguageAnalyzerFactory.class);
    private final JavaLanguageAnalyzer javaAnalyzer;
    // TODO: PythonLanguageAnalyzer pythonAnalyzer;
    // TODO: JavaScriptLanguageAnalyzer jsAnalyzer;

    public LanguageAnalyzerFactory(JavaLanguageAnalyzer javaAnalyzer) {
        this.javaAnalyzer = javaAnalyzer;
    }

    /**
     * Creates an analyzer for the given file path.
     */
    public LanguageAnalyzer getAnalyzer(String filePath) {
        LanguageAnalyzer.Language lang = LanguageAnalyzer.Language.fromFilePath(filePath);

        switch (lang) {
            case JAVA:
                return javaAnalyzer;
            case PYTHON:
                log.warn("Python analyzer not yet implemented");
                return null;
            case JAVASCRIPT:
            case TYPESCRIPT:
                log.warn("JavaScript/TypeScript analyzer not yet implemented");
                return null;
            case GO:
                log.warn("Go analyzer not yet implemented");
                return null;
            default:
                log.warn("Unknown language for file: {}", filePath);
                return null;
        }
    }

    /**
     * Gets an analyzer by explicit language.
     */
    public LanguageAnalyzer getAnalyzerByLanguage(LanguageAnalyzer.Language lang) {
        return getAnalyzer("dummy" + lang.fileExtension);
    }

    /**
     * Lists all supported languages.
     */
    public LanguageAnalyzer.Language[] getSupportedLanguages() {
        return new LanguageAnalyzer.Language[]{
            LanguageAnalyzer.Language.JAVA
            // Add others as implemented
        };
    }
}
