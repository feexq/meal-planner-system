package com.feex.mealplannersystem.service;

import com.feex.mealplannersystem.dto.tag.ingredient.IngredientTagRequest;
import com.feex.mealplannersystem.dto.tag.ingredient.IngredientTagResponse;

import java.util.List;

public interface TagService {

    IngredientTagResponse create(IngredientTagRequest request);

    IngredientTagResponse update(Long id, IngredientTagRequest request);

    IngredientTagResponse getById(Long id);

    List<IngredientTagResponse> getAll();

    void delete(Long id);
}