package com.feex.mealplannersystem.service.impl;

import com.feex.mealplannersystem.common.survey.CookBudget;
import com.feex.mealplannersystem.common.survey.CookComplexity;
import com.feex.mealplannersystem.common.survey.CookTime;
import com.feex.mealplannersystem.common.MealType;
import com.feex.mealplannersystem.dto.recipe.RecipeResponse;
import com.feex.mealplannersystem.dto.recipe.RecipeSummaryResponse;
import com.feex.mealplannersystem.repository.RecipeRepository;
import com.feex.mealplannersystem.service.RecipeService;
import com.feex.mealplannersystem.service.exception.CustomNotFoundException;
import com.feex.mealplannersystem.service.mapper.RecipeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RecipeServiceImpl implements RecipeService {

    private final RecipeRepository recipeRepository;
    private final RecipeMapper recipeMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<RecipeSummaryResponse> getAll(
            String search, MealType mealType, CookTime cookTime,
            CookComplexity cookComplexity, CookBudget cookBudget,
            String tag, Pageable pageable
    ) {
        return recipeRepository.findAllWithFilters(
                search, mealType, cookTime, cookComplexity, cookBudget, tag, pageable
        ).map(recipeMapper::toSummaryResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public RecipeResponse getById(Long id) {
        return recipeRepository.findById(id)
                .map(recipeMapper::toFullResponse)
                .orElseThrow(() -> new CustomNotFoundException("Recipe", id.toString()));
    }

    @Override
    @Transactional(readOnly = true)
    public RecipeResponse getBySlug(String slug) {
        return recipeRepository.findBySlug(slug)
                .map(recipeMapper::toFullResponse)
                .orElseThrow(() -> new CustomNotFoundException("Recipe", slug));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RecipeSummaryResponse> getByIngredient(Long ingredientId, Pageable pageable) {
        return recipeRepository.findByIngredientId(ingredientId, pageable)
                .map(recipeMapper::toSummaryResponse);
    }
}
