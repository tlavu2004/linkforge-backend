package com.tlavu.linkforge.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "short_links")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ShortLinkJpaEntity {

    @Id
    private Long id;

    @Column(nullable = false, unique = true, length = 10)
    private String code;

    @Column(name = "original_url", nullable = false, columnDefinition = "TEXT")
    private String originalUrl;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "click_count", nullable = false)
    private Long clickCount;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "delete_token_hash", length = 64)
    private String deleteTokenHash;
}
