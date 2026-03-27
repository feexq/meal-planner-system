package com.feex.mealplannersystem.service;

import com.feex.mealplannersystem.common.DietaryConditionType;
import com.feex.mealplannersystem.dto.dietary.DietaryConditionResponse;
import com.feex.mealplannersystem.dto.dietary.IngredientDietaryTagResponse;
import com.feex.mealplannersystem.dto.dietary.UpdateDietaryTagsRequest;

import java.util.List;
import java.util.Map;

public interface DietaryTagService {
    List<DietaryConditionResponse> getAllConditions();
    List<DietaryConditionResponse> getConditionsByType(DietaryConditionType type);
    List<IngredientDietaryTagResponse> getTagsByIngredient(Long ingredientId);
    void updateTags(Long ingredientId, UpdateDietaryTagsRequest request);
    void saveBatchTags(Long ingredientId, Map<String, String> tags); // для Gemini
}
