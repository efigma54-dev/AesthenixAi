package com.aicode;

import com.aicode.model.ParsedCodeInfo;
import com.aicode.service.CodeAnalysisService;
import com.aicode.service.ScoringService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CodeAnalysisServiceTest {

    private final CodeAnalysisService analysisService = new CodeAnalysisService();
    private final ScoringService scoringService = new ScoringService();

    @Test
    void testNestedLoopDetection() {
        String code = """
                public class Test {
                    public void run() {
                        for (int i = 0; i < 10; i++) {
                            for (int j = 0; j < 10; j++) {
                                System.out.println(i + j);
                            }
                        }
                    }
                }
                """;

        ParsedCodeInfo info = analysisService.analyze(code);
        assertEquals(1, info.getMethodCount());
        assertTrue(info.getNestedLoopCount() > 0);
        assertFalse(info.isHasExceptionHandling());
    }

    @Test
    void testExceptionHandlingDetection() {
        String code = """
                public class Test {
                    public void run() {
                        try {
                            int x = 1 / 0;
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                """;

        ParsedCodeInfo info = analysisService.analyze(code);
        assertTrue(info.isHasExceptionHandling());
    }

    @Test
    void testScoringDeductsForNestedLoops() {
        ParsedCodeInfo info = ParsedCodeInfo.builder()
                .nestedLoopCount(2)
                .cyclomaticComplexity(3)
                .longMethodCount(0)
                .hasExceptionHandling(true)
                .methodCount(1)
                .loopCount(2)
                .build();

        int score = scoringService.calculateScore(info);
        assertTrue(score < 100);
        assertEquals(80, score); // 100 - 2*10
    }
}
