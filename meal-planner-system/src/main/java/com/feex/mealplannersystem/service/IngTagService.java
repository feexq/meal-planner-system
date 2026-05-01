package com.feex.mealplannersystem.service;

import com.feex.mealplannersystem.dto.tag.base.BaseTagCreateRequest;
import com.feex.mealplannersystem.dto.tag.base.BaseTagResponse;
import com.feex.mealplannersystem.dto.tag.base.BaseTagUpdateRequest;

import java.util.List;

public interface IngTagService {
    List<BaseTagResponse> getTagsForIngredient(Long ingredientId);

    void addTagToIngredient(Long ingredientId, Long tagId);

    void removeTagFromIngredient(Long ingredientId, Long tagId);
}
