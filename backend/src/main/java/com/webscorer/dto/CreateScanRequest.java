package com.webscorer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateScanRequest(
        @NotBlank(message = "Enter a website URL to scan.")
        @Size(max = 2048, message = "The URL must be at most 2048 characters.")
        String url
) {
}
