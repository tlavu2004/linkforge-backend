package com.tlavu.linkforge.domain.exception;

public class ShortLinkExpiredException extends RuntimeException {
    public ShortLinkExpiredException(String message) {
        super(message);
    }
}
