package com.aicode.model;

/**
 * A suggestion for improving code quality.
 * Used by the analysis pipeline and LocalAIService.
 */
public class Suggestion {

    private final String message;
    private final String source;   // "rule" | "ai"

    public Suggestion(String message, String source) {
        this.message = message;
        this.source  = source;
    }

    public Suggestion(String message) {
        this(message, "rule");
    }

    public String getMessage() { return message; }
    public String getSource()  { return source;  }

    @Override
    public String toString() { return message; }
}
