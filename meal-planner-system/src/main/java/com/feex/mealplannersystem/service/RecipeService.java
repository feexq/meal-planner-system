package com.feex.mealplannersystem.service;

import com.feex.mealplannersystem.common.survey.CookBudget;
import com.feex.mealplannersystem.common.survey.CookComplexity;
import com.feex.mealplannersystem.common.survey.CookTime;
import com.feex.mealplannersystem.common.mealplan.MealType;
import com.feex.mealplannersystem.dto.recipe.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface RecipeService {
    Page<RecipeSummaryResponse> getAll(
            String search, MealType mealType, CookTime cookTime,
            CookComplexity cookComplexity, CookBudget cookBudget,
            String tag, Pageable pageable
    );
    Page<RecipeResponse> getAllWithDetails(
            String search, MealType mealType, CookTime cookTime,
            CookComplexity cookComplexity, CookBudget cookBudget,
            String tag, Pageable pageable
    );
    Page<RecipeSummaryResponse> getMarketplaceRecipes(
            String search, List<MealType> mealTypes, List<CookTime> cookTimes,
            List<CookComplexity> cookComplexities, List<CookBudget> cookBudgets,
            List<String> tags, Pageable pageable
    );
    RecipeResponse getById(Long id);
    RecipeResponse getBySlug(String slug);
    Page<RecipeSummaryResponse> getByIngredient(Long ingredientId, Pageable pageable);
    List<RecipeMatchResponse> findRecipesByIngredients(List<Long> ingredientIds);

    RecipeResponse create(CreateRecipeRequest request);
    RecipeResponse update(Long id, UpdateRecipeRequest request);
    void delete(Long id);
}
