package com.project.reflash.backend.dto;

import com.project.reflash.backend.entity.Teacher;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class TeacherDto {
    private Integer id;
    private String firstName;
    private String lastName;

    public TeacherDto(Teacher teacher) {
        this.id = teacher.getId();
        this.firstName = teacher.getFirstName();
        this.lastName = teacher.getLastName();
    }
}
