package com.tlavu.linkforge.domain.entity;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@EqualsAndHashCode(of = "id")
@NoArgsConstructor(force = true, access = AccessLevel.PRIVATE)
public class RefreshToken {

    private final Long id;
    private final Long userId;
    private final String token;
    private final Instant expiryDate;

    public RefreshToken(Long id, Long userId, String token, Instant expiryDate) {
        this.id = id;
        this.userId = userId;
        this.token = token;
        this.expiryDate = expiryDate;
    }

    public boolean isExpired(Instant now) {
        return expiryDate.isBefore(now);
    }
}
