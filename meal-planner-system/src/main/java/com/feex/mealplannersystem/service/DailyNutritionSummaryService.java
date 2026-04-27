package com.feex.mealplannersystem.service;

import com.feex.mealplannersystem.dto.profile.statistic.WeeklyAveragesResponse;
import com.feex.mealplannersystem.repository.entity.auth.UserEntity;
import com.feex.mealplannersystem.repository.entity.mealplan.FoodLogEntity;
import com.feex.mealplannersystem.repository.entity.mealplan.MealPlanSlotEntity;
import com.feex.mealplannersystem.repository.entity.profile.DailyNutritionSummaryEntity;

import java.time.LocalDate;
import java.util.List;

public interface DailyNutritionSummaryService {
    DailyNutritionSummaryEntity recalculate(
            UserEntity user,
            LocalDate date,
            List<FoodLogEntity> foodLogs,
            List<MealPlanSlotEntity> slots,
            Integer calorieTarget
    );

    DailyNutritionSummaryEntity getOrEmpty(UserEntity user, LocalDate date);

    List<DailyNutritionSummaryEntity> getRange(Long userId, LocalDate from, LocalDate to);

    WeeklyAveragesResponse getWeeklyAverages(Long userId, LocalDate weekStart);
}
