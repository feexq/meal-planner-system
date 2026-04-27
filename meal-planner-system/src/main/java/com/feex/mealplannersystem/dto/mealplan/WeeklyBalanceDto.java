package com.feex.mealplannersystem.dto.mealplan;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WeeklyBalanceDto {
    private int weeklyCalorieTarget;
    private double totalConsumed;
    private double remainingBudget;
    private int remainingDays;
    private double adjustedDailyTarget;
    private boolean overLimitWarning;
    private String adjustmentNote;
}
