package com.aicode.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GithubReviewRequest {

    @NotBlank(message = "GitHub URL must not be empty")
    private String repoUrl;
}
