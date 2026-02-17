package com.tlavu.linkforge.domain.exception;

import com.tlavu.linkforge.shared.exception.DomainException;

public class InvalidUrlException extends DomainException {
    public InvalidUrlException(String message) {
        super(message);
    }
}
