package com.aicode;

import com.aicode.service.AIUsageLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for AIUsageLimiter.
 * Verifies daily cap enforcement, reset, and top-10 reporting.
 */
class AIUsageLimiterTest {

    private AIUsageLimiter limiter;

    @BeforeEach
    void setUp() {
        limiter = new AIUsageLimiter();
        ReflectionTestUtils.setField(limiter, "dailyLimit", 5);
    }

    @Test
    void allowsExactlyNCallsBeforeBlocking() {
        String identity = "test-user";
        for (int i = 0; i < 5; i++) {
            assertThat(limiter.allowAndIncrement(identity))
                    .as("Call %d should be allowed", i + 1)
                    .isTrue();
        }
        // 6th call must be blocked
        assertThat(limiter.allowAndIncrement(identity)).isFalse();
    }

    @Test
    void differentIdentitiesHaveIndependentCounters() {
        ReflectionTestUtils.setField(limiter, "dailyLimit", 2);

        assertThat(limiter.allowAndIncrement("user-a")).isTrue();
        assertThat(limiter.allowAndIncrement("user-a")).isTrue();
        assertThat(limiter.allowAndIncrement("user-a")).isFalse(); // blocked

        // user-b is unaffected
        assertThat(limiter.allowAndIncrement("user-b")).isTrue();
    }

    @Test
    void resetClearsAllCounters() {
        limiter.allowAndIncrement("user-x");
        limiter.allowAndIncrement("user-x");
        limiter.allowAndIncrement("user-x");
        limiter.allowAndIncrement("user-x");
        limiter.allowAndIncrement("user-x");
        assertThat(limiter.allowAndIncrement("user-x")).isFalse();

        limiter.resetDailyCounters();

        // After reset, user-x should be allowed again
        assertThat(limiter.allowAndIncrement("user-x")).isTrue();
    }

    @Test
    void getTop10UsageReturnsAtMost10Entries() {
        for (int i = 0; i < 15; i++) {
            limiter.allowAndIncrement("user-" + i);
        }
        List<Map.Entry<String, Integer>> top10 = limiter.getTop10Usage();
        assertThat(top10).hasSizeLessThanOrEqualTo(10);
    }

    @Test
    void getTop10UsageIsSortedDescending() {
        ReflectionTestUtils.setField(limiter, "dailyLimit", 100);
        for (int i = 0; i < 5; i++) limiter.allowAndIncrement("heavy-user");
        for (int i = 0; i < 2; i++) limiter.allowAndIncrement("light-user");

        List<Map.Entry<String, Integer>> top = limiter.getTop10Usage();
        assertThat(top.get(0).getKey()).isEqualTo("heavy-user");
        assertThat(top.get(0).getValue()).isEqualTo(5);
    }

    @Test
    void getUsageCountReturnsZeroForUnknownIdentity() {
        assertThat(limiter.getUsageCount("never-seen")).isEqualTo(0);
    }
}
