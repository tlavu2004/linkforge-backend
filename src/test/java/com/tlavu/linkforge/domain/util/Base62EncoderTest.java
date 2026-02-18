package com.tlavu.linkforge.domain.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Base62EncoderTest {

    @ParameterizedTest
    @CsvSource({
            "0, 0",
            "1, 1",
            "10, A",
            "35, Z",
            "36, a",
            "61, z",
            "62, 10"
    })
    @DisplayName("Should encode values correctly")
    void shouldEncodeValuesCorrectly(long input, String expected) {
        String encoded = Base62Encoder.encode(input);
        assertThat(encoded).isEqualTo(expected);
    }

    @Test
    @DisplayName("Should throw exception for negative input")
    void shouldThrowExceptionForNegativeInput() {
        assertThatThrownBy(() -> Base62Encoder.encode(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Value must be non-negative");
    }
}
