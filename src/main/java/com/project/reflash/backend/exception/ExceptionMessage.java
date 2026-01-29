package com.project.reflash.backend.exception;

public enum ExceptionMessage {

    USER_DOES_NOT_EXIST("User does not exist"),
    VALIDATION_FAILED("Validation has failed"),
    ;

    final private String message;
    ExceptionMessage(String message) {
        this.message = message;
    }

    public String toString() {
        return message;
    }
}
