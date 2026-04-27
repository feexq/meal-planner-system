package com.feex.mealplannersystem.repository.projection;

import com.feex.mealplannersystem.repository.entity.recipe.RecipeEntity;

public interface RecipeMatchProjection {
    Long getRecipeId();
    String getRecipeName();
    String getImageUrl();
    Double getMatchPercent();
    Long getMatchedCount();
    Long getTotalIngredients();
}