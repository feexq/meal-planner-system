package com.feex.mealplannersystem.service;

import com.feex.mealplannersystem.dto.mealplan.WeeklyBalanceDto;
import com.feex.mealplannersystem.dto.mealplan.response.AdaptedPlanResponse;
import com.feex.mealplannersystem.repository.entity.mealplan.MealPlanRecordEntity;

public interface WeeklyBalanceService {

    void applyMinimumSafeLimits(MealPlanRecordEntity plan, int currentDay);

    AdaptedPlanResponse recalculate(MealPlanRecordEntity plan, int currentDay);

    WeeklyBalanceDto buildBalance(MealPlanRecordEntity plan, int currentDay);
}
