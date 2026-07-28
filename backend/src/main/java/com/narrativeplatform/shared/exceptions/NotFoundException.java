package com.narrativeplatform.shared.exceptions;

import org.springframework.http.HttpStatus;

public class NotFoundException extends DomainException {
    public NotFoundException(final String message) {
        super(HttpStatus.NOT_FOUND, "not_found", message);
    }
}
