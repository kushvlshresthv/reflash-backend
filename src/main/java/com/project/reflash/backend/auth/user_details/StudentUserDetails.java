package com.project.reflash.backend.auth.user_details;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
public class StudentUserDetails implements UserDetails {
    private final Integer id;
    private final String firstName;
    private final String lastName;
    private final String academicYear;
    private final String grade;
    private final String section;
    private final String roll;
    private final String password;
    private final String role = "ROLE_STUDENT";

    public StudentUserDetails(Integer id, String firstName, String lastName, String academicYear, String grade, String section, String roll, String password) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.academicYear = academicYear;
        this.grade = grade;
        this.section = section;
        this.roll = roll;
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
        //NOTE: grade + section + roll are all mandatory fields in the database
        if (section.equalsIgnoreCase("NONE")) {
            return academicYear + "_" + grade + "_" + roll;
        }
        return academicYear + "_" + grade + "_" + section + "_" + roll;
    }
}
