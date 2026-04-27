package com.feex.mealplannersystem.dto.tag.ingredient;

import jakarta.validation.constraints.NotBlank;

public record IngredientTagRequest(
        @NotBlank
        String name
) {}