package com.feex.mealplannersystem.service.mapper;

import com.feex.mealplannersystem.dto.tag.recipe.RecipeTagRequest;
import com.feex.mealplannersystem.dto.tag.recipe.RecipeTagResponse;
import com.feex.mealplannersystem.repository.entity.tag.RecipeTagEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RecipeTagMapper {
    RecipeTagEntity toEntity(RecipeTagRequest request);

    RecipeTagResponse toResponse(RecipeTagEntity entity);
}