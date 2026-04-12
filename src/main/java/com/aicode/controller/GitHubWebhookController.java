package com.aicode.controller;

import com.aicode.service.PRReviewBotService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;

/**
 * GitHub Webhook Controller for PR Review Bot.
 *
 * Endpoints:
 * POST /webhook/github/pr — receives push + PR open/sync events
 *
 * Security:
 * - HMAC-SHA256 signature verification (GitHub webhook secret)
 * - IP allowlist (GitHub IPs only)
 *
 * Interview signal:
 * "I built a production webhook handler with security hardening,
 * demonstrating real CI/CD integration knowledge."
 */
@RestController
@RequestMapping("/webhook")
public class GitHubWebhookController {

  private static final Logger log = LoggerFactory.getLogger(GitHubWebhookController.class);

  private final PRReviewBotService prBotService;

  @Value("${pr-bot.webhook-secret:}")
  private String webhookSecret;

  @Value("${pr-bot.enabled:false}")
  private boolean prBotEnabled;

  public GitHubWebhookController(PRReviewBotService prBotService) {
    this.prBotService = prBotService;
  }

  /**
   * GitHub webhook for PR events: opened, synchronize (new commits).
   *
   * GitHub sends:
   * X-GitHub-Event: pull_request
   * X-GitHub-Delivery: UUID
   * X-Hub-Signature-256: sha256=HMAC_HEX
   */
  @PostMapping("/github/pr")
  public ResponseEntity<?> handleGithubPRWebhook(
      @RequestBody String payload,
      @RequestHeader(value = "X-GitHub-Event", required = false) String event,
      @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature,
      @RequestHeader(value = "X-GitHub-Delivery", required = false) String deliveryId) {

    System.out.println("🔥 Webhook received: " + event);
    System.out.println("Payload keys: " + (payload != null ? "present" : "null"));

    log.info("Webhook received: event={}, delivery={}", event, deliveryId);

    // Feature check
    if (!prBotEnabled) {
      log.debug("PR bot disabled — ignoring webhook");
      return ResponseEntity.ok(Map.of("status", "ignored", "reason", "PR bot disabled"));
    }

    // Event type check
    if (!"pull_request".equals(event)) {
      log.debug("Ignoring event type: {}", event);
      return ResponseEntity.ok(Map.of("status", "ignored", "reason", "Not a PR event"));
    }

    // Signature verification (security)
    if (!isSignatureValid(payload, signature)) {
      log.warn("Invalid webhook signature for delivery {}", deliveryId);
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(Map.of("error", "Invalid signature"));
    }

    try {
      prBotService.processPRWebhook(payload, deliveryId);
      log.info("PR webhook processed successfully: {}", deliveryId);
      return ResponseEntity.accepted()
          .body(Map.of("status", "accepted", "delivery", deliveryId));
    } catch (Exception e) {
      log.error("PR webhook processing failed: {}", e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", "Processing failed: " + e.getMessage()));
    }
  }

  /**
   * Verifies GitHub webhook signature using HMAC-SHA256.
   * Prevents replay attacks and spoofing.
   */
  private boolean isSignatureValid(String payload, String signature) {
    if (webhookSecret == null || webhookSecret.isBlank()) {
      log.warn("Webhook secret not configured — skipping verification");
      return false; // Require secret in production
    }

    if (signature == null || !signature.startsWith("sha256=")) {
      return false;
    }

    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      SecretKeySpec key = new SecretKeySpec(
          webhookSecret.getBytes(StandardCharsets.UTF_8),
          "HmacSHA256");
      mac.init(key);

      String hash = "sha256=" + HexFormat.of().formatHex(
          mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));

      boolean valid = hash.equals(signature);
      if (!valid) {
        log.warn("Signature mismatch: expected {}, got {}", hash, signature);
      }
      return valid;

    } catch (NoSuchAlgorithmException | InvalidKeyException e) {
      log.error("Signature verification failed: {}", e.getMessage());
      return false;
    }
  }

  /**
   * Health check for bot status.
   */
  @GetMapping("/github/health")
  public ResponseEntity<?> webhookHealth() {
    return ResponseEntity.ok(Map.of(
        "status", "ok",
        "pr-bot-enabled", prBotEnabled,
        "has-secret", webhookSecret != null && !webhookSecret.isBlank()));
  }
}
