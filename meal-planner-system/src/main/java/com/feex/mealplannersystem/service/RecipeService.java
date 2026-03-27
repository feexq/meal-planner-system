package com.feex.mealplannersystem.service;

import com.feex.mealplannersystem.common.survey.CookBudget;
import com.feex.mealplannersystem.common.survey.CookComplexity;
import com.feex.mealplannersystem.common.survey.CookTime;
import com.feex.mealplannersystem.common.MealType;
import com.feex.mealplannersystem.dto.recipe.RecipeResponse;
import com.feex.mealplannersystem.dto.recipe.RecipeSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RecipeService {
    Page<RecipeSummaryResponse> getAll(
            String search, MealType mealType, CookTime cookTime,
            CookComplexity cookComplexity, CookBudget cookBudget,
            String tag, Pageable pageable
    );
    RecipeResponse getById(Long id);
    RecipeResponse getBySlug(String slug);
    Page<RecipeSummaryResponse> getByIngredient(Long ingredientId, Pageable pageable);
}
