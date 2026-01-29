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
        Optional<AppUser> user =  appUserRepository.findByUsername(username);

        if(user.isPresent()) {
            return user.get();
        } else {
            throw new UserDoesNotExistException(ExceptionMessage.USER_DOES_NOT_EXIST);
        }
    }

//    /**
//     * This method saves the AppUser object if the username is available
//     *
//     * @param appUser the user data that is to be persisted
//     * @return savedUser
//     */
//    public AppUser saveNewUser(AppUser appUser) {
//        appUser.setUid(0);   //don't try to update the existing data
//        if(appUserRepository.existsByUsername(appUser.getUsername())) {
//            throw new UsernameAlreadyExistsException(ExceptionMessages.USERNAME_ALREADY_EXISTS.toString(), appUser.getUsername());
//        }
//        appUser.setPassword(passwordEncoder.encode(appUser.getPassword()));
//        return appUserRepository.save(appUser);
//    }
//
//
//    /**
//     * This method is used to update AppUser's firstName, lastName, email
//     *
//     * @param updatedUserData data to be updated
//     * @param currentUsername the username invoking this method
//     * @return updatedUser
//     *
//     * TODO: implement the feature to change the username as well
//     */
//
//    public AppUser updateUser(AppUser updatedUserData, String currentUsername) {
//        AppUser currentUser = loadUserByUsername(currentUsername);
//
//        //make sure that the new username is available
//        /*
//        if(updatedUserData.getUsername() != null && !updatedUserData.getUsername().isBlank() && !updatedUserData.getUsername().equals(currentUser.getUsername())) {
//            log.error("TODO: the feature to change the username has not been implemented as of now");
//            if(appUserRepository.existsByUsername(updatedUserData.getUsername())) {
//                throw new UsernameAlreadyExistsException(ExceptionMessages.USERNAME_ALREADY_EXISTS);
//            }
//        }
//        */
//
//
//        //check whether the password is being changed or not
//        if(updatedUserData.getPassword() != null) {
//            if(!passwordEncoder.matches(updatedUserData.getPassword(), currentUser.getPassword())) {
//                throw new PasswordChangeNotAllowedException(ExceptionMessages.ROUTE_UPDATE_USER_CANT_UPDATE_PASSWORD);
//            }
//        }
//
//        if(updatedUserData.getFirstName()!=null && updatedUserData.getFirstName().isBlank()) {
//            currentUser.setFirstName(updatedUserData.getFirstName());
//        }
//        if(updatedUserData.getLastName()!=null && updatedUserData.getLastName().isBlank()) {
//            currentUser.setLastName(updatedUserData.getLastName());
//        }
//        if(updatedUserData.getEmail()!=null && updatedUserData.getEmail().isBlank()) {
//            currentUser.setEmail(updatedUserData.getEmail());
//        }
//
//        //workaround so that the validation does not fail
//        currentUser.setConfirmPassword(currentUser.getPassword());
//
//        //performing validations for the AppUser object with the new updated data
//        BindingResult bindingResult = new BeanPropertyBindingResult(currentUser, "user");
//        validator.validate(currentUser, bindingResult);
//
//        if(bindingResult.hasErrors()) {
//            throw new ValidationFailureException(ExceptionMessages.VALIDATION_FAILED, bindingResult);
//        }
//
//        return appUserRepository.save(currentUser);
//    }
}
