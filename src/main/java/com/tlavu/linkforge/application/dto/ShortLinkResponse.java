package com.tlavu.linkforge.application.dto;

import java.time.Instant;

public record ShortLinkResponse(String shortCode, String originalUrl, Instant createdAt, Instant expiresAt,
        String deleteToken) {
}
