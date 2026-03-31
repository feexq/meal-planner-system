package com.feex.mealplannersystem.mealplan.service;

import com.feex.mealplannersystem.mealplan.dto.FilterDebugDto;
import com.feex.mealplannersystem.mealplan.mapper.IngredientClassificationAdapter;
import com.feex.mealplannersystem.mealplan.mapper.RecipeDataAdapter;
import com.feex.mealplannersystem.mealplan.mapper.UserProfileAdapter;
import com.feex.mealplannersystem.mealplan.model.UserProfileModel;
import com.feex.mealplannersystem.mealplan.service.filter.RecipeFilterService;
import com.feex.mealplannersystem.repository.UserPreferenceRepository;
import com.feex.mealplannersystem.repository.entity.preference.UserPreferenceEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class MealPlanDebugService {

    private final UserPreferenceRepository          userPreferenceRepository;
    private final UserProfileAdapter                userProfileAdapter;
    private final RecipeDataAdapter                 recipeDataAdapter;
    private final IngredientClassificationAdapter   classificationAdapter;
    private final RecipeFilterService               recipeFilter;

    @Transactional(readOnly = true)
    public FilterDebugDto debugFilter(String userEmail, String slotType) {
        UserPreferenceEntity prefs = userPreferenceRepository
                .findByUserEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userEmail));

        UserProfileModel user = userProfileAdapter.toModel(prefs);

        RecipeDataAdapter.RecipeDataContext data = recipeDataAdapter.buildContext();
        IngredientClassificationAdapter.ClassificationContext classification =
                classificationAdapter.buildContext();

        RecipeFilterService.FilterResult result =
                recipeFilter.filterRecipes(data, classification, user, slotType);

        Map<String, Integer> eliminations = result.eliminationCounts();
        int total = data.getRecipes().size();
        int valid = result.recipes().size();

        String topCause = eliminations.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(e -> e.getKey() + " (" + e.getValue() + " recipes)")
                .orElse("none");

        log.info("=== Filter Debug: user={} slot={} ===", userEmail, slotType);
        log.info("  Total recipes in DB : {}", total);
        log.info("  Valid after filtering: {}", valid);
        log.info("  Eliminations:");
        eliminations.forEach((k, v) -> {
            if (v > 0) log.info("    {:30s}: {}", k, v);
        });
        log.info("  Top cause: {}", topCause);

        return FilterDebugDto.builder()
                .userId(user.getUserId())
                .slotType(slotType)
                .eliminations(eliminations)
                .validRecipes(valid)
                .totalRecipes(total)
                .topEliminationCause(topCause)
                .build();
    }

    @Transactional(readOnly = true)
    public Map<String, FilterDebugDto> debugAllSlots(String userEmail) {
        List<String> slots = List.of("breakfast", "lunch", "dinner", "snack", "all");
        Map<String, FilterDebugDto> results = new LinkedHashMap<>();

        UserPreferenceEntity prefs = userPreferenceRepository
                .findByUserEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userEmail));

        UserProfileModel user = userProfileAdapter.toModel(prefs);

        RecipeDataAdapter.RecipeDataContext data = recipeDataAdapter.buildContext();
        IngredientClassificationAdapter.ClassificationContext classification =
                classificationAdapter.buildContext();

        int total = data.getRecipes().size();

        log.info("=== Full Filter Debug: user={} totalRecipes={} ===", userEmail, total);

        for (String slot : slots) {
            RecipeFilterService.FilterResult result =
                    recipeFilter.filterRecipes(data, classification, user, slot);

            Map<String, Integer> eliminations = result.eliminationCounts();
            int valid = result.recipes().size();

            String topCause = eliminations.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(e -> e.getKey() + " (" + e.getValue() + ")")
                    .orElse("none");

            log.info("  [{}] valid={} | top_cause={}", slot, valid, topCause);

            results.put(slot, FilterDebugDto.builder()
                    .userId(user.getUserId())
                    .slotType(slot)
                    .eliminations(eliminations)
                    .validRecipes(valid)
                    .totalRecipes(total)
                    .topEliminationCause(topCause)
                    .build());
        }

        return results;
    }
}
