package com.aicode.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SafeFixRequest {
    @NotBlank(message = "Code is required")
    private String code;
    
    private String language = "java";
    
    public String getSanitizedCode() {
        return code != null ? code.trim() : "";
    }
}
