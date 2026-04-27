package com.feex.mealplannersystem.mealplan.dto.plan;

import com.feex.mealplannersystem.mealplan.dto.scoring.EstimatedDailyMacrosDto;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class DayPlanDto {
    private int day;
    private int dailyCalorieTarget;
    private EstimatedDailyMacrosDto estimatedDailyMacros;
    private List<MealSlotDto> slots;
    private double relaxationRate;
}
