package com.webscorer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CopilotRequest(
        @NotBlank(message = "Ask Webby a question first.")
        @Size(max = 2000, message = "Questions are limited to 2000 characters.")
        String question
) {
}
