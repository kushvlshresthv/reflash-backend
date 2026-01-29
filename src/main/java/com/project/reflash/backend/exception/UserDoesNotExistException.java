package com.project.reflash.backend.exception;

public class UserDoesNotExistException extends RuntimeException {
    public UserDoesNotExistException(String message) {
        super(message);
    }

    public UserDoesNotExistException(ExceptionMessage message) {
        super(message.toString());
    }
}
