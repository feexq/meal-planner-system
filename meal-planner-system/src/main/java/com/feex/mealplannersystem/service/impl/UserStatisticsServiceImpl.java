package com.feex.mealplannersystem.service.impl;

import com.feex.mealplannersystem.dto.profile.statistic.NutritionHeatmapResponse;
import com.feex.mealplannersystem.dto.profile.statistic.TopRecipeResponse;
import com.feex.mealplannersystem.repository.DailyNutritionSummaryRepository;
import com.feex.mealplannersystem.repository.MealPlanSlotRepository;
import com.feex.mealplannersystem.repository.entity.profile.DailyNutritionSummaryEntity;
import com.feex.mealplannersystem.repository.projection.TopRecipeProjection;
import com.feex.mealplannersystem.service.UserStatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserStatisticsServiceImpl implements UserStatisticsService {

    private final MealPlanSlotRepository mealPlanSlotRepository;
    private final DailyNutritionSummaryRepository dailyNutritionSummaryRepository;

    @Transactional(readOnly = true)
    public List<TopRecipeResponse> getTopRecipes(Long userId, int limit) {
        List<TopRecipeProjection> projections = mealPlanSlotRepository.findTopRecipesByUserId(
                userId, PageRequest.of(0, limit)
        );

        return projections.stream()
                .map(p -> TopRecipeResponse.builder()
                        .recipeId(p.getRecipeId())
                        .recipeName(p.getRecipeName())
                        .count(p.getCount())
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<NutritionHeatmapResponse> getNutritionHeatmap(Long userId, LocalDate from, LocalDate to) {
        List<DailyNutritionSummaryEntity> summaries = dailyNutritionSummaryRepository.findByUserIdAndDateRange(userId, from, to);
        return summaries.stream()
                .map(s -> NutritionHeatmapResponse.builder()
                        .date(s.getSummaryDate())
                        .totalCalories(s.getTotalCalories())
                        .calorieTarget(s.getCalorieTarget())
                        .completionRate(s.getCompletionRate())
                        .build())
                .toList();
    }
}
