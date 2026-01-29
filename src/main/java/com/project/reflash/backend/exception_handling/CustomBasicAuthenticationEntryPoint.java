package com.project.reflash.backend.exception_handling;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.reflash.backend.response.ApiResponse;
import com.project.reflash.backend.response.ResponseMessage;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;

@Slf4j
public class CustomBasicAuthenticationEntryPoint implements AuthenticationEntryPoint {
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        String requestedUri = request.getRequestURI();
        ApiResponse errorResponse = new ApiResponse();

        errorResponse.setMessage(ResponseMessage.AUTHENTICATION_FAILED.toString() + ": " + authException.getMessage());

        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(response.getWriter(), errorResponse);

        log.error("{} for {} : {}", ResponseMessage.AUTHENTICATION_FAILED.toString(), requestedUri , authException.getMessage());
    }
}
