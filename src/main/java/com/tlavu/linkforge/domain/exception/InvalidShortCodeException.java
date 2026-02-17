package com.tlavu.linkforge.domain.exception;

import com.tlavu.linkforge.shared.exception.DomainException;

public class InvalidShortCodeException extends DomainException {
    public InvalidShortCodeException(String message) {
        super(message);
    }
}
