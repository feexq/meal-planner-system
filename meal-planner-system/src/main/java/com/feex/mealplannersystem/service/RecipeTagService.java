package com.feex.mealplannersystem.service;

import com.feex.mealplannersystem.dto.tag.recipe.RecipeTagRequest;
import com.feex.mealplannersystem.dto.tag.recipe.RecipeTagResponse;

import java.util.List;

public interface RecipeTagService {

    RecipeTagResponse create(RecipeTagRequest request);

    RecipeTagResponse update(Long id, RecipeTagRequest request);

    RecipeTagResponse getById(Long id);

    List<RecipeTagResponse> getAll();

    void delete(Long id);
}