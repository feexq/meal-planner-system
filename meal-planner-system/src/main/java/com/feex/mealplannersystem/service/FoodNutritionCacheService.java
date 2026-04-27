package com.feex.mealplannersystem.service;

import com.feex.mealplannersystem.dto.mealplan.nutrition.CachedNutrition;
import java.util.Optional;

public interface FoodNutritionCacheService {
    Optional<CachedNutrition> get(String foodName);
    void put(String foodName, CachedNutrition nutrition);
    void evict(String foodName);
}
