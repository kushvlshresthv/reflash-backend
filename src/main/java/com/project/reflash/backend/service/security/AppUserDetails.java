package com.project.reflash.backend.service.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class AppUserDetails implements UserDetails {
    private final Integer userId;
    private final String grade;
    private final String section;
    private final String roll;
    private final String password;

    public AppUserDetails(Integer userId, String grade,String section, String roll,  String password ) {
        this.userId = userId;
        this.grade = grade;
        this.section = section;
        this.roll = roll;
        this.password = password;
    }

    public Integer getUserId() {
        return userId;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        if(section != null && !section.isBlank())  {
            return grade + "_" + section + "_" + roll;
        }
        return grade + "_" + roll;
    }

}
