package com.aicode.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ReviewResponse {
    private int score;
    private List<Issue> issues;
    private List<String> suggestions;
    private String improvedCode;
    private ParsedCodeInfo parsedInfo;
}
