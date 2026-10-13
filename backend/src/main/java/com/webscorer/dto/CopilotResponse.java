package com.webscorer.dto;

import java.time.Instant;
import java.util.List;

/**
 * @param provider LOCAL = rule-based guidance from the saved report,
 *                  LLM = configured AI provider
 */
public record CopilotResponse(
        String message,
        String provider,
        String note,
        List<String> suggestions,
        Instant createdAt
) {
}
