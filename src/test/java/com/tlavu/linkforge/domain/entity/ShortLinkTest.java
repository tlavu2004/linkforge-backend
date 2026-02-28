package com.tlavu.linkforge.domain.entity;

import com.tlavu.linkforge.domain.valueobject.OriginalUrl;
import com.tlavu.linkforge.domain.valueobject.ShortCode;
import com.tlavu.linkforge.domain.exception.InvalidShortLinkException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ShortLinkTest {

    @Test
    @DisplayName("Should create valid ShortLink")
    void shouldCreateValidShortLink() {
        Long id = 12345L;
        ShortCode code = ShortCode.of("abc");
        OriginalUrl url = OriginalUrl.of("http://example.com");
        Instant expiresAt = Instant.now().plus(1, ChronoUnit.DAYS);

        ShortLink link = ShortLink.create(id, code, url, expiresAt, null, "hash");

        assertThat(link.getId()).isEqualTo(id);
        assertThat(link.getShortCode()).isEqualTo(code);
        assertThat(link.getOriginalUrl()).isEqualTo(url);
        assertThat(link.getCreatedAt()).isNotNull();
        assertThat(link.getExpiresAt()).isEqualTo(expiresAt);
        assertThat(link.getClickCount()).isZero();
    }

    @Test
    @DisplayName("Should throw exception if expiration is in the past")
    void shouldThrowExceptionIfExpirationIsPast() {
        Long id = 12345L;
        ShortCode code = ShortCode.of("abc");
        OriginalUrl url = OriginalUrl.of("http://example.com");
        Instant past = Instant.now().minus(1, ChronoUnit.DAYS);

        assertThatThrownBy(() -> ShortLink.create(id, code, url, past, null, "hash"))
                .isInstanceOf(InvalidShortLinkException.class)
                .hasMessageContaining("future");
    }

    @Test
    @DisplayName("Should check expiration correctly")
    void shouldCheckExpiration() {
        Long id = 12345L;
        ShortCode code = ShortCode.of("abc");
        OriginalUrl url = OriginalUrl.of("http://example.com");
        Instant expiresAt = Instant.now().plus(1, ChronoUnit.HOURS);

        ShortLink link = ShortLink.create(id, code, url, expiresAt, null, "hash");

        assertThat(link.isExpired(Instant.now())).isFalse();
        assertThat(link.isExpired(Instant.now().plus(2, ChronoUnit.HOURS))).isTrue();
    }

    @Test
    void validCreateWithExpiration() {
        Optional<Instant> expiration = Optional.of(Instant.now().plusSeconds(3600));
        ShortLink shortLink = ShortLink.create(
                1L,
                ShortCode.of("abc"),
                OriginalUrl.of("http://example.com"),
                expiration.orElse(null),
                null,
                "tokenHash");
        boolean expired = shortLink.isExpired(Instant.now());
        assertThat(expired).isFalse();
    }

    @Test
    void createWithNullId_throwsException() {
        assertThrows(InvalidShortLinkException.class,
                () -> ShortLink.create(null, ShortCode.of("abc"), OriginalUrl.of("http://example.com"), null, null,
                        "hash"));
    }

    @Test
    @DisplayName("Should increment click count")
    void shouldIncrementClickCount() {
        ShortLink link = ShortLink.create(1L, ShortCode.of("a"), OriginalUrl.of("http://c.com"), null, null, "hash");
        assertThat(link.getClickCount()).isZero();

        link.incrementClickCount();
        assertThat(link.getClickCount()).isEqualTo(1);
    }

}
