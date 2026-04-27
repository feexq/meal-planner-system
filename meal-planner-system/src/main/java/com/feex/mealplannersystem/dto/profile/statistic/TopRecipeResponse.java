package com.feex.mealplannersystem.dto.profile.statistic;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TopRecipeResponse {
    Long recipeId;
    String recipeName;
    Long count;
}
