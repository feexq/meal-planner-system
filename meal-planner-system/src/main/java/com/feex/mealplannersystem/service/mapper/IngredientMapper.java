package com.feex.mealplannersystem.service.mapper;

import com.feex.mealplannersystem.domain.ingredient.Ingredient;
import com.feex.mealplannersystem.dto.ingredient.IngredientResponse;
import com.feex.mealplannersystem.dto.ingredient.IngredientSummaryResponse;
import com.feex.mealplannersystem.repository.entity.ingredient.IngredientAliasEntity;
import com.feex.mealplannersystem.repository.entity.ingredient.IngredientEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring")
public interface IngredientMapper {

    @Mapping(target = "aliases", expression = "java(toAliasList(entity.getAliases()))")
    @Mapping(target = "categoryId", source = "category.id")
    @Mapping(target = "categoryName", source = "category.name")
    Ingredient toDomain(IngredientEntity entity);

    IngredientResponse toResponse(Ingredient ingredient);
    IngredientSummaryResponse toSummaryResponse(Ingredient ingredient);

    default List<String> toAliasList(Set<IngredientAliasEntity> aliases) {
        if (aliases == null) return new ArrayList<>();
        return aliases.stream()
                .map(IngredientAliasEntity::getRawName)
                .toList();
    }
}
