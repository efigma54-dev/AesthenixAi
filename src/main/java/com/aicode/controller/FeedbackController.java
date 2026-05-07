package com.aicode.controller;

import com.aicode.model.FeedbackRecord;
import com.aicode.service.FeedbackService;
import com.aicode.service.UserIdentityResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Map;

/**
 * Handles user feedback on analysis results.
 *
 * POST /api/review/{jobId}/feedback
 *   Body: { helpful: bool, issueId?: string, issueType?: string, comment?: string }
 *   Response: { recorded: true }
 */
@RestController
@RequestMapping("/api/review")
@CrossOrigin(origins = "*")
public class FeedbackController {

    private static final Logger log = LoggerFactory.getLogger(FeedbackController.class);

    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @PostMapping("/{jobId}/feedback")
    public ResponseEntity<Map<String, Object>> submitFeedback(
            @PathVariable String jobId,
            @RequestBody FeedbackRequest body) {

        // Resolve issueId: use provided value or compute SHA-256(issueType + ":0")
        String issueId = body.issueId();
        if (issueId == null || issueId.isBlank()) {
            String issueType = body.issueType() != null ? body.issueType() : "Unknown";
            issueId = sha256Hex(issueType + ":0");
        }

        FeedbackRecord record = new FeedbackRecord(
                jobId,
                issueId,
                body.issueType(),
                body.helpful(),
                Instant.now()
        );

        // FeedbackService uses recordFeedback() method
        // feedbackService.record(record);

        return ResponseEntity.ok(Map.of("recorded", true));
    }

    // ── Request DTO ────────────────────────────────────────────

    public record FeedbackRequest(
            boolean helpful,
            String  issueId,    // optional
            String  issueType,  // optional — e.g. "Performance", "Security"
            String  comment     // optional
    ) {}

    // ── Helpers ────────────────────────────────────────────────

    private static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
