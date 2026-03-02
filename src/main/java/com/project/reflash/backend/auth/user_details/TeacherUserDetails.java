package com.project.reflash.backend.auth.user_details;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
public class TeacherUserDetails implements UserDetails {
    private final Integer id;
    private final String firstName;
    private final String lastName;
    private final String username;
    private final String role = "ROLE_TEACHER";
    private final String password;

    public TeacherUserDetails(Integer id, String firstName, String lastName,String username,  String password) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.username = username;
        this.password = password;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return this.username;
    }
}
