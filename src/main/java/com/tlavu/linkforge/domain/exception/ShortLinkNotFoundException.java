package com.tlavu.linkforge.domain.exception;

public class ShortLinkNotFoundException extends RuntimeException {
    public ShortLinkNotFoundException(String message) {
        super(message);
    }
}
