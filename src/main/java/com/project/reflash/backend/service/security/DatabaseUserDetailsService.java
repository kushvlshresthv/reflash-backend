package com.project.reflash.backend.service.security;

import com.project.reflash.backend.entity.AppUser;
import com.project.reflash.backend.exception.ExceptionMessage;
import com.project.reflash.backend.exception.UserDoesNotExistException;
import com.project.reflash.backend.service.AppUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@Slf4j
public class DatabaseUserDetailsService implements UserDetailsService {
    AppUserService appUserService;

    public DatabaseUserDetailsService(AppUserService appUserService) {
        this.appUserService = appUserService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        try {
            AppUser registeredUser = appUserService.loadUserByUsername(username);
            log.info("UserDetailsService invoked by: {}", username);

            if (registeredUser != null) {
                UserDetails user = new AppUserDetails(registeredUser.getUid(), registeredUser.getUsername(), registeredUser.getPassword());
                return user;
            } else {
                throw new UserDoesNotExistException(ExceptionMessage.USER_DOES_NOT_EXIST);
            }
        } catch (UserDoesNotExistException e) {
            throw new UsernameNotFoundException("@ " + username + "username not found");
        }
    }
}
