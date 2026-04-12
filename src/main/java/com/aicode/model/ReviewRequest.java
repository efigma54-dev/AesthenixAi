package com.aicode.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReviewRequest {

    @NotBlank(message = "Code must not be empty")
    @Size(max = 100_000, message = "Code exceeds the 100 KB limit")
    private String code;

    /** Returns sanitized code — strips null bytes, normalizes line endings */
    public String getSanitizedCode() {
        if (code == null) return "";
        return code
                .replace("\u0000", "")   // strip null bytes
                .replace("\r\n", "\n")   // normalize CRLF
                .strip();
    }
}
