package com.feex.mealplannersystem.service.mapper;

import com.feex.mealplannersystem.dto.recipe.*;
import com.feex.mealplannersystem.repository.entity.recipe.RecipeEntity;
import com.feex.mealplannersystem.repository.entity.recipe.RecipeIngredientEntity;
import com.feex.mealplannersystem.repository.entity.recipe.RecipeStepEntity;
import com.feex.mealplannersystem.repository.entity.tag.TagEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface RecipeMapper {

    // ───── SUMMARY ─────
    @Mapping(target = "mealType", expression = "java(toLower(r.getMealType()))")
    @Mapping(target = "cookTime", expression = "java(toName(r.getCookTime()))")
    @Mapping(target = "cookComplexity", expression = "java(toLower(r.getCookComplexity()))")
    @Mapping(target = "cookBudget", expression = "java(toLower(r.getCookBudget()))")
    @Mapping(target = "tags", expression = "java(mapTags(r.getTags()))")
    @Mapping(source = "nutrition.calories", target = "calories")
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

    default List<String> mapTags(List<TagEntity> tags) {
        return tags == null ? null : tags.stream().map(TagEntity::getName).toList();
    }

    default List<String> mapSteps(List<RecipeStepEntity> steps) {
        return steps == null ? null : steps.stream()
                .map(RecipeStepEntity::getDescription)
                .toList();
    }

    default List<RecipeIngredientResponse> mapIngredients(List<RecipeIngredientEntity> ingredients) {
        return ingredients == null ? null : ingredients.stream()
                .map(this::mapIngredient)
                .toList();
    }

    default RecipeIngredientResponse mapIngredient(RecipeIngredientEntity ri) {
        return RecipeIngredientResponse.builder()
                .ingredientId(ri.getIngredient() != null ? ri.getIngredient().getId() : null)
                .rawName(ri.getRawName())
                .availableInShop(ri.getIngredient() != null && ri.getIngredient().isAvailable())
                .price(ri.getIngredient() != null ? ri.getIngredient().getPrice() : null)
                .unit(ri.getIngredient() != null ? ri.getIngredient().getUnit() : null)
                .imageUrl(ri.getIngredient() != null ? ri.getIngredient().getImageUrl() : null)
                .build();
    }
}
