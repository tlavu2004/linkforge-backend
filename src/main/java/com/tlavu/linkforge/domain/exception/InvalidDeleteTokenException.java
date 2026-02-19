package com.tlavu.linkforge.domain.exception;

public class InvalidDeleteTokenException extends RuntimeException {
    public InvalidDeleteTokenException(String message) {
        super(message);
    }
}
