package com.aicode.model;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class SafeFixResponse {
    private String fixedCode;
    private List<AppliedFix> appliedFixes;
    private int fixCount;
    
    @Data
    @Builder
    public static class AppliedFix {
        private String type;
        private String description;
        private int line;
    }
}
