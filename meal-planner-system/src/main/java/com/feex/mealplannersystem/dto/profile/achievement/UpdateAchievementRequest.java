package com.feex.mealplannersystem.dto.profile.achievement;

import jakarta.validation.constraints.Positive;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class UpdateAchievementRequest {
    String title;
    String description;
    String iconUrl;

    @Positive
    Integer targetValue;
}
