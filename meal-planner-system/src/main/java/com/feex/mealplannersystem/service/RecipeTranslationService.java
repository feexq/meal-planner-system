package com.feex.mealplannersystem.service;

import com.feex.mealplannersystem.repository.entity.recipe.RecipeEntity;
import com.feex.mealplannersystem.repository.entity.recipe.RecipeTranslationEntity;
import com.feex.mealplannersystem.service.impl.RecipeTranslationServiceImpl.RecipeTranslationInfo;

import java.util.Collection;
import java.util.Map;

public interface RecipeTranslationService {

    RecipeEntity applyTranslation(RecipeEntity recipe);

    RecipeEntity applyTranslation(RecipeEntity recipe, RecipeTranslationEntity t);

    Map<Long, String> getUkrainianNames(Collection<Long> recipeIds);

    Map<Long, RecipeTranslationInfo> getTranslationInfo(Collection<Long> recipeIds);
}
