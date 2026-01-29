package com.project.reflash.backend.exception_handling;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.reflash.backend.response.ApiResponse;
import com.project.reflash.backend.response.ResponseMessage;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;

@Slf4j
//handles instances when a route that does not exists is invoked
public class CustomAccessDeniedHandler implements AccessDeniedHandler {
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException, ServletException {
        response.setContentType("application/json;charset=utf-8");
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);

        String requestedUri = request.getRequestURI();

        ApiResponse errorResponse = new ApiResponse();
        errorResponse.setMessage(ResponseMessage.ACCESS_DENIED +": " + accessDeniedException.getMessage());

        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(response.getWriter(), errorResponse);

        log.error("{} for {}: {} ", ResponseMessage.ACCESS_DENIED , requestedUri ,accessDeniedException.getMessage());
    }
}
