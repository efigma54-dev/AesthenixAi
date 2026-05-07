package com.aicode.service;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Handles cold start warm-up for Render deployments.
 * 
 * Warm-up sequence:
 * 1. Parse a minimal Java snippet to initialize JavaParser
 * 2. Ping Ollama to confirm AI model is loaded
 * 
 * This prevents the first user request from timing out.
 */
@Service
public class WarmUpService {
    
    private static final Logger log = LoggerFactory.getLogger(WarmUpService.class);
    
    @Value("${warmup.enabled:true}")
    private boolean warmupEnabled;
    
    @Value("${warmup.window-seconds:20}")
    private int warmupWindowSeconds;
    
    @Value("${ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;
    
    private final AtomicBoolean warmupComplete = new AtomicBoolean(false);
    private final AtomicReference<Instant> startupTime = new AtomicReference<>(Instant.now());
    private final RestTemplate restTemplate = new RestTemplate();
    
    /**
     * Execute warm-up sequence when application is ready.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void warmUp() {
        if (!warmupEnabled) {
            log.info("Warm-up disabled via configuration");
            warmupComplete.set(true);
            return;
        }
        
        log.info("Starting warm-up sequence (window: {}s)", warmupWindowSeconds);
        startupTime.set(Instant.now());
        
        // Run warm-up in background thread
        new Thread(() -> {
            try {
                warmUpJavaParser();
                warmUpOllama();
                
                warmupComplete.set(true);
                long duration = Duration.between(startupTime.get(), Instant.now()).toMillis();
                log.info("✓ Warm-up complete in {}ms", duration);
                
            } catch (Exception e) {
                log.warn("Warm-up failed (non-fatal): {}", e.getMessage());
                warmupComplete.set(true);  // Don't block forever
            }
        }, "warmup-thread").start();
    }
    
    /**
     * Initialize JavaParser by parsing a minimal snippet.
     */
    private void warmUpJavaParser() {
        try {
            log.info("Warming up JavaParser...");
            
            String snippet = """
                public class WarmUp {
                    public void test() {
                        System.out.println("warm");
                    }
                }
                """;
            
            JavaParser parser = new JavaParser();
            ParseResult<CompilationUnit> result = parser.parse(snippet);
            
            if (result.isSuccessful()) {
                log.info("✓ JavaParser warm-up complete");
            } else {
                log.warn("JavaParser warm-up parse failed (non-fatal)");
            }
            
        } catch (Exception e) {
            log.warn("JavaParser warm-up failed: {}", e.getMessage());
        }
    }
    
    /**
     * Ping Ollama to confirm AI model is loaded.
     */
    private void warmUpOllama() {
        try {
            log.info("Warming up Ollama at {}...", ollamaBaseUrl);
            
            String url = ollamaBaseUrl + "/api/tags";
            restTemplate.getForObject(url, String.class);
            
            log.info("✓ Ollama warm-up complete");
            
        } catch (Exception e) {
            log.warn("Ollama warm-up failed (non-fatal): {}", e.getMessage());
        }
    }
    
    /**
     * Check if warm-up is complete.
     */
    public boolean isWarmupComplete() {
        return warmupComplete.get();
    }
    
    /**
     * Check if we're still in the warm-up window.
     */
    public boolean isInWarmupWindow() {
        if (!warmupEnabled) {
            return false;
        }
        
        long secondsSinceStartup = Duration.between(startupTime.get(), Instant.now()).getSeconds();
        return secondsSinceStartup < warmupWindowSeconds;
    }
    
    /**
     * Get uptime in seconds.
     */
    public long getUptimeSeconds() {
        return Duration.between(startupTime.get(), Instant.now()).getSeconds();
    }
    
    /**
     * Get warm-up status for health endpoint.
     */
    public String getStatus() {
        if (warmupComplete.get()) {
            return "ready";
        } else if (isInWarmupWindow()) {
            return "warming_up";
        } else {
            return "ready";  // Window expired, accept traffic anyway
        }
    }
}
