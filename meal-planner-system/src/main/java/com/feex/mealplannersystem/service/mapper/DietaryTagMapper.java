package com.feex.mealplannersystem.service.mapper;

import com.feex.mealplannersystem.dto.dietary.IngredientDietaryTagResponse;
import com.feex.mealplannersystem.repository.entity.ingredient.IngredientDietaryTagEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DietaryTagMapper {

    @Mapping(target = "conditionId", source = "condition.id")
    @Mapping(target = "conditionName", source = "condition.name")
    @Mapping(target = "conditionType", source = "condition.type")
    IngredientDietaryTagResponse toResponse(IngredientDietaryTagEntity entity);
}
