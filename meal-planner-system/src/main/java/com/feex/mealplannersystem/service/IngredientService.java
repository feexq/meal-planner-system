package com.feex.mealplannersystem.service;

import com.feex.mealplannersystem.domain.ingredient.Ingredient;
import com.feex.mealplannersystem.dto.ingredient.CreateIngredientRequest;
import com.feex.mealplannersystem.dto.ingredient.UpdateIngredientRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IngredientService {
    Page<Ingredient> getAll(String search, Boolean available, Long categoryId, Pageable pageable);
    Ingredient getById(Long id);
    Ingredient getBySlug(String slug);
    Ingredient create(CreateIngredientRequest request);
    Ingredient update(Long id, UpdateIngredientRequest request);
    void delete(Long id);
}