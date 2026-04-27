package com.feex.mealplannersystem.service;

import com.feex.mealplannersystem.dto.profile.statistic.NutritionHeatmapResponse;
import com.feex.mealplannersystem.dto.profile.statistic.TopRecipeResponse;

import java.time.LocalDate;
import java.util.List;

public interface UserStatisticsService {

    List<TopRecipeResponse> getTopRecipes(Long userId, int limit);

    List<NutritionHeatmapResponse> getNutritionHeatmap(Long userId, LocalDate from, LocalDate to);
}
