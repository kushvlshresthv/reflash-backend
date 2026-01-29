package com.project.reflash.backend.utils;

import com.project.reflash.backend.exception.ExceptionMessage;
import com.project.reflash.backend.exception.ValidationFailureException;
import org.springframework.stereotype.Component;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Validator;

@Component
public class EntityValidator {
    private final Validator validator;
    EntityValidator(Validator validator) {
        this.validator = validator;
    }

    public void validate(Object object){
        BindingResult bindingResult = new BeanPropertyBindingResult(object, "object");
        validator.validate(object, bindingResult);
        if(bindingResult.hasErrors()) {
            throw new ValidationFailureException(ExceptionMessage.VALIDATION_FAILED, bindingResult);
        }
    }
}
