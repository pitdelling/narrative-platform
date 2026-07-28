package com.narrativeplatform.shared.exceptions;

import org.springframework.http.HttpStatus;

public class ConflictException extends DomainException {
    public ConflictException(final String message) {
        super(HttpStatus.CONFLICT, "conflict", message);
    }
}
