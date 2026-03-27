package com.feex.mealplannersystem.dto.validation.annotation;

import com.feex.mealplannersystem.dto.validation.validators.NoSpaceValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Constraint(validatedBy = NoSpaceValidator.class)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidNoSpace {
    String message() default "Title cannot contain spaces. Use '-' please";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
