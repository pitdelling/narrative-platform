package com.narrativeplatform.shared.exceptions;

import org.springframework.http.HttpStatus;

public class InvalidStoryVoteUnitsException extends DomainException {
    private static final String ERROR_CODE = "invalid_story_vote_units";

    public InvalidStoryVoteUnitsException(final String message) {
        super(HttpStatus.BAD_REQUEST, ERROR_CODE, message);
    }
}
