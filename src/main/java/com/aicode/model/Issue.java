package com.aicode.model;

public class Issue {

    private String title;
    private String description;
    private String type;
    private int line;
    private String suggestion;
    private Severity severity;

    // ✅ Main constructor (existing system compatible)
    public Issue(String title, String description, String type, int line, String suggestion, int severityLevel) {
        this.title = title;
        this.description = description;
        this.type = type;
        this.line = line;
        this.suggestion = suggestion;
        this.severity = mapSeverity(severityLevel);
    }

    // ✅ Minimal constructor (your simplified usage)
    public Issue(String message, int line, Severity severity) {
        this.title = message;
        this.description = message;
        this.type = "GENERAL";
        this.line = line;
        this.suggestion = "";
        this.severity = severity;
    }

    private Severity mapSeverity(int level) {
        if (level >= 4)
            return Severity.CRITICAL;
        if (level == 3)
            return Severity.HIGH;
        if (level == 2)
            return Severity.MEDIUM;
        if (level == 1)
            return Severity.LOW;
        return Severity.INFO;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getType() {
        return type;
    }

    public int getLine() {
        return line;
    }

    public String getSuggestion() {
        return suggestion;
    }

    public int getSeverity() {
        return severity != null ? severity.getLevel() : 0;
    }

    public Severity getSeverityEnum() {
        return severity;
    }

    public String getMessage() {
        return description != null ? description : title;
    }
}
