package com.feex.mealplannersystem.service;

import com.feex.mealplannersystem.dto.mealplan.request.LogFoodRequest;
import com.feex.mealplannersystem.dto.mealplan.request.MarkSlotEatenRequest;
import com.feex.mealplannersystem.dto.mealplan.response.AdaptedPlanResponse;
import com.feex.mealplannersystem.dto.mealplan.response.LogFoodResponse;
import com.feex.mealplannersystem.dto.mealplan.status.PlanStatusDto;

public interface FoodLogService {
    LogFoodResponse logFood(Long userId, LogFoodRequest request);
    AdaptedPlanResponse markSlotEaten(Long userId, MarkSlotEatenRequest request);
    PlanStatusDto getPlanStatus(Long userId);
}
