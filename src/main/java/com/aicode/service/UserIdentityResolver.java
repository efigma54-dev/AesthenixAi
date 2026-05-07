package com.aicode.service;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Resolves a canonical User_Identity string for rate limiting and AI usage tracking.
 *
 * Resolution order (first non-blank value wins):
 *   1. githubLogin  — GitHub user login from webhook payload (most specific)
 *   2. X-User-Id header — UUID stored locally by the VS Code extension on first install
 *   3. SHA-256(clientIP) — raw IP is never stored; hashed for privacy compliance
 *
 * The returned identity string is safe to use as a ConcurrentHashMap key.
 * Raw IP addresses are never returned or stored.
 */
public final class UserIdentityResolver {

    private static final Logger log = LoggerFactory.getLogger(UserIdentityResolver.class);
    private static final String HEADER_USER_ID = "X-User-Id";

    // Utility class — no instantiation
    private UserIdentityResolver() {}

    /**
     * Resolves the identity for a request.
     *
     * @param request     the inbound HTTP request (used for header + IP fallback)
     * @param githubLogin GitHub user login from webhook payload, or null if not a webhook
     * @return a non-null, non-blank identity string prefixed with its source:
     *         "github:{login}", "header:{uuid}", or "ip:{sha256hex}"
     */
    public static String resolve(HttpServletRequest request, String githubLogin) {
        // 1. GitHub user login (webhook path)
        if (githubLogin != null && !githubLogin.isBlank()) {
            return "github:" + githubLogin.trim().toLowerCase();
        }

        // 2. X-User-Id header (VS Code extension)
        if (request != null) {
            String userId = request.getHeader(HEADER_USER_ID);
            if (userId != null && !userId.isBlank()) {
                return "header:" + userId.trim();
            }

            // 3. SHA-256 hash of client IP — raw IP never stored
            String ip = extractClientIp(request);
            if (ip != null && !ip.isBlank()) {
                return "ip:" + sha256Hex(ip);
            }
        }

        // Fallback — should not happen in practice
        log.warn("Could not resolve any identity for request — using 'anonymous'");
        return "anonymous";
    }

    /**
     * Extracts the real client IP, respecting X-Forwarded-For for reverse proxies.
     */
    private static String extractClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            // X-Forwarded-For may be a comma-separated list; first entry is the originating client
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * Returns the lowercase hex SHA-256 digest of the input string.
     * Used to hash IP addresses so raw IPs are never stored.
     * Package-visible for testing.
     */
    public static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is guaranteed by the JVM spec — this should never happen
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
