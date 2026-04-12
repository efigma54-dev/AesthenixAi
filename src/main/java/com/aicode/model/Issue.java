package com.aicode.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a single code quality issue found during analysis.
 * Compatible with both the rule engine (6-arg constructor) and
 * the legacy API response format (3-arg builder).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Issue {

    // ── Core fields (used by API response) ────────────────────
    private int    line;
    private String type;     // Bug | Performance | Security | Style | Maintainability
    private String message;

    // ── Extended fields (used by rule engine) ─────────────────
    @Builder.Default private String filePath  = "";
    @Builder.Default private String category  = "";
    @Builder.Default private int    severity  = 5;   // 1–10

    /**
     * 6-arg constructor used by rule engine and LocalAIService.
     * Maps: title→type, description→message
     */
    public Issue(String title, String description, String filePath,
                 int line, String category, int severity) {
        this.type     = title;
        this.message  = description;
        this.filePath = filePath;
        this.line     = line;
        this.category = category;
        this.severity = severity;
    }

    /** Convenience accessor — rules store the issue title in `type` */
    public String getTitle() { return type; }

    /** Convenience accessor */
    public String getDescription() { return message; }
}
