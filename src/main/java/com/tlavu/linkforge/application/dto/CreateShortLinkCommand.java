package com.tlavu.linkforge.application.dto;

import java.time.Instant;

public record CreateShortLinkCommand(String originalUrl, Instant expiresAt) {
}
