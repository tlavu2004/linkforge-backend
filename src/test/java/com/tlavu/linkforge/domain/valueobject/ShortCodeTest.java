package com.tlavu.linkforge.domain.valueobject;

import com.tlavu.linkforge.domain.exception.InvalidShortCodeException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ShortCodeTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "abc", "123", "aBc123D", "XyZ", "007"
    })
    @DisplayName("Should create ShortCode from valid alphanumeric string")
    void shouldCreateShortCodeFromValidString(String validCode) {
        ShortCode shortCode = ShortCode.of(validCode);
        assertThat(shortCode).isNotNull();
        assertThat(shortCode.code()).isEqualTo(validCode);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "   " })
    @DisplayName("Should throw exception for null, empty or blank code")
    void shouldThrowExceptionForEmptyCode(String emptyCode) {
        assertThatThrownBy(() -> ShortCode.of(emptyCode))
                .isInstanceOf(InvalidShortCodeException.class)
                .hasMessageContaining("empty");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "abc_", "a-b", " abc", "abc ", "$%^", "ab.c", "user@name"
    })
    @DisplayName("Should throw exception for non-alphanumeric characters")
    void shouldThrowExceptionForInvalidCharacters(String invalidCode) {
        assertThatThrownBy(() -> ShortCode.of(invalidCode))
                .isInstanceOf(InvalidShortCodeException.class)
                .hasMessageContaining("alphanumeric");
    }

    @Test
    @DisplayName("Should throw exception if code is too long")
    void shouldThrowExceptionIfTooLong() {
        String longCode = "a".repeat(21);
        assertThatThrownBy(() -> ShortCode.of(longCode))
                .isInstanceOf(InvalidShortCodeException.class)
                .hasMessageContaining("exceeds maximum length");
    }
}
