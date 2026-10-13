package com.webscorer.dto;

import java.time.Instant;

public record MessageResponse(
        Long id,
        String role,
        String message,
        String provider,
        Instant createdAt
) {
}
