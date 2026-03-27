package com.feex.mealplannersystem.dto.auth;

import com.feex.mealplannersystem.dto.validation.ExtendedValidation;
import com.feex.mealplannersystem.dto.validation.annotation.ValidNoSpace;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
@AllArgsConstructor
public class LoginRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    @ValidNoSpace(groups = ExtendedValidation.class)
    String password;
}
