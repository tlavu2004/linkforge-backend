package com.tlavu.linkforge.domain.valueobject;

import com.tlavu.linkforge.domain.exception.InvalidShortCodeException;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

public record ShortCode(String code) {

    private static final Pattern ALPHANUMERIC_PATTERN = Pattern.compile("^[0-9a-zA-Z]+$");
    // TSID base62 length is ~13 chars. Let's allow margin up to 20 for future
    // custom aliases.
    private static final int MAX_LENGTH = 20;

    public ShortCode {
        if (!StringUtils.hasText(code)) {
            throw new InvalidShortCodeException("Short code cannot be empty");
        }
        if (code.length() > MAX_LENGTH) {
            throw new InvalidShortCodeException("Short code exceeds maximum length of " + MAX_LENGTH);
        }
        if (!ALPHANUMERIC_PATTERN.matcher(code).matches()) {
            throw new InvalidShortCodeException("Short code must be alphanumeric (0-9, a-z, A-Z)");
        }
    }

    public static ShortCode of(String code) {
        return new ShortCode(code);
    }
}
