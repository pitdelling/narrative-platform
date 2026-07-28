package com.narrativeplatform.shared.exceptions;

public class TurnExpiredException extends ConflictException {
    public TurnExpiredException(final String message) {
        super(message);
    }
}
