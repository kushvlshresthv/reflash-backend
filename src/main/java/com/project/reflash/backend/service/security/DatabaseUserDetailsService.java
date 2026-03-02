package com.project.reflash.backend.service.security;

import com.project.reflash.backend.auth.user_details.StudentUserDetails;
import com.project.reflash.backend.auth.user_details.TeacherUserDetails;
import com.project.reflash.backend.entity.Enrollment;
import com.project.reflash.backend.entity.Student;
import com.project.reflash.backend.entity.Teacher;
import com.project.reflash.backend.exception.InvalidRoleException;
import com.project.reflash.backend.service.StudentService;
import com.project.reflash.backend.service.TeacherService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
public class DatabaseUserDetailsService implements UserDetailsService {
    private UserDetails userDetails;
    private final StudentService studentService;
    private final TeacherService teacherService;

    public DatabaseUserDetailsService(StudentService studentService, TeacherService teacherService) {
        this.studentService = studentService;
        this.teacherService = teacherService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();

        String role = request.getHeader("role");
        log.info("Loading user {} with role {}", username, role);

        if (role.trim().equalsIgnoreCase("STUDENT")) {
            Enrollment enrollment = studentService.loadEnrollment(username);
            Student student = enrollment.getStudent();
            userDetails = new StudentUserDetails(student.getId(), student.getFirstName(), student.getLastName(),
                    enrollment.getAcademicYear(), enrollment.getGrade(), enrollment.getSection(),
                    enrollment.getRoll(), student.getPassword());
        } else if (role.trim().equalsIgnoreCase("TEACHER")) {
            Teacher teacher = teacherService.loadTeacher(username);
            userDetails = new TeacherUserDetails(teacher.getId(), teacher.getFirstName(), teacher.getLastName(),
                    teacher.getUsername(), teacher.getPassword());
        } else {
            throw new InvalidRoleException();
        }
        return userDetails;
    }
}
