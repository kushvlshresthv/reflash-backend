package com.project.reflash.backend.response;

public enum ResponseMessage {
    LOGIN_SUCCESSFUL("Login successful"),
    LOGOUT_SUCCESSFUL("Logout successful"),
    AUTHENTICATION_FAILED("Authentication Failed"),
    ACCESS_DENIED("Access Denied"),
    SUCCESS("Success"),
    ;
    private final String message;
    ResponseMessage(String message) {
        this.message = message;
    }

    public String toString() {
        return message;
    }
}
