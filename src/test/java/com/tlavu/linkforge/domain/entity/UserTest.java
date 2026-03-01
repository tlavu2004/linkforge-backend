package com.tlavu.linkforge.domain.entity;

import com.tlavu.linkforge.domain.exception.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserTest {

    @Test
    @DisplayName("Should create valid User")
    void shouldCreateValidUser() {
        User user = User.create(1L, "Test User", "test@example.com", "hash", Role.USER);

        assertThat(user.getId()).isEqualTo(1L);
        assertThat(user.getEmail()).isEqualTo("test@example.com");
        assertThat(user.getPasswordHash()).isEqualTo("hash");
        assertThat(user.getRole()).isEqualTo(Role.USER);
        assertThat(user.isVip()).isFalse();
        assertThat(user.getVipExpiresAt()).isNull();
        assertThat(user.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should throw exception if email is empty")
    void shouldThrowExceptionIfEmailIsEmpty() {
        assertThatThrownBy(() -> User.create(1L, "Test User", "", "hash", Role.USER))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("email");
    }

    @Test
    @DisplayName("Should grant and revoke lifetime VIP")
    void shouldGrantAndRevokeLifetimeVip() {
        User user = User.create(1L, "Test User", "test@example.com", "hash", Role.USER);

        user.grantLifetimeVip();
        assertThat(user.isVip()).isTrue();
        assertThat(user.getVipExpiresAt()).isNull();
        assertThat(user.isVipActive(Instant.now())).isTrue();

        user.revokeVip();
        assertThat(user.isVip()).isFalse();
        assertThat(user.getVipExpiresAt()).isNull();
        assertThat(user.isVipActive(Instant.now())).isFalse();
    }

    @Test
    @DisplayName("Should grant temporary VIP and check expiration")
    void shouldGrantTemporaryVipAndCheckExpiration() {
        User user = User.create(1L, "Test User", "test@example.com", "hash", Role.USER);
        Instant future = Instant.now().plus(1, ChronoUnit.DAYS);
        Instant past = Instant.now().minus(1, ChronoUnit.DAYS);

        // Grant temporary
        user.grantTemporaryVip(future);
        assertThat(user.isVip()).isTrue();
        assertThat(user.getVipExpiresAt()).isEqualTo(future);

        // Check active right now
        assertThat(user.isVipActive(Instant.now())).isTrue();

        // Fast forward past expiration
        assertThat(user.isVipActive(Instant.now().plus(2, ChronoUnit.DAYS))).isFalse();

        // Expired date constraint on grant
        assertThatThrownBy(() -> user.grantTemporaryVip(past))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("future");
    }
}
