package com.narrativeplatform.app.canon.services;

/**
 * Thrown by {@link CanonMapValidationService} when the AI's raw output fails structural or
 * semantic validation. Never surfaced through {@code GlobalExceptionHandler} — {@code
 * AiJobStateService} catches this internally and routes it through the normal job-failure path.
 */
public class CanonMapValidationException extends RuntimeException {
    public CanonMapValidationException(final String message) {
        super(message);
    }
}
