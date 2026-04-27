package com.feex.mealplannersystem.service;

import com.feex.mealplannersystem.dto   .mealplan.score.AdditionalRecipeDto;
import com.feex.mealplannersystem.mealplan.mapper.context.ClassificationContext;
import com.feex.mealplannersystem.mealplan.mapper.context.RecipeDataContext;
import com.feex.mealplannersystem.mealplan.model.UserProfileModel;

import java.util.List;

public interface AdditionalRecipeService {
    List<AdditionalRecipeDto> getAdditionalRecipes(
            UserProfileModel user,
            RecipeDataContext data,
            ClassificationContext classification);

    List<AdditionalRecipeDto> getAdditionalRecipesByTarget(
            UserProfileModel user,
            RecipeDataContext data,
            ClassificationContext classification,
            double targetCalories);
}
