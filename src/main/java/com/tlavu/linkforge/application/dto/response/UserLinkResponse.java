package com.tlavu.linkforge.application.dto.response;

import java.time.Instant;

public record UserLinkResponse(
        String shortCode,
        String originalUrl,
        Instant createdAt,
        Instant expiresAt,
        long clickCount,
        boolean expired) {
}
