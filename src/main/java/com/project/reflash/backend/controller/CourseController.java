package com.project.reflash.backend.controller;


import com.project.reflash.backend.auth.user_details.StudentUserDetails;
import com.project.reflash.backend.auth.user_details.TeacherUserDetails;
import com.project.reflash.backend.dto.CourseStudent;
import com.project.reflash.backend.response.ApiResponse;
import com.project.reflash.backend.service.CourseService;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Slf4j
@RequestMapping("/api")
public class CourseController {

    CourseService courseService;

    CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/student/courses")
    public ResponseEntity<ApiResponse> getStudentCourses(HttpSession session, @AuthenticationPrincipal StudentUserDetails student) {
        List<CourseStudent> courses = courseService.getCoursesOfStudent(student.getId());
        return new ResponseEntity<ApiResponse>(new ApiResponse(courses), HttpStatus.OK);
    }


    @PreAuthorize("hasRole('TEACHER')")
    @GetMapping("/teacher/courses")
    public ResponseEntity<ApiResponse> getTeacherCourses(HttpSession session, @AuthenticationPrincipal TeacherUserDetails teacher) {
        List<CourseStudent> courses = courseService.getCoursesOfTeacher(teacher.getId());
        return new ResponseEntity<ApiResponse>(new ApiResponse(courses), HttpStatus.OK);
    }
}
