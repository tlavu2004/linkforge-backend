package com.tlavu.linkforge.domain.event;

import java.time.Instant;

public record ShortLinkAccessedEvent(String shortCode, Instant accessedAt) {
}
