package com.feex.mealplannersystem.service.impl;

import com.feex.mealplannersystem.dto.mealplan.score.AdditionalRecipeDto;
import com.feex.mealplannersystem.dto.mealplan.score.ScoredAdditional;
import com.feex.mealplannersystem.mealplan.dto.FilterResult;
import com.feex.mealplannersystem.mealplan.mapper.context.ClassificationContext;
import com.feex.mealplannersystem.mealplan.mapper.context.RecipeDataContext;
import com.feex.mealplannersystem.mealplan.model.NutritionModel;
import com.feex.mealplannersystem.mealplan.model.RecipeModel;
import com.feex.mealplannersystem.mealplan.model.UserProfileModel;
import com.feex.mealplannersystem.mealplan.service.filter.RecipeFilterService;
import com.feex.mealplannersystem.service.AdditionalRecipeService;
import com.feex.mealplannersystem.service.mapper.AdditionalRecipeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdditionalRecipeServiceImpl implements AdditionalRecipeService {

    private final RecipeFilterService recipeFilter;
    private final AdditionalRecipeMapper additionalRecipeMapper;

    private static final double MAX_CALORIES    = 250.0;
    private static final int    PER_CATEGORY    = 5;

    private static final Set<String> SIDE_MEAL_TYPES = Set.of(
            "snack", "dessert", "drink", "sauce_or_condiment", "breakfast", "lunch", "dinner"
    );

    private static final List<String> STANDALONE_KEYWORDS = List.of(
            "stew", "roast", "casserole", "lasagna", "curry",
            "steak", "meatloaf", "whole", "stuffed", "baked chicken",
            "pot roast", "slow cooker"
    );

    public List<AdditionalRecipeDto> getAdditionalRecipes(
            UserProfileModel user,
            RecipeDataContext data,
            ClassificationContext classification) {

        FilterResult filtered =
                recipeFilter.filterRecipes(data, classification, user, "all");

        List<ScoredAdditional> candidates = new ArrayList<>();

        for (RecipeModel recipe : filtered.recipes()) {
            NutritionModel nutrition = data.getNutrition(recipe.getId());
            if (nutrition == null) continue;

            double cals = nutrition.getCalories();
            if (cals <= 0 || cals > MAX_CALORIES) continue;

            if (isStandalone(recipe)) continue;

            int score = scoreAdditional(recipe, nutrition);
            candidates.add(new ScoredAdditional(recipe, nutrition, score));
        }

        candidates.sort((a, b) -> Integer.compare(b.score(), a.score()));

        return pickDiverse(candidates, PER_CATEGORY);
    }

    private int scoreAdditional(RecipeModel recipe, NutritionModel nutrition) {
        int score = 0;

        String mt = recipe.getMealType() != null ? recipe.getMealType().toLowerCase() : "";
        if (SIDE_MEAL_TYPES.contains(mt)) score += 20;

        if (nutrition.getCalories() < 100) score += 15;
        else if (nutrition.getCalories() < 150) score += 8;

        if (nutrition.getProteinG() >= 8) score += 10;
        else if (nutrition.getProteinG() >= 4) score += 5;

        boolean hasRelevantTag = recipe.getTags().stream().anyMatch(t ->
                t.contains("low-cal") || t.contains("light") ||
                        t.contains("healthy") || t.contains("drink") ||
                        t.contains("beverage") || t.contains("smoothie") ||
                        t.contains("snack") || t.contains("yogurt") ||
                        t.contains("fruit") || t.contains("dairy") ||
                        t.contains("salad") || t.contains("soup"));
        if (hasRelevantTag) score += 10;

        return score;
    }

    private boolean isStandalone(RecipeModel recipe) {
        String name = recipe.getName() != null ? recipe.getName().toLowerCase() : "";
        return STANDALONE_KEYWORDS.stream().anyMatch(name::contains);
    }

    private List<AdditionalRecipeDto> pickDiverse(
            List<ScoredAdditional> sorted, int perCategory) {

        Map<String, List<ScoredAdditional>> byType = sorted.stream()
                .collect(Collectors.groupingBy(s ->
                        s.recipe().getMealType() != null
                                ? s.recipe().getMealType().toLowerCase()
                                : "other"));

        List<AdditionalRecipeDto> result = new ArrayList<>();
        Set<Long> added = new HashSet<>();
        Random random = new Random();

        for (Map.Entry<String, List<ScoredAdditional>> entry : byType.entrySet()) {
            List<ScoredAdditional> group = entry.getValue();

            int poolSize = Math.min(20, group.size());
            List<ScoredAdditional> topPool = new ArrayList<>(group.subList(0, poolSize));

            Collections.shuffle(topPool, random);

            int taken = 0;
            for (ScoredAdditional item : topPool) {
                if (taken >= perCategory) break;
                if (added.add(item.recipe().getId())) {
                    result.add(additionalRecipeMapper.toDto(item));
                    taken++;
                }
            }
        }

        return result;
    }

    public List<AdditionalRecipeDto> getAdditionalRecipesByTarget(
            UserProfileModel user,
            RecipeDataContext data,
            ClassificationContext classification,
            double targetCalories) {

        FilterResult filtered =
                recipeFilter.filterRecipes(data, classification, user, "all");

        List<ScoredAdditional> candidates = new ArrayList<>();

        double margin = Math.max(15.0, targetCalories * 0.15);
        double minCalAllowed = targetCalories - margin;
        double maxCalAllowed = targetCalories + margin;

        for (RecipeModel recipe : filtered.recipes()) {
            NutritionModel nutrition = data.getNutrition(recipe.getId());
            if (nutrition == null) continue;

            double cals = nutrition.getCalories();

            if (cals < minCalAllowed || cals > maxCalAllowed) continue;

            if (isStandalone(recipe)) continue;

            int score = scoreAdditional(recipe, nutrition);

            double diff = Math.abs(cals - targetCalories);
            if (diff <= 5.0) score += 15;

            candidates.add(new ScoredAdditional(recipe, nutrition, score));
        }

        candidates.sort((a, b) -> Integer.compare(b.score(), a.score()));

        return pickDiverse(candidates, PER_CATEGORY);
    }
}