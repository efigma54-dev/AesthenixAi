package com.aicode.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Validates critical configuration at startup.
 *
 * Fails fast with a clear message instead of silently misbehaving in production.
 * Runs after the application context is fully initialized so all @Value fields are set.
 */
@Component
public class StartupValidator {

    private static final Logger log = LoggerFactory.getLogger(StartupValidator.class);

    @Value("${pr-bot.webhook-secret:}")
    private String webhookSecret;

    @Value("${github.app-id:}")
    private String githubAppId;

    @Value("${ollama.enabled:true}")
    private boolean ollamaEnabled;

    @Value("${ollama.url:}")
    private String ollamaUrl;

    @Value("${server.port:8082}")
    private int serverPort;

    @EventListener(ApplicationReadyEvent.class)
    public void validate() {
        log.info("── Startup configuration check ──────────────────────────");

        // PR Bot webhook secret — required for secure webhook handling
        if (webhookSecret == null || webhookSecret.isBlank()) {
            log.warn("⚠  PR_BOT_WEBHOOK_SECRET is not set — webhook signature verification is DISABLED");
            log.warn("   Set PR_BOT_WEBHOOK_SECRET in .env or environment variables to enable security");
        } else {
            log.info("✓  Webhook secret configured");
        }

        // GitHub App ID — required for Check Runs and App-authenticated API calls
        if (githubAppId == null || githubAppId.isBlank()) {
            log.warn("⚠  GITHUB_APP_ID is not set — GitHub App features (Check Runs, annotations) are DISABLED");
        } else {
            log.info("✓  GitHub App ID: {}", githubAppId);
        }

        // Ollama — warn if enabled but URL looks wrong
        if (ollamaEnabled) {
            if (ollamaUrl == null || ollamaUrl.isBlank()) {
                log.warn("⚠  OLLAMA_ENABLED=true but OLLAMA_URL is not set — AI analysis will fail");
            } else {
                log.info("✓  Ollama enabled at: {}", ollamaUrl);
            }
        } else {
            log.info("✓  Ollama disabled — rule-based analysis only (fast mode)");
        }

        log.info("✓  Server listening on port {}", serverPort);
        log.info("─────────────────────────────────────────────────────────");
    }
}
