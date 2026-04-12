package com.aicode.model;

/**
 * Issue severity levels — used by the PR bot, rule engine, and annotations.
 *
 * Maps to GitHub annotation levels:
 *   CRITICAL / HIGH  → "failure"
 *   WARNING / MEDIUM → "warning"
 *   INFO / LOW       → "notice"
 */
public enum Severity {

    CRITICAL(5, "🚨", "CRITICAL", "failure"),
    HIGH    (4, "🔴", "HIGH",     "failure"),
    WARNING (3, "⚠️",  "WARNING",  "warning"),
    MEDIUM  (2, "🟡", "MEDIUM",   "warning"),
    LOW     (1, "🔵", "LOW",      "notice"),
    INFO    (0, "ℹ️",  "INFO",     "notice");

    private final int    level;
    private final String emoji;
    private final String label;
    private final String annotationLevel;   // GitHub Checks API annotation_level

    Severity(int level, String emoji, String label, String annotationLevel) {
        this.level           = level;
        this.emoji           = emoji;
        this.label           = label;
        this.annotationLevel = annotationLevel;
    }

    public int    getLevel()           { return level; }
    public String getEmoji()           { return emoji; }
    public String getLabel()           { return label; }
    public String getAnnotationLevel() { return annotationLevel; }

    /** Map a numeric severity score (1–10) to a Severity enum. */
    public static Severity fromScore(int score) {
        if (score >= 9) return CRITICAL;
        if (score >= 7) return HIGH;
        if (score >= 5) return WARNING;
        if (score >= 3) return MEDIUM;
        if (score >= 1) return LOW;
        return INFO;
    }

    /** Map an issue type string to a Severity. */
    public static Severity fromIssueType(String type) {
        if (type == null) return INFO;
        return switch (type.toLowerCase()) {
            case "bug", "security"                          -> CRITICAL;
            case "performance", "nested loop", "god class" -> HIGH;
            case "long method", "no exception handling",
                 "empty catch block"                        -> WARNING;
            case "maintainability"                          -> MEDIUM;
            case "style", "naming convention"               -> LOW;
            default                                         -> INFO;
        };
    }
}
