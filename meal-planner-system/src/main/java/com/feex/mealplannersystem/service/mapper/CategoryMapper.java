package com.feex.mealplannersystem.service.mapper;

import com.feex.mealplannersystem.domain.category.Category;
import com.feex.mealplannersystem.dto.category.CategoryResponse;
import com.feex.mealplannersystem.dto.category.CategorySummaryResponse;
import com.feex.mealplannersystem.repository.entity.category.CategoryEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    @Mapping(target = "parentId", source = "parent.id")
    @Mapping(target = "parentName", source = "parent.name")
    @Mapping(target = "children", source = "children")
    Category toDomain(CategoryEntity entity);

    CategoryResponse toResponse(Category category);
    CategorySummaryResponse toSummaryResponse(Category category);
}
