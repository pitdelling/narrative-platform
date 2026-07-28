package com.narrativeplatform.shared.exceptions;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.LinkedHashMap;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DomainException.class)
    ProblemDetail handleDomainException(final DomainException exception) {
        final var detail = ProblemDetail.forStatusAndDetail(exception.getStatus(), exception.getMessage());
        detail.setTitle(exception.getCode());
        detail.setType(URI.create("urn:narrative-platform:error:" + exception.getCode()));
        return detail;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidation(final MethodArgumentNotValidException exception) {
        final var errors = new LinkedHashMap<String, String>();
        for (final var error : exception.getBindingResult().getFieldErrors()) {
            final var fieldError = (FieldError) error;
            errors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }
        final var detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request validation failed.");
        detail.setTitle("validation_error");
        detail.setProperty("errors", errors);
        return detail;
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(final Exception exception) {
        log.error("Unhandled API exception.", exception);
        final var detail = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred."
        );
        detail.setTitle("internal_error");
        return detail;
    }
}
