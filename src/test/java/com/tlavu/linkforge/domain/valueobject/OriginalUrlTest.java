package com.tlavu.linkforge.domain.valueobject;

import com.tlavu.linkforge.domain.exception.InvalidUrlException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OriginalUrlTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "http://google.com",
            "https://example.com/path",
            "https://sub.domain.co.vn:8080/path?query=1&b=2#frag",
            "HTTP://GOOGLE.COM" // Case insensitive scheme/host
    })
    @DisplayName("Should create OriginalUrl from valid URL string")
    void shouldCreateOriginalUrlFromValidString(String validUrl) {
        OriginalUrl originalUrl = OriginalUrl.of(validUrl);
        assertThat(originalUrl).isNotNull();
        assertThat(originalUrl.url()).isEqualTo(validUrl.toLowerCase()); // Simple normalization check
    }

    @Test
    @DisplayName("Should normalize scheme and host to lowercase")
    void shouldNormalizeSchemeAndHost() {
        String input = "HTTP://GoOgLe.CoM/Path?Query=A";
        OriginalUrl originalUrl = OriginalUrl.of(input);

        // Scheme & host lowercased, path & query preserved case
        assertThat(originalUrl.url()).isEqualTo("http://google.com/Path?Query=A");
    }

    @Test
    @DisplayName("Should trim whitespace")
    void shouldTrimWhitespace() {
        String input = "  https://example.com  ";
        OriginalUrl originalUrl = OriginalUrl.of(input);
        assertThat(originalUrl.url()).isEqualTo("https://example.com");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "   ",
            "ftp://example.com",
            "javascript:alert(1)",
            "data:text/plain;base64,SGVsbG8=",
            "file:///etc/passwd",
            "example.com", // Missing scheme
            "http://", // Missing host
    })
    @DisplayName("Should throw InvalidUrlException for invalid URLs")
    void shouldThrowExceptionForInvalidUrls(String invalidUrl) {
        assertThatThrownBy(() -> OriginalUrl.of(invalidUrl))
                .isInstanceOf(InvalidUrlException.class);
    }

    @Test
    @DisplayName("Should throw exception if URL is too long")
    void shouldThrowExceptionIfTooLong() {
        String longUrl = "https://example.com/" + "a".repeat(2050);
        assertThatThrownBy(() -> OriginalUrl.of(longUrl))
                .isInstanceOf(InvalidUrlException.class)
                .hasMessageContaining("exceeds maximum length");
    }
}
