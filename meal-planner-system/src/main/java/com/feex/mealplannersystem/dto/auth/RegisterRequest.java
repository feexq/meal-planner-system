package com.feex.mealplannersystem.dto.auth;

import com.feex.mealplannersystem.dto.validation.ExtendedValidation;
import com.feex.mealplannersystem.dto.validation.annotation.ValidNoSpace;
import jakarta.validation.GroupSequence;
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
@GroupSequence({RegisterRequest.class, ExtendedValidation.class})
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    @ValidNoSpace(groups = ExtendedValidation.class)
    String password;

    @NotBlank(message = "First name is required")
    @Size(min = 2, message = "First name must be at least 2 characters")
    @Size(max = 99, message = "First name must not exceed 99 characters")
    String firstName;

    @NotBlank(message = "First name is required")
    @Size(min = 2, message = "Last name must be at least 2 characters")
    @Size(max = 99, message = "Last name must not exceed 99 characters")
    String lastName;
}
