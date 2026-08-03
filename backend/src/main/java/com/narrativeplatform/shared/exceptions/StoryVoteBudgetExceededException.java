package com.narrativeplatform.shared.exceptions;

import org.springframework.http.HttpStatus;

public class StoryVoteBudgetExceededException extends DomainException {
    private static final String ERROR_CODE = "story_vote_budget_exceeded";

    public StoryVoteBudgetExceededException(final String message) {
        super(HttpStatus.CONFLICT, ERROR_CODE, message);
    }
}
