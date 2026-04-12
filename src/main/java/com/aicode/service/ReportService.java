package com.aicode.service;

import com.aicode.model.ReportResponse;
import com.aicode.model.ReviewResponse;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory report store.
 * Reports expire after 24 hours and are capped at 500 entries.
 * For production, swap the map for a database or Redis.
 */
@Service
public class ReportService {

    private static final int    MAX_REPORTS  = 500;
    private static final long   TTL_MS       = 24 * 60 * 60 * 1000L;

    private record Entry(ReportResponse report, long createdMs) {}

    private final Map<String, Entry> store = new ConcurrentHashMap<>();

    /** Persist a review result and return a shareable report */
    public ReportResponse save(ReviewResponse review, String baseUrl) {
        evictExpired();

        if (store.size() >= MAX_REPORTS) {
            // Evict oldest entry
            store.entrySet().stream()
                 .min(Map.Entry.comparingByValue((a, b) -> Long.compare(a.createdMs(), b.createdMs())))
                 .map(Map.Entry::getKey)
                 .ifPresent(store::remove);
        }

        String id = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String shareUrl = baseUrl + "/api/report/" + id;

        ReportResponse report = ReportResponse.builder()
                .reportId(id)
                .shareUrl(shareUrl)
                .createdAt(Instant.now())
                .score(review.getScore())
                .issues(review.getIssues())
                .suggestions(review.getSuggestions())
                .improvedCode(review.getImprovedCode())
                .parsedInfo(review.getParsedInfo())
                .build();

        store.put(id, new Entry(report, System.currentTimeMillis()));
        return report;
    }

    /** Retrieve a report by ID, or null if not found / expired */
    public ReportResponse get(String id) {
        Entry entry = store.get(id);
        if (entry == null) return null;
        if (System.currentTimeMillis() - entry.createdMs() > TTL_MS) {
            store.remove(id);
            return null;
        }
        return entry.report();
    }

    private void evictExpired() {
        long now = System.currentTimeMillis();
        store.entrySet().removeIf(e -> now - e.getValue().createdMs() > TTL_MS);
    }
}
