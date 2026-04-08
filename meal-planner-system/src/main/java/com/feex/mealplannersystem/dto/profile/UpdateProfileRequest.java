package com.feex.mealplannersystem.dto.profile;

import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateProfileRequest {
    private String timezone;
    private Double targetWeightKg;
    private LocalDate dateOfBirth;
    private String bio;
}
