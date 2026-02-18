package com.tlavu.linkforge.shared.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Base62Test {

    @Test
    @DisplayName("Should encode and decode roundtrip correctly")
    void shouldEncodeAndDecodeRoundtrip() {
        long original = 123456789L;
        String encoded = Base62.encode(original);
        long decoded = Base62.decode(encoded);

        assertThat(decoded).isEqualTo(original);
    }

    @Test
    @DisplayName("Should handle 0")
    void shouldHandleZero() {
        assertThat(Base62.encode(0)).isEqualTo("0");
        assertThat(Base62.decode("0")).isEqualTo(0);
    }

    @Test
    @DisplayName("Should handle Long.MAX_VALUE")
    void shouldHandleMaxValue() {
        long max = Long.MAX_VALUE;
        String encoded = Base62.encode(max);
        long decoded = Base62.decode(encoded);

        assertThat(decoded).isEqualTo(max);
    }

    @Test
    @DisplayName("Should throw exception for negative value")
    void shouldThrowExceptionForNegativeValue() {
        assertThatThrownBy(() -> Base62.encode(-1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should throw exception for invalid characters")
    void shouldThrowExceptionForInvalidCharacters() {
        assertThatThrownBy(() -> Base62.decode("abc_"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
