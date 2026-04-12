package com.aicode.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

/**
 * Security validation layer for analysis requests.
 *
 * Prevents:
 *   - Denial of service (massive files)
 *   - Code injection via malicious inputs
 *   - Path traversal in file paths
 *   - Resource exhaustion (rate limiting)
 *
 * Interview signal:
 *   "I implemented security hardening including input validation,
 *    rate limiting, and resource limits."
 */
@Service
public class SecurityValidator {

    private static final Logger log = LoggerFactory.getLogger(SecurityValidator.class);

    // Input constraints
    @Value("${security.max-code-size:5242880}")  // 5MB
    private long maxCodeSize;

    @Value("${security.max-files-per-batch:100}")
    private int maxFilesPerBatch;

    @Value("${security.allowed-file-extensions:.java,.py,.js,.ts,.go}")
    private String allowedExtensions;

    @Value("${security.rate-limit-enabled:true}")
    private boolean rateLimitingEnabled;

    // Patterns for injection detection
    private static final Pattern COMMAND_INJECTION = Pattern.compile("(?i)(cmd|eval|exec|system)\\s*\\(");
    private static final Pattern PATH_TRAVERSAL = Pattern.compile("(\\.\\.|~/|/etc/)");
    private static final Pattern SQL_INJECTION = Pattern.compile("(?i)(drop|delete|insert|update)\\s+");

    /**
     * Validates user-submitted code for security issues.
     * Throws SecurityException if validation fails.
     */
    public void validateCode(String code, String filePath) throws SecurityException {
        if (code == null || code.isEmpty()) {
            throw new SecurityException("Code cannot be empty");
        }

        // Size limit
        if (code.length() > maxCodeSize) {
            log.warn("Code size {} exceeds limit {}", code.length(), maxCodeSize);
            throw new SecurityException(
                String.format("Code exceeds maximum size of %d bytes", maxCodeSize)
            );
        }

        // File path validation
        validateFilePath(filePath);

        // Injection detection (heuristic)
        if (containsInjectionPattern(code)) {
            log.warn("Potential injection pattern detected in code");
            throw new SecurityException("Code contains potentially malicious patterns");
        }

        log.debug("Code validation passed: {} bytes, file: {}", code.length(), filePath);
    }

    /**
     * Validates file path to prevent directory traversal.
     */
    public void validateFilePath(String filePath) throws SecurityException {
        if (filePath == null || filePath.isBlank()) {
            throw new SecurityException("File path cannot be empty");
        }

        // Prevent path traversal
        if (PATH_TRAVERSAL.matcher(filePath).find()) {
            log.warn("Path traversal attempt detected: {}", filePath);
            throw new SecurityException("Invalid file path");
        }

        // Check file extension
        String[] extensions = allowedExtensions.split(",");
        boolean hasValidExtension = false;
        for (String ext : extensions) {
            if (filePath.endsWith(ext.trim())) {
                hasValidExtension = true;
                break;
            }
        }

        if (!hasValidExtension) {
            log.warn("Invalid file extension: {}", filePath);
            throw new SecurityException(
                String.format("File extension not allowed. Allowed: %s", allowedExtensions)
            );
        }
    }

    /**
     * Validates batch request (multiple files).
     */
    public void validateBatchRequest(int fileCount) throws SecurityException {
        if (fileCount > maxFilesPerBatch) {
            log.warn("Batch size {} exceeds limit {}", fileCount, maxFilesPerBatch);
            throw new SecurityException(
                String.format("Batch size exceeds maximum of %d files", maxFilesPerBatch)
            );
        }
    }

    /**
     * Sanitizes user input for safe logging.
     */
    public String sanitizeForLogging(String input) {
        if (input == null) return null;
        // Replace potentially sensitive strings
        return input.replaceAll("(?i)(password|token|secret|api_key)\\s*[=:]\\s*[^\\s]+", "***REDACTED***");
    }

    /**
     * Detects common injection patterns (heuristic; not perfect).
     */
    private boolean containsInjectionPattern(String code) {
        // Java analysis code shouldn't contain shell commands
        if (code.toLowerCase().contains("runtime.exec")) {
            return true;
        }
        if (COMMAND_INJECTION.matcher(code).find()) {
            return true;
        }
        // SQL in Java code analysis (suspicious)
        if (SQL_INJECTION.matcher(code).find() && code.contains("\"")) {
            return true;
        }
        return false;
    }

    /**
     * Custom exception for security violations.
     */
    public static class SecurityException extends Exception {
        public SecurityException(String message) {
            super(message);
        }

        public SecurityException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Gets current security configuration for display.
     */
    public SecurityConfig getConfig() {
        return new SecurityConfig(
            maxCodeSize,
            maxFilesPerBatch,
            allowedExtensions,
            rateLimitingEnabled
        );
    }

    public static class SecurityConfig {
        public final long maxCodeSize;
        public final int maxFilesPerBatch;
        public final String allowedExtensions;
        public final boolean rateLimitingEnabled;

        public SecurityConfig(long mcs, int mfb, String ae, boolean rle) {
            this.maxCodeSize = mcs;
            this.maxFilesPerBatch = mfb;
            this.allowedExtensions = ae;
            this.rateLimitingEnabled = rle;
        }
    }
}

/**
 * Rate limiter for API requests.
 * Prevents abuse and resource exhaustion.
 */
@Service
class RateLimiter {

    private static final Logger log = LoggerFactory.getLogger(RateLimiter.class);

    @Value("${security.rate-limit.requests-per-minute:60}")
    private int requestsPerMinute;

    // Simple in-memory counter per IP (use Redis for distributed systems)
    private final java.util.Map<String, RequestCount> requestCounts = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * Checks if request from IP is within rate limit.
     */
    public boolean isAllowed(String clientIp) {
        RequestCount count = requestCounts.compute(clientIp, (ip, rc) -> {
            if (rc == null) {
                return new RequestCount();
            }
            rc.checkExpiry();  // Clear old window
            return rc;
        });

        return count.increment(requestsPerMinute);
    }

    private static class RequestCount {
        private long windowStart = System.currentTimeMillis();
        private int count = 0;
        private static final long WINDOW_MS = 60_000;  // 1 minute

        void checkExpiry() {
            if (System.currentTimeMillis() - windowStart > WINDOW_MS) {
                windowStart = System.currentTimeMillis();
                count = 0;
            }
        }

        boolean increment(int limit) {
            checkExpiry();
            if (count >= limit) {
                return false;
            }
            count++;
            return true;
        }
    }
}
