package com.project.reflash.backend.dto;

import com.project.reflash.backend.entity.Course;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CourseStudent {
    private Integer id;
    private String courseName;

    public CourseStudent(Course course) {
        this.id = course.getId();
        this.courseName = course.getCourseName();
    }
}
