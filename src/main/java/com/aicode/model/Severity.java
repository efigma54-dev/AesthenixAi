package com.aicode.model;

public enum Severity {
    CRITICAL("🔴", "Critical", 4),
    HIGH("🟠", "High", 3),
    MEDIUM("🟡", "Medium", 2),
    LOW("🔵", "Low", 1),
    INFO("⚪", "Info", 0);

    private final String emoji;
    private final String label;
    private final int level;

    Severity(String emoji, String label, int level) {
        this.emoji = emoji;
        this.label = label;
        this.level = level;
    }

    public String getEmoji() {
        return emoji;
    }

    public String getLabel() {
        return label;
    }

    public int getLevel() {
        return level;
    }

    /** Map an issue type string to a Severity. */
    public static Severity fromIssueType(String type) {
        if (type == null)
            return INFO;
        return switch (type.toLowerCase()) {
            case "bug", "security" -> CRITICAL;
            case "performance", "nested loop", "god class" -> HIGH;
            case "long method", "no exception handling",
                    "empty catch block" ->
                MEDIUM;
            case "maintainability" -> MEDIUM;
            case "style", "naming convention" -> LOW;
            default -> INFO;
        };
    }
}
