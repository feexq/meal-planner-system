package com.feex.mealplannersystem.mealplan.mapper.context;

import com.feex.mealplannersystem.mealplan.model.NutritionModel;
import com.feex.mealplannersystem.mealplan.model.RecipeModel;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class RecipeDataContext {
    private final List<RecipeModel> recipes;
    private final Map<Long, NutritionModel> nutritionMap;

    public RecipeDataContext(List<RecipeModel> recipes,
                             Map<Long, NutritionModel> nutritionMap) {
        this.recipes      = Collections.unmodifiableList(recipes);
        this.nutritionMap = Collections.unmodifiableMap(nutritionMap);
    }

    public List<RecipeModel> getRecipes() { return recipes; }
    public NutritionModel getNutrition(long recipeId) { return nutritionMap.get(recipeId); }
}