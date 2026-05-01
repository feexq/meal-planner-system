package com.feex.mealplannersystem.service.mapper;

import com.feex.mealplannersystem.dto.product.ProductResponse;
import com.feex.mealplannersystem.dto.product.ProductSummaryResponse;
import com.feex.mealplannersystem.repository.entity.product.ProductEntity;
import com.feex.mealplannersystem.repository.entity.tag.BaseTagEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    @Mapping(target = "categoryName", source = "category.name")
    @Mapping(target = "categoryId", source = "category.id")
    @Mapping(target = "isAvailable", source = "available")
    @Mapping(target = "tags", source = "tags", qualifiedByName = "mapTagsToStrings")
    ProductResponse toResponse(ProductEntity entity);

    @Mapping(target = "categoryName", source = "category.name")
    @Mapping(target = "isAvailable", source = "available")
    ProductSummaryResponse toSummaryResponse(ProductEntity entity);

    @Named("mapTagsToStrings")
    default Set<String> mapTagsToStrings(Set<BaseTagEntity> tags) {
        if (tags == null) {
            return Collections.emptySet();
        }
        return tags.stream()
                .map(BaseTagEntity::getName)
                .collect(Collectors.toSet());
    }
}