package com.narrativeplatform.shared.exceptions;

import org.springframework.http.HttpStatus;

public class StoryNotCompletedException extends DomainException {
    private static final String ERROR_CODE = "story_not_completed";

    public StoryNotCompletedException(final String message) {
        super(HttpStatus.BAD_REQUEST, ERROR_CODE, message);
    }
}
