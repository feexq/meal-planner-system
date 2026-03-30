package com.feex.mealplannersystem.dto.recipe;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecipeResponse {
    private Long id;
    private String name;
    private String slug;
    private String description;
    private String imageUrl;
    private String mealType;
    private String mealTypeDetailed;
    private String cookTime;
    private String cookComplexity;
    private String cookBudget;
    private Integer servings;
    private String servingSize;
    private Set<String> steps;
    private Set<RecipeIngredientResponse> ingredients;
    private Set<String> tags;
    private RecipeNutritionResponse nutrition;
}