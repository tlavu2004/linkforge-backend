package com.tlavu.linkforge.domain.valueobject;

import com.tlavu.linkforge.domain.exception.InvalidShortCodeException;

import java.util.regex.Pattern;

public record ShortCode(String code) {

    private static final Pattern ALPHANUMERIC_PATTERN = Pattern.compile("^[0-9a-zA-Z\\-_]+$");
    // TSID base62 length is ~13 chars. Let's allow margin up to 50 for future
    // custom aliases.
    private static final int MAX_LENGTH = 50;

    public ShortCode {
        if (code == null || code.trim().isEmpty()) {
            throw new InvalidShortCodeException("validation.short_code_empty");
        }
        if (code.length() > MAX_LENGTH) {
            throw new InvalidShortCodeException("validation.short_code_max_length");
        }
        if (!ALPHANUMERIC_PATTERN.matcher(code).matches()) {
            throw new InvalidShortCodeException("validation.short_code_alphanumeric");
        }
    }

    public static ShortCode of(String code) {
        return new ShortCode(code);
    }
}
