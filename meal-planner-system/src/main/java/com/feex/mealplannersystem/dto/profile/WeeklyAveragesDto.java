package com.feex.mealplannersystem.dto.profile;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class WeeklyAveragesDto {
    double avgCalories;
    double avgProteinG;
    double avgCarbsG;
    double avgFatG;
    double avgCompletionRate;
    int cleanDays;

    public static WeeklyAveragesDto empty() {
        return WeeklyAveragesDto.builder().build();
    }
}
