package com.project.reflash.backend.service;

import com.project.reflash.backend.entity.Teacher;
import com.project.reflash.backend.exception.ExceptionMessage;
import com.project.reflash.backend.exception.UserDoesNotExistException;
import com.project.reflash.backend.repository.TeacherRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
public class TeacherService {
    private final TeacherRepository teacherRepository;

    TeacherService(TeacherRepository teacherRepository) {
        this.teacherRepository = teacherRepository;
    }

    /**
     *
     * @param username
     * @return teacher
     * @throws UserDoesNotExistException if teacher with the given username does not exist
     */
    public Teacher loadTeacher(String username) {
        if (username == null || username.isBlank()) {
            throw new UserDoesNotExistException(ExceptionMessage.INVALID_USERNAME);
        }

        Optional<Teacher> teacher = teacherRepository.findByUsername(username);

        if (teacher.isPresent()) {
            return teacher.get();
        } else {
            throw new UserDoesNotExistException(ExceptionMessage.USER_DOES_NOT_EXIST);
        }
    }
}
