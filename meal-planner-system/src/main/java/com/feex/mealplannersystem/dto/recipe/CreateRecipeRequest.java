package com.feex.mealplannersystem.dto.recipe;

import com.feex.mealplannersystem.common.mealplan.MealType;
import com.feex.mealplannersystem.common.survey.CookBudget;
import com.feex.mealplannersystem.common.survey.CookComplexity;
import com.feex.mealplannersystem.common.survey.CookTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateRecipeRequest {

    @NotBlank
    private String name;

    @NotBlank
    private String slug;

    private String description;
    private String imageUrl;

    @NotNull
    private MealType mealType;

    private String mealTypeDetailed;

    @NotNull
    private CookTime cookTime;

    @NotNull
    private CookComplexity cookComplexity;

    @NotNull
    private CookBudget cookBudget;

    private Integer servings;
    private String servingSize;
    private String ingredientsRawStr;

    @NotNull
    @NotEmpty
    private List<@NotBlank String> steps;

    @NotNull
    @NotEmpty
    private List<@Valid CreateRecipeIngredientDto> ingredients;

    private Set<Long> tagIds;

    @Valid
    private CreateRecipeNutritionDto nutrition;

    @Valid
    private RecipeTranslationDto translationUk;
}
