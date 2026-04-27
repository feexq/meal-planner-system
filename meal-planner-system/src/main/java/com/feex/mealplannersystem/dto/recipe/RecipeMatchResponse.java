package com.feex.mealplannersystem.dto.recipe;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RecipeMatchResponse {
    private Long recipeId;
    private String recipeName;
    private String imageUrl;
    private Long matchedCount;
    private Long totalIngredients;
    private Double matchPercent;
}