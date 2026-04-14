package com.aicode.model;

public class Issue {

    private String message;
    private int line;
    private Severity severity;

    public Issue(String message, int line, Severity severity) {
        this.message = message;
        this.line = line;
        this.severity = severity;
    }

    public String getMessage() {
        return message;
    }

    public int getLine() {
        return line;
    }

    public Severity getSeverity() {
        return severity;
    }
}
