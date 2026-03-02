package com.project.reflash.backend.service;

import com.project.reflash.backend.dto.CourseStudent;
import com.project.reflash.backend.entity.Student;
import com.project.reflash.backend.repository.CourseRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class CourseService {
    private final CourseRepository courseRepository;
    CourseService(CourseRepository courseRepository) {
        this.courseRepository = courseRepository;
    }

    public List<CourseStudent> getCoursesOfStudent(Integer studentId) {
        //TODO: implement this properly
        return this.courseRepository.getCoursesOfStudent(studentId).stream().map(CourseStudent::new).toList();
    }

    public List<CourseStudent> getCoursesOfTeacher(Integer teacherId) {
        //TODO: implement this properly
        return this.courseRepository.getCoursesOfTeacher(teacherId).stream().map(CourseStudent::new).toList();
    }
}
