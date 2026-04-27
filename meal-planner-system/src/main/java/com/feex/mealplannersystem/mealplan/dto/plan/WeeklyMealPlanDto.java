package com.feex.mealplannersystem.mealplan.dto.plan;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class WeeklyMealPlanDto {
    private String userId;
    private int dailyCalorieTarget;
    private int weeklyCalorieTarget;
    private double relaxationRate;
    private boolean lowCoverageWarning;
    private List<DayPlanDto> days;
}
