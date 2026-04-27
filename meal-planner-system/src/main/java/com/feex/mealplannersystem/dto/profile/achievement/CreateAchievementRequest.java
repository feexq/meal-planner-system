package com.feex.mealplannersystem.dto.profile.achievement;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CreateAchievementRequest {

    @NotBlank(message = "Key is required")
    String key;

    @NotBlank(message = "Title is required")
    String title;

    @NotBlank(message = "Description is required")
    String description;

    String iconUrl;

    @NotNull(message = "Target value is required")
    @Positive(message = "Target value must be positive")
    Integer targetValue;

    @NotBlank(message = "Type is required")
    String type;
}
