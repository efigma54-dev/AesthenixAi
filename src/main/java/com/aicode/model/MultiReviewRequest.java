package com.aicode.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class MultiReviewRequest {

    @NotEmpty(message = "Files list must not be empty")
    private List<FileEntry> files;

    @Data
    public static class FileEntry {
        @NotBlank(message = "File name must not be empty")
        @Pattern(regexp = "^[\\w\\-./]+\\.java$", message = "Invalid filename — must be a .java path with no special characters")
        private String name;

        @NotBlank(message = "File code must not be empty")
        @Size(max = 100_000, message = "File code exceeds the 100 KB limit")
        private String code;
    }
}
