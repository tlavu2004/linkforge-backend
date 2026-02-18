package com.tlavu.linkforge.domain.entity;

import com.tlavu.linkforge.domain.valueobject.OriginalUrl;
import com.tlavu.linkforge.domain.valueobject.ShortCode;
import com.tlavu.linkforge.domain.exception.InvalidShortLinkException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.time.Instant;

@Getter
@ToString
@EqualsAndHashCode(of = "id")
public class ShortLink {

    private final Long id;
    private final ShortCode shortCode;
    private final OriginalUrl originalUrl;
    private final Instant createdAt;

    private Instant expiresAt;
    private boolean isEnabled;
    private long clickCount;
    private String deleteTokenHash;

    // Reconstruction constructor (for persistence/mapping)
    public ShortLink(Long id, ShortCode shortCode, OriginalUrl originalUrl, Instant createdAt, Instant expiresAt,
            boolean isEnabled, long clickCount, String deleteTokenHash) {
        this.id = id;
        this.shortCode = shortCode;
        this.originalUrl = originalUrl;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.isEnabled = isEnabled;
        this.clickCount = clickCount;
        this.deleteTokenHash = deleteTokenHash;
    }

    // Static factory for creation
    public static ShortLink create(Long id, ShortCode shortCode, OriginalUrl originalUrl, Instant expiresAt,
            String deleteTokenHash) {
        if (id == null) {
            throw new InvalidShortLinkException("ID cannot be null");
        }
        if (shortCode == null) {
            throw new InvalidShortLinkException("ShortCode cannot be null");
        }
        if (originalUrl == null) {
            throw new InvalidShortLinkException("OriginalUrl cannot be null");
        }
        // deleteTokenHash can be null? Probably enforce it?
        // Let's allow it to be null for now if logic allows, or enforce it.
        // TASK_BREAKDOWN 1.4 says fields include it.

        // Validate expiration is in future if present
        if (expiresAt != null && expiresAt.isBefore(Instant.now())) {
            throw new InvalidShortLinkException("Expiration time must be in the future");
        }

        return new ShortLink(
                id,
                shortCode,
                originalUrl,
                Instant.now(),
                expiresAt,
                true, // Enabled by default
                0L, // Zero clicks
                deleteTokenHash);
    }

    public void enable() {
        this.isEnabled = true;
    }

    public void disable() {
        this.isEnabled = false;
    }

    public void incrementClickCount() {
        this.clickCount++;
    }

    public boolean isExpired(Instant now) {
        return expiresAt != null && expiresAt.isBefore(now);
    }
}
