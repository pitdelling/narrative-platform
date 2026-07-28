package com.narrativeplatform.shared.exceptions;

import org.springframework.http.HttpStatus;

public class AiNotConfiguredException extends DomainException {
    private static final String ERROR_CODE = "ai_not_configured";
    private static final String ERROR_MESSAGE = "AI generation is not configured yet.";

    public AiNotConfiguredException() {
        super(HttpStatus.SERVICE_UNAVAILABLE, ERROR_CODE, ERROR_MESSAGE);
    }
}
