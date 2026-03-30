package com.feex.mealplannersystem.domain.output;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class DayPlan {
    private int day;
    private int dailyCalorieTarget;
    private EstimatedDailyMacros estimatedDailyMacros;
    private List<MealSlot> slots;
    private double relaxationRate;
}
