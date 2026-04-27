package com.feex.mealplannersystem.dto.mealplan.score;

import java.util.List;

public record AdditionalRecipeDto(
        long recipeId,
        String name,
        String mealType,
        double calories,
        double proteinG,
        double carbsG,
        double fatG,
        List<String> ingredients,
        List<String> tags
) {}
