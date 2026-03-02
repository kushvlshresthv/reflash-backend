package com.project.reflash.backend.dto;

import com.project.reflash.backend.entity.Student;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StudentDto {
    private Integer id;
    private String firstName;
    private String lastName;

    public StudentDto(Student student) {
        this.id = student.getId();
        this.firstName = student.getFirstName();
        this.lastName = student.getLastName();
    }
}
