package com.aicode.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Enforces a per-identity daily cap on AI-powered analyses.
 *
 * When the limit is reached, the caller should fall back to rule-only analysis
 * rather than rejecting the request outright — this keeps UX smooth while
 * controlling AI compute costs.
 *
 * Counter reset: fixed daily boundary at midnight UTC (not a rolling 24h window).
 * Identity resolution order: GitHub login → X-User-Id header → SHA-256(IP).
 */
@Service
public class AIUsageLimiter {

    private static final Logger log = LoggerFactory.getLogger(AIUsageLimiter.class);

    @Value("${ai.daily-limit:50}")
    private int dailyLimit;

    // Identity → DailyCounter — lock-free reads via ConcurrentHashMap
    private final ConcurrentHashMap<String, DailyCounter> counters = new ConcurrentHashMap<>();

    /**
     * Checks whether the identity is under the daily AI limit and, if so,
     * increments the counter atomically.
     *
     * @param identity the resolved User_Identity string
     * @return true if the AI call is allowed (counter incremented),
     *         false if the daily limit has been reached (counter unchanged)
     */
    public boolean allowAndIncrement(String identity) {
        DailyCounter counter = counters.computeIfAbsent(identity, k -> new DailyCounter());
        int current = counter.count.get();
        if (current >= dailyLimit) {
            log.info("AI daily limit reached for identity={} count={} limit={}", identity, current, dailyLimit);
            return false;
        }
        counter.count.incrementAndGet();
        return true;
    }

    /**
     * Returns the current AI usage count for an identity (0 if not seen today).
     */
    public int getUsageCount(String identity) {
        DailyCounter counter = counters.get(identity);
        return counter == null ? 0 : counter.count.get();
    }

    /**
     * Returns the top 10 identities by AI usage count, sorted descending.
     * Used by the /api/metrics endpoint.
     */
    public List<Map.Entry<String, Integer>> getTop10Usage() {
        List<Map.Entry<String, Integer>> entries = new ArrayList<>();
        counters.forEach((identity, counter) ->
                entries.add(Map.entry(identity, counter.count.get())));
        entries.sort(Comparator.<Map.Entry<String, Integer>, Integer>comparing(Map.Entry::getValue).reversed());
        return entries.size() > 10 ? entries.subList(0, 10) : entries;
    }

    /**
     * Resets all per-identity counters at midnight UTC.
     * Fixed daily boundary — not a rolling 24-hour window.
     */
    @Scheduled(cron = "0 0 0 * * *", zone = "UTC")
    public void resetDailyCounters() {
        int size = counters.size();
        counters.clear();
        log.info("AI daily counters reset at midnight UTC — cleared {} identities", size);
    }

    // ── Inner types ────────────────────────────────────────────

    public static class DailyCounter {
        public final AtomicInteger count     = new AtomicInteger(0);
        public volatile LocalDate  resetDate = LocalDate.now(ZoneOffset.UTC);
    }
}
