package com.feex.mealplannersystem.repository.projection;

public interface TopRecipeProjection {
    Long getRecipeId();
    String getRecipeName();
    Long getCount();
}
