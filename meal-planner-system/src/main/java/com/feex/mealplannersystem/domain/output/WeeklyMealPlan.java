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
public class WeeklyMealPlan {
    private String userId;
    private int dailyCalorieTarget;
    private int weeklyCalorieTarget;
    private double relaxationRate;
    private boolean lowCoverageWarning;
    private List<DayPlan> days;
}
