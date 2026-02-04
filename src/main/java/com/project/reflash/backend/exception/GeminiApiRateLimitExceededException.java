package com.project.reflash.backend.exception;

public class GeminiApiRateLimitExceededException extends RuntimeException {

    public GeminiApiRateLimitExceededException(String message) {
        super(message);
    }
}
