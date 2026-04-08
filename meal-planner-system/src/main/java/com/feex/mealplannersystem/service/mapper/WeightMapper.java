package com.feex.mealplannersystem.service.mapper;

import com.feex.mealplannersystem.dto.profile.WeightResponse;
import com.feex.mealplannersystem.repository.entity.profile.WeightHistoryEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface WeightMapper {
    WeightResponse toResponse(WeightHistoryEntity entity);
}