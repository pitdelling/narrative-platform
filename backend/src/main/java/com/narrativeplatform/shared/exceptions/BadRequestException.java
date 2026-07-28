package com.narrativeplatform.shared.exceptions;

import org.springframework.http.HttpStatus;

public class BadRequestException extends DomainException {
    public BadRequestException(final String message) {
        super(HttpStatus.BAD_REQUEST, "bad_request", message);
    }
}
