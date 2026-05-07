package com.aicode.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Verifies GitHub webhook HMAC-SHA256 signatures.
 *
 * GitHub sends: X-Hub-Signature-256: sha256=<hex>
 * We compute:   HMAC-SHA256(webhookSecret, rawPayload)
 * and compare using constant-time comparison to prevent timing attacks.
 *
 * If no secret is configured, verification is skipped (dev mode).
 */
@Service
public class WebhookSignatureService {

    private static final Logger log = LoggerFactory.getLogger(WebhookSignatureService.class);

    @Value("${pr-bot.webhook-secret:}")
    private String webhookSecret;

    /**
     * Returns true if the signature is valid (or if no secret is configured).
     * Returns false if a secret is configured but the signature doesn't match.
     */
    public boolean isValid(String payload, String signatureHeader) {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            log.debug("No webhook secret configured — skipping signature verification");
            return true;
        }

        if (signatureHeader == null || !signatureHeader.startsWith("sha256=")) {
            log.warn("Missing or malformed X-Hub-Signature-256 header");
            return false;
        }

        try {
            String expected = signatureHeader.substring("sha256=".length());
            String computed = computeHmac(payload);
            boolean valid = MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                computed.getBytes(StandardCharsets.UTF_8)
            );
            if (!valid) log.warn("Webhook signature mismatch — possible spoofed request");
            return valid;
        } catch (Exception e) {
            log.error("Signature verification error: {}", e.getMessage());
            return false;
        }
    }

    private String computeHmac(String payload) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(
            webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(hash);
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
