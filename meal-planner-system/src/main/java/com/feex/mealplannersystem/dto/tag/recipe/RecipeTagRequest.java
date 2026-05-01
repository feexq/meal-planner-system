package com.feex.mealplannersystem.dto.tag.recipe;

import jakarta.validation.constraints.NotBlank;

public record RecipeTagRequest(
        @NotBlank
        String name
) {}