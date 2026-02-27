package com.project.reflash.backend.service;

import com.project.reflash.backend.entity.AppUser;
import com.project.reflash.backend.exception.ExceptionMessage;
import com.project.reflash.backend.exception.UserDoesNotExistException;
import com.project.reflash.backend.repository.AppUserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
public class AppUserService {

    private final AppUserRepository appUserRepository;

    AppUserService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }
    public AppUser loadUserByUsername(String username) {
        if(username == null) {
            throw new UserDoesNotExistException("Username is null");
        }

        long count = username.chars().filter(c -> c == '_').count();

        if(count < 1) {
            throw new UserDoesNotExistException("Invalid usrname");
        }

        String grade;
        String section;
        String roll;

        //TODO: check what the database returns if the section is null

        if(count ==1) {
            grade = username.split("_")[0];
            section = null;
            roll = username.split("_")[2];
        } else {
            grade = username.split("_")[0];
            section = username.split("_")[1];
            roll = username.split("_")[2];
        }

        //TODO verify that grade, section and roll only contains alphanumeric characters

        Optional<AppUser> user =  appUserRepository.findUserByGradeSectionAndRoll(grade, section, roll);

        if(user.isPresent()) {
            return user.get();
        } else {
            throw new UserDoesNotExistException(ExceptionMessage.USER_DOES_NOT_EXIST);
        }
    }
}
