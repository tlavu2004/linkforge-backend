package com.tlavu.linkforge.application.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ShortLinkResponse(
        String shortCode,
        String originalUrl,
        Instant createdAt,
        Instant expiresAt,
        Boolean enabled,
        String deleteToken) {
}
