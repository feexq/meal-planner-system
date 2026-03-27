package com.feex.mealplannersystem.service.mapper;

import com.feex.mealplannersystem.dto.dietary.DietaryConditionResponse;
import com.feex.mealplannersystem.repository.entity.DietaryConditionEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DietaryConditionMapper {
    DietaryConditionResponse toResponse(DietaryConditionEntity entity);
}
