package com.tlavu.linkforge.infrastructure.generator;

import com.tlavu.linkforge.domain.valueobject.ShortCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class TsidShortCodeGeneratorTest {

    private final TsidShortCodeGenerator generator = new TsidShortCodeGenerator();

    @Test
    @DisplayName("Should generate valid ShortCode")
    void shouldGenerateValidShortCode() {
        ShortCode shortCode = generator.generate();

        assertThat(shortCode).isNotNull();
        assertThat(shortCode.code()).isNotEmpty();
        // TSID based Base62 is usually around 10-13 chars long
        assertThat(shortCode.code().length()).isGreaterThan(0);
    }

    @RepeatedTest(100)
    @DisplayName("Should generate unique codes")
    void shouldGenerateUniqueCodes() {
        Set<String> codes = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            ShortCode code = generator.generate();
            codes.add(code.code());
        }
        // Since TSID is time-sorted unique, local generation should be unique
        assertThat(codes).hasSize(100);
    }
}
