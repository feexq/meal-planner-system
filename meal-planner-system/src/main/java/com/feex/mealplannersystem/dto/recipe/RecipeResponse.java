package com.feex.mealplannersystem.dto.recipe;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

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
    private List<String> steps;
    private List<RecipeIngredientResponse> ingredients;
    private List<String> tags;
    private RecipeNutritionResponse nutrition;
}