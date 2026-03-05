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
    private long clickCount;
    private Long userId;
    private String deleteTokenHash;
    private String qrCode;

    // Reconstruction constructor (for persistence/mapping)
    public ShortLink(Long id, ShortCode shortCode, OriginalUrl originalUrl, Instant createdAt, Instant expiresAt,
            long clickCount, Long userId, String deleteTokenHash, String qrCode) {
        this.id = id;
        this.shortCode = shortCode;
        this.originalUrl = originalUrl;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.clickCount = clickCount;
        this.userId = userId;
        this.deleteTokenHash = deleteTokenHash;
        this.qrCode = qrCode;
    }

    // Static factory for creation
    public static ShortLink create(Long id, ShortCode shortCode, OriginalUrl originalUrl, Instant expiresAt,
            Long userId, String deleteTokenHash) {
        if (id == null) {
            throw new InvalidShortLinkException("ID cannot be null");
        }
        if (shortCode == null) {
            throw new InvalidShortLinkException("ShortCode cannot be null");
        }
        if (originalUrl == null) {
            throw new InvalidShortLinkException("OriginalUrl cannot be null");
        }

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
                0L, // Zero clicks
                userId,
                deleteTokenHash,
                null); // Initially no QR code
    }

    public void incrementClickCount() {
        this.clickCount++;
    }

    public boolean isExpired(Instant now) {
        return expiresAt != null && expiresAt.isBefore(now);
    }

    public void assignQrCode(String qrCode) {
        if (qrCode == null || qrCode.isBlank()) {
            throw new InvalidShortLinkException("QR Code data cannot be empty");
        }
        this.qrCode = qrCode;
    }

    public void deleteQrCode() {
        this.qrCode = null;
    }
}
