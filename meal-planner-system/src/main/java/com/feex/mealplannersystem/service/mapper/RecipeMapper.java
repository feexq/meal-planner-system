package com.feex.mealplannersystem.service.mapper;

import com.feex.mealplannersystem.dto.recipe.*;
import com.feex.mealplannersystem.repository.entity.recipe.RecipeEntity;
import com.feex.mealplannersystem.repository.entity.recipe.RecipeIngredientEntity;
import com.feex.mealplannersystem.repository.entity.recipe.RecipeStepEntity;
import com.feex.mealplannersystem.repository.entity.tag.RecipeTagEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface RecipeMapper {

    // ───── SUMMARY ─────
    @Mapping(source = "nutrition.calories", target = "calories")
    @Mapping(source = "nutrition.proteinG", target = "proteinG")
    @Mapping(source = "nutrition.totalFatG", target = "totalFatG")
    @Mapping(source = "nutrition.totalCarbsG", target = "totalCarbsG")
    RecipeSummaryResponse toSummaryResponse(RecipeEntity r);

    // ───── FULL ─────
    @Mapping(target = "mealType", expression = "java(toLower(r.getMealType()))")
    @Mapping(target = "cookTime", expression = "java(toName(r.getCookTime()))")
    @Mapping(target = "cookComplexity", expression = "java(toLower(r.getCookComplexity()))")
    @Mapping(target = "cookBudget", expression = "java(toLower(r.getCookBudget()))")
    @Mapping(target = "steps", expression = "java(mapSteps(r.getSteps()))")
    @Mapping(target = "ingredients", expression = "java(mapIngredients(r.getIngredients()))")
    @Mapping(target = "tags", expression = "java(mapTags(r.getTags()))")
    RecipeResponse toFullResponse(RecipeEntity r);

    // ───── HELPERS ─────

    default String toLower(Enum<?> e) {
        return e != null ? e.name().toLowerCase() : null;
    }

    default String toName(Enum<?> e) {
        return e != null ? e.name() : null;
    }

    default Set<String> mapTags(Set<RecipeTagEntity> tags) {
        return tags == null ? null : tags.stream().map(RecipeTagEntity::getName).collect(Collectors.toSet());
    }

    default Set<String> mapSteps(Set<RecipeStepEntity> steps) {
        if (steps == null || steps.isEmpty()) return Set.of();
        return steps.stream()
                .sorted(Comparator.comparingInt(RecipeStepEntity::getStepNumber))
                .map(RecipeStepEntity::getDescription)
                .collect(Collectors.toSet());
    }

    default Set<RecipeIngredientResponse> mapIngredients(Set<RecipeIngredientEntity> ingredients) {
        return ingredients == null ? null : ingredients.stream()
                .map(this::mapIngredient)
                .collect(Collectors.toSet());
    }

    default RecipeIngredientResponse mapIngredient(RecipeIngredientEntity ri) {
        return RecipeIngredientResponse.builder()
                .ingredientId(ri.getIngredient() != null ? ri.getIngredient().getId() : null)
                .rawName(ri.getRawName())
                .availableInShop(ri.getIngredient() != null && ri.getIngredient().isAvailable())
                .price(ri.getIngredient() != null ? ri.getIngredient().getPrice() : null)
                .unit(ri.getIngredient() != null ? ri.getIngredient().getUnit() : null)
                .imageUrl(ri.getIngredient() != null ? ri.getIngredient().getImageUrl() : null)
                .productId(ri.getIngredient() != null ? ri.getIngredient().getProduct().getId() : null)
                .build();
    }
}
