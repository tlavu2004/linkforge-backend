package com.tlavu.linkforge.domain.exception;

import com.tlavu.linkforge.shared.exception.DomainException;

public class InvalidShortLinkException extends DomainException {
    public InvalidShortLinkException(String message) {
        super(message);
    }
}
