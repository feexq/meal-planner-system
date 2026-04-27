package com.feex.mealplannersystem.service;

import com.feex.mealplannersystem.dto.mealplan.FinalizedMealPlanDto;
import com.feex.mealplannersystem.mealplan.dto.plan.WeeklyMealPlanDto;
import com.feex.mealplannersystem.repository.entity.auth.UserEntity;
import com.feex.mealplannersystem.repository.entity.mealplan.MealPlanRecordEntity;

public interface MealPlanPersistenceService {
    MealPlanRecordEntity saveFinalizedPlan(
            UserEntity user,
            FinalizedMealPlanDto finalizedPlan,
            WeeklyMealPlanDto weeklyPlan);
}
