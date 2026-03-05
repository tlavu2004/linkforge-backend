package com.tlavu.linkforge.application.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.time.Instant;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ShortLinkResponse(
                String shortCode,
                String originalUrl,
                Instant createdAt,
                Instant expiresAt,
                String deleteToken,
                boolean skipAds,
                String qrCode) {
}
