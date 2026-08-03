package com.narrativeplatform.shared.exceptions;

import org.springframework.http.HttpStatus;

public class StoryVoteConcurrencyConflictException extends DomainException {
    private static final String ERROR_CODE = "story_vote_concurrency_conflict";
    private static final String ERROR_MESSAGE = "Please try voting again.";

    public StoryVoteConcurrencyConflictException() {
        super(HttpStatus.CONFLICT, ERROR_CODE, ERROR_MESSAGE);
    }
}
