package com.aicode.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackRequest {
    
    @NotBlank(message = "Review ID is required")
    private String reviewId;
    
    @NotNull(message = "Feedback type is required")
    private FeedbackType type;
    
    private String comment; // Optional user comment
    
    private String context; // vscode, github_pr, web_app
    
    public enum FeedbackType {
        THUMBS_UP,
        THUMBS_DOWN,
        HELPFUL,
        NOT_HELPFUL,
        INCORRECT,
        FALSE_POSITIVE
    }
}
