package com.jnrptt.eventos.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PasswordValidoValidator.class)
public @interface PasswordValido {
    String message() default PoliticaPassword.MENSAJE;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
