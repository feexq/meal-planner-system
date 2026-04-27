package com.feex.mealplannersystem.dto.profile.statistic;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class WeeklyAveragesResponse {
    double avgCalories;
    double avgProteinG;
    double avgCarbsG;
    double avgFatG;
    double avgCompletionRate;
    int cleanDays;

    public static WeeklyAveragesResponse empty() {
        return WeeklyAveragesResponse.builder().build();
    }
}
