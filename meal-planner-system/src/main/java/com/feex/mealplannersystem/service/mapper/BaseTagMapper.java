package com.feex.mealplannersystem.service.mapper;

import com.feex.mealplannersystem.dto.tag.base.BaseTagCreateRequest;
import com.feex.mealplannersystem.dto.tag.base.BaseTagResponse;
import com.feex.mealplannersystem.repository.entity.tag.BaseTagEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface BaseTagMapper {
    BaseTagResponse toDto(BaseTagEntity entity);

    BaseTagEntity toEntity(BaseTagCreateRequest request);
}
