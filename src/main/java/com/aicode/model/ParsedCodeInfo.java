package com.aicode.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ParsedCodeInfo {
    private int methodCount;
    private int loopCount;
    private int nestedLoopCount;
    private int cyclomaticComplexity;
    @Builder.Default private List<String> methodNames = new java.util.ArrayList<>();
    private boolean hasExceptionHandling;
    private int longMethodCount; // methods with > 30 lines
}
