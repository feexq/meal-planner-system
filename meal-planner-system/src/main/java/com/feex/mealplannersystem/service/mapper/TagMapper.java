package com.feex.mealplannersystem.service.mapper;

import com.feex.mealplannersystem.dto.tag.product.ProductTagResponse;
import com.feex.mealplannersystem.dto.tag.product.ProductTagCreateRequest;
import com.feex.mealplannersystem.dto.tag.ingredient.IngredientTagRequest;
import com.feex.mealplannersystem.dto.tag.ingredient.IngredientTagResponse;
import com.feex.mealplannersystem.repository.entity.tag.IngTagEntity;
import com.feex.mealplannersystem.repository.entity.tag.TagEntity;
import org.springframework.stereotype.Component;

@Component
public class TagMapper {

    public ProductTagResponse toDto(IngTagEntity entity) {
        return new ProductTagResponse(
                entity.getId(),
                entity.getName(),
                entity.getSlug(),
                entity.getColor()
        );
    }

    public IngTagEntity toEntity(ProductTagCreateRequest request) {
        return IngTagEntity.builder()
                .name(request.name())
                .slug(request.slug())
                .color(request.color())
                .build();
    }

    public TagEntity toEntity(IngredientTagRequest request) {
        return TagEntity.builder()
                .name(request.name())
                .build();
    }

    public IngredientTagResponse toResponse(TagEntity entity) {
        return new IngredientTagResponse(
                entity.getId(),
                entity.getName()
        );
    }
}