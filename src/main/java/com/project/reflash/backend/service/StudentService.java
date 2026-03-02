package com.project.reflash.backend.service;

import com.project.reflash.backend.entity.Enrollment;
import com.project.reflash.backend.entity.Student;
import com.project.reflash.backend.exception.ExceptionMessage;
import com.project.reflash.backend.exception.UserDoesNotExistException;
import com.project.reflash.backend.repository.StudentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
public class StudentService {

    private final StudentRepository studentRepository;

    StudentService(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    /**
     *
     * @param username
     * @return Enrollment
     * @throws UserDoesNotExistException if enrollment with year + grade + section + roll does not exist
     */
    public Enrollment loadEnrollment(String username) {
        if(username == null || username.isBlank()) {
            throw new UserDoesNotExistException(ExceptionMessage.INVALID_USERNAME);
        }

        long count = username.chars().filter(c -> c == '_').count();

        if(count < 1) {
            throw new UserDoesNotExistException(ExceptionMessage.INVALID_USERNAME);
        }

        String year;
        String grade;
        String section;
        String roll;


        if(count == 2) {
            year = username.split("_")[0];
            grade = username.split("_")[1];
            section = "NONE";
            roll = username.split("_")[2];
        } else if(count == 3) {
            year = username.split("_")[0];
            grade = username.split("_")[1];
            section = username.split("_")[2];
            roll = username.split("_")[3];
        } else {
            throw new UserDoesNotExistException(ExceptionMessage.INVALID_USERNAME);
        }

        Optional<Enrollment> user =  studentRepository.findByGradeSectionRollAndYear(year, grade, section, roll);

        if(user.isPresent()) {
            return user.get();
        } else {
            throw new UserDoesNotExistException(ExceptionMessage.USER_DOES_NOT_EXIST);
        }
    }
}
