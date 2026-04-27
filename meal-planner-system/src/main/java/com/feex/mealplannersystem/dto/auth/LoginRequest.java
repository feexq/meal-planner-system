package com.feex.mealplannersystem.dto.auth;

import com.feex.mealplannersystem.dto.validation.ExtendedValidation;
import com.feex.mealplannersystem.dto.validation.annotation.ValidNoSpace;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,
        @ValidNoSpace(groups = ExtendedValidation.class)
        @NotBlank(message = "Password is required")
        @Size(min = 6, message = "Password must be at least 6 characters")
        String password) {

}
