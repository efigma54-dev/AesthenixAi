package com.aicode.controller;

import com.aicode.service.WarmUpService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Lightweight health endpoint for uptime monitors and cold-start detection.
 *
 * GET /api/health/ping
 *   Response: { "status": "warming_up" | "ready", "uptime": <seconds> }
 *   Always returns HTTP 200 — no analysis or AI calls.
 *
 * This is separate from the existing GET /api/health plain-text endpoint
 * in CodeReviewController, which is kept for backward compatibility.
 */
@RestController
@RequestMapping("/api/health")
@CrossOrigin(origins = "*")
public class HealthController {

    private final WarmUpService warmUpService;

    public HealthController(WarmUpService warmUpService) {
        this.warmUpService = warmUpService;
    }

    /**
     * Returns the current warm-up status and uptime in seconds.
     * Designed to respond within 500ms — no heavy operations.
     */
    @GetMapping("/ping")
    public ResponseEntity<Map<String, Object>> ping() {
        String status = warmUpService.isWarmupComplete() ? "ready" : "warming_up";
        return ResponseEntity.ok(Map.of(
                "status", status,
                "uptime", warmUpService.getUptimeSeconds()
        ));
    }
}
