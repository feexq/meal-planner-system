package com.feex.mealplannersystem.dto.profile.statistic;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;

@Value
@Builder
public class NutritionHeatmapResponse {
    LocalDate date;
    Double totalCalories;
    Integer calorieTarget;
    Integer completionRate;
}
