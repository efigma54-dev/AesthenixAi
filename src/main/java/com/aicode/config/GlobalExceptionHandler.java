package com.aicode.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ── Validation (400) ───────────────────────────────────────
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fields = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String field = ((FieldError) error).getField();
            fields.put(field, error.getDefaultMessage());
        });
        String first = fields.values().stream().findFirst().orElse("invalid input");
        log.warn("Validation failed: {}", fields);
        return ResponseEntity.badRequest().body(Map.of(
                "type", "validation",
                "error", "Validation failed: " + first,
                "fields", fields));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArg(IllegalArgumentException ex) {
        log.warn("Illegal argument: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(Map.of(
                "type", "validation",
                "error", ex.getMessage() != null ? ex.getMessage() : "Invalid input"));
    }

    // ── AI service HTTP errors (502) ───────────────────────────────
    @ExceptionHandler(WebClientResponseException.class)
    public ResponseEntity<Map<String, Object>> handleWebClientResponse(WebClientResponseException ex) {
        int status = ex.getStatusCode().value();
        log.error("AI service HTTP error: status={}", status);
        String msg = switch (status) {
            case 401 -> "AI service authentication failed.";
            case 429 -> "AI service rate limit exceeded. Please wait before retrying.";
            case 503 -> "AI service is temporarily unavailable. Try again shortly.";
            default -> "AI service error: HTTP " + status;
        };
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of(
                "type", status == 429 ? "ratelimit" : status == 401 ? "auth" : "server",
                "error", msg));
    }

    // ── AI service connection failure (503) ────────────────────────
    @ExceptionHandler(WebClientRequestException.class)
    public ResponseEntity<Map<String, Object>> handleWebClientRequest(WebClientRequestException ex) {
        log.error("AI service connection failed: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                "type", "network",
                "error", "Could not connect to the AI service. Check that Ollama is running locally."));
    }

    // ── Timeout (503) ──────────────────────────────────────────
    @ExceptionHandler(TimeoutException.class)
    public ResponseEntity<Map<String, Object>> handleTimeout(TimeoutException ex) {
        log.warn("Request timed out: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                "type", "timeout",
                "error", "Request timed out. The AI service took too long to respond."));
    }

    // ── General runtime (500) ──────────────────────────────────
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntime(RuntimeException ex) {
        String msg = ex.getMessage() != null ? ex.getMessage() : "Internal server error";
        log.error("Unhandled runtime error: {}", msg);

        if (msg.contains("timeout") || msg.contains("TimeoutException") || msg.contains("ReadTimeout"))
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                    "type", "timeout",
                    "error", "Request timed out. The AI service took too long to respond."));

        if (msg.contains("rate limit") || msg.contains("429"))
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of(
                    "type", "ratelimit",
                    "error", msg));

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "type", "server",
                "error", msg));
    }

    // ── Catch-all (500) ────────────────────────────────────────
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleAll(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "type", "unknown",
                "error", "An unexpected error occurred."));
    }
}
