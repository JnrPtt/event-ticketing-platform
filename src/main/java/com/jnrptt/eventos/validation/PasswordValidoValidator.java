package com.jnrptt.eventos.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordValidoValidator implements ConstraintValidator<PasswordValido, String> {

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        return password == null || PoliticaPassword.cumple(password);
    }
}
