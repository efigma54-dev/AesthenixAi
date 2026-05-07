package com.aicode.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Runtime-mutable feature flags.
 *
 * Uses AtomicBoolean so flags can be toggled at runtime (e.g. by the failover
 * monitor in ReviewJobService) without requiring a restart or redeployment.
 *
 * Initial values are read from application.yml at startup.
 */
@Service
public class FeatureFlagService {

    private static final Logger log = LoggerFactory.getLogger(FeatureFlagService.class);

    @Value("${features.ai-enabled:true}")
    private boolean initialAiEnabled;

    private final AtomicBoolean aiEnabled = new AtomicBoolean(true);

    @PostConstruct
    public void init() {
        aiEnabled.set(initialAiEnabled);
        log.info("FeatureFlagService initialized — ai-enabled={}", initialAiEnabled);
    }

    /** Returns true when AI analysis is enabled. */
    public boolean isAiEnabled() {
        return aiEnabled.get();
    }

    /**
     * Enables or disables AI analysis at runtime.
     * Called by the failover monitor in ReviewJobService.
     */
    public void setAiEnabled(boolean enabled) {
        boolean previous = aiEnabled.getAndSet(enabled);
        if (previous != enabled) {
            log.info("Feature flag ai-enabled changed: {} → {}", previous, enabled);
        }
    }
}
