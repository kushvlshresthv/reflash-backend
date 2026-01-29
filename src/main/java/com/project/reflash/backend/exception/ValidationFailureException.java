package com.project.reflash.backend.exception;

import lombok.Getter;
import org.springframework.validation.Errors;

@Getter
public class ValidationFailureException extends RuntimeException {
    final private String message;
    final private Errors errors;

    public ValidationFailureException(ExceptionMessage message, Errors errors) {
        this.message = message.toString();
        this.errors = errors;
    }
}
