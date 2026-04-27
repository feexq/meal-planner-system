package com.feex.mealplannersystem.dto.mealplan.score;

import com.feex.mealplannersystem.mealplan.model.NutritionModel;
import com.feex.mealplannersystem.mealplan.model.RecipeModel;

public record ScoredAdditional(RecipeModel recipe, NutritionModel nutrition, int score) {}
