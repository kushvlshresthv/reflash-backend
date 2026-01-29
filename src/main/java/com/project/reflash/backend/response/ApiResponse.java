package com.project.reflash.backend.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@RequiredArgsConstructor
public class ApiResponse {
    String message;
    Object mainBody;

    public ApiResponse(Object mainBody) {
        this.mainBody = mainBody;
    }

    public ApiResponse(String message) {
        this.message = message;
    }

    public ApiResponse(ResponseMessage responseMessage) {
        this.message = responseMessage.toString();
    }

    public ApiResponse(ResponseMessage responseMessage, Object mainBody) {
        this.message = responseMessage.toString();
        this.mainBody = mainBody;
    }
}
