package com.project.reflash.backend.exception;

public class GeminiResponseParseFailureException extends RuntimeException {
    public GeminiResponseParseFailureException(String message) {
        super(message);
    }
}
