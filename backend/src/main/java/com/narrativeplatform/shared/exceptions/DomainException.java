package com.narrativeplatform.shared.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class DomainException extends RuntimeException {
    private final HttpStatus status;
    private final String code;

    public DomainException(final HttpStatus status, final String code, final String message) {
        super(message);
        this.status = status;
        this.code = code;
    }
}
