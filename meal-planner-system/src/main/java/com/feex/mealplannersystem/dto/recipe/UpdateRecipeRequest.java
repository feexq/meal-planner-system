package com.feex.mealplannersystem.dto.recipe;

import com.feex.mealplannersystem.common.mealplan.MealType;
import com.feex.mealplannersystem.common.survey.CookBudget;
import com.feex.mealplannersystem.common.survey.CookComplexity;
import com.feex.mealplannersystem.common.survey.CookTime;
import jakarta.validation.Valid;
import lombok.*;

import java.util.List;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateRecipeRequest {
    private String name;
    private String slug;
    private String description;
    private String imageUrl;
    private MealType mealType;
    private String mealTypeDetailed;
    private CookTime cookTime;
    private CookComplexity cookComplexity;
    private CookBudget cookBudget;
    private Integer servings;
    private String servingSize;
    private String ingredientsRawStr;

    private List<String> steps;
    private List<@Valid CreateRecipeIngredientDto> ingredients;
    private Set<Long> tagIds;

    @Valid
    private CreateRecipeNutritionDto nutrition;

    @Valid
    private RecipeTranslationDto translationUk;
}
