package com.tlavu.linkforge.application.dto.command;

import java.time.Instant;

public record CreateShortLinkCommand(String originalUrl, Instant expiresAt, String customAlias) {
}
