package com.feex.mealplannersystem.service.mapper;

import com.feex.mealplannersystem.dto.user.UserPreferenceRequest;
import com.feex.mealplannersystem.dto.user.UserPreferenceResponse;
import com.feex.mealplannersystem.repository.entity.preference.*;
import org.mapstruct.*;

import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface UserPreferenceMapper {

    // ====================== TO RESPONSE ======================
    UserPreferenceResponse toResponse(UserPreferenceEntity entity);

    default List<String> mapHealthConditions(List<UserHealthConditionEntity> conditions) {
        return conditions == null ? List.of() :
                conditions.stream()
                        .map(UserHealthConditionEntity::getConditionName)
                        .collect(Collectors.toList());
    }

    default List<String> mapAllergies(List<UserAllergyEntity> allergies) {
        return allergies == null ? List.of() :
                allergies.stream()
                        .map(UserAllergyEntity::getAllergyName)
                        .collect(Collectors.toList());
    }

    default List<String> mapDislikedIngredients(List<UserDislikedIngredientEntity> ingredients) {
        return ingredients == null ? List.of() :
                ingredients.stream()
                        .map(UserDislikedIngredientEntity::getIngredientName)
                        .collect(Collectors.toList());
    }

    // ====================== TO ENTITY (для create) ======================
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "healthConditions", ignore = true)
    @Mapping(target = "allergies", ignore = true)
    @Mapping(target = "dislikedIngredients", ignore = true)
    UserPreferenceEntity toEntity(UserPreferenceRequest request);

    // ====================== UPDATE (ВИПРАВЛЕНО) ======================
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "healthConditions", ignore = true)
    @Mapping(target = "allergies", ignore = true)
    @Mapping(target = "dislikedIngredients", ignore = true)
    UserPreferenceEntity updateEntity(@MappingTarget UserPreferenceEntity entity, UserPreferenceRequest request);

    // ====================== AFTER MAPPING ======================
    @AfterMapping
    default void afterToEntity(UserPreferenceRequest request, @MappingTarget UserPreferenceEntity entity) {
        mapCollections(request, entity);
    }

    @AfterMapping
    default void afterUpdate(UserPreferenceRequest request, @MappingTarget UserPreferenceEntity entity) {
        mapCollections(request, entity);
    }

    private void mapCollections(UserPreferenceRequest request, UserPreferenceEntity entity) {
        // Health Conditions
        entity.getHealthConditions().clear();
        if (request.getHealthConditions() != null) {
            request.getHealthConditions().forEach(condition ->
                    entity.getHealthConditions().add(
                            UserHealthConditionEntity.builder()
                                    .conditionName(condition)
                                    .userPreference(entity)
                                    .build()
                    )
            );
        }

        // Allergies
        entity.getAllergies().clear();
        if (request.getAllergies() != null) {
            request.getAllergies().forEach(allergy ->
                    entity.getAllergies().add(
                            UserAllergyEntity.builder()
                                    .allergyName(allergy)
                                    .userPreference(entity)
                                    .build()
                    )
            );
        }

        // Disliked Ingredients
        entity.getDislikedIngredients().clear();
        if (request.getDislikedIngredients() != null) {
            request.getDislikedIngredients().forEach(ingredient ->
                    entity.getDislikedIngredients().add(
                            UserDislikedIngredientEntity.builder()
                                    .ingredientName(ingredient)
                                    .userPreference(entity)
                                    .build()
                    )
            );
        }
    }
}