package com.project.reflash.backend.controller;

import com.project.reflash.backend.response.ApiResponse;
import com.project.reflash.backend.response.ResponseMessage;
import com.project.reflash.backend.service.security.AppUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class LoginLogoutController {
    @GetMapping("/login")
    public ResponseEntity<ApiResponse> tryLogin(HttpSession session, @AuthenticationPrincipal AppUserDetails appUser) {
        System.out.println("Successfully logged in");
        System.out.println(appUser.getUserId());
        return new ResponseEntity<ApiResponse>(new ApiResponse(ResponseMessage.LOGIN_SUCCESSFUL), HttpStatus.OK);
    }

    @GetMapping("/api/logout")
    public ResponseEntity<ApiResponse> logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        if(authentication != null){
            //clear the SecurityContextHolder and the following code also invalidates the HttpSession
            new SecurityContextLogoutHandler().logout(request, response, authentication);
        }
        System.out.println("logging out:::");
        return ResponseEntity.ok().body(new ApiResponse(ResponseMessage.LOGOUT_SUCCESSFUL));
    }

    @GetMapping("/isAuthenticated")
    public ResponseEntity<ApiResponse> isAuthenticated(Authentication authentication) {

        if (authentication != null && !authentication.getName().equals("anonymous") && !authentication.getName().equals("anonymousUser")) {
            if (authentication.isAuthenticated()) {
                log.info("The user: {} is authenticated", authentication.getName());
                return ResponseEntity.ok(new ApiResponse("true"));
            }
        }
        return new ResponseEntity<ApiResponse>(new ApiResponse("false"), HttpStatus.UNAUTHORIZED);
    }
}
