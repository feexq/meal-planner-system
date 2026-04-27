package com.feex.mealplannersystem.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.feex.mealplannersystem.dto.recipe.RecipeStepDetail;
import com.feex.mealplannersystem.dto.recipe.RecipeTranslationInfo;
import com.feex.mealplannersystem.repository.RecipeRepository;
import com.feex.mealplannersystem.repository.RecipeTranslationRepository;
import com.feex.mealplannersystem.repository.entity.recipe.RecipeEntity;
import com.feex.mealplannersystem.repository.entity.recipe.RecipeStepEntity;
import com.feex.mealplannersystem.repository.entity.recipe.RecipeTranslationEntity;
import com.feex.mealplannersystem.service.RecipeTranslationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecipeTranslationServiceImpl implements RecipeTranslationService {

    private final RecipeRepository recipeRepository;
    private final RecipeTranslationRepository translationRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String LANG = "uk";
    private static final String DELIMITER = "|||";

    public RecipeEntity applyTranslation(RecipeEntity recipe) {
        return translationRepository
                .findByRecipeIdAndLanguageCode(recipe.getId(), LANG)
                .map(t -> applyTranslation(recipe, t))
                .orElse(recipe);
    }

    public RecipeEntity applyTranslation(RecipeEntity recipe, RecipeTranslationEntity t) {
        if (t == null) return recipe;

        if (t.getName() != null)        recipe.setName(t.getName());
        if (t.getDescription() != null) recipe.setDescription(t.getDescription());
        if (t.getServingSize() != null) recipe.setServingSize(t.getServingSize());

        if (t.getIngredients() != null && !t.getIngredients().isEmpty()) {
            try {
                recipe.setIngredientsRawStr(
                        objectMapper.writeValueAsString(t.getIngredients())
                );
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }

        if (t.getSteps() != null && !t.getSteps().isEmpty()) {
            Set<RecipeStepEntity> translatedSteps = new LinkedHashSet<>();
            for (RecipeStepDetail d : t.getSteps()) {
                String stepWithDelimiter = d.getStepNumber() + DELIMITER + d.getDescription();

                translatedSteps.add(RecipeStepEntity.builder()
                        .stepNumber(d.getStepNumber())
                        .description(stepWithDelimiter)
                        .recipe(recipe)
                        .build());
            }
            recipe.getSteps().clear();
            recipe.getSteps().addAll(translatedSteps);
        }

        return recipe;
    }

    public Map<Long, String> getUkrainianNames(Collection<Long> recipeIds) {
        if (recipeIds == null || recipeIds.isEmpty()) return Map.of();

        return translationRepository
                .findByRecipeIdInAndLanguageCode(recipeIds, LANG)
                .stream()
                .filter(t -> t.getName() != null)
                .collect(Collectors.toMap(
                        t -> t.getRecipe().getId(),
                        RecipeTranslationEntity::getName,
                        (a, b) -> a
                ));
    }

    public Map<Long, RecipeTranslationInfo> getTranslationInfo(Collection<Long> recipeIds) {
        if (recipeIds == null || recipeIds.isEmpty()) return Map.of();

        Map<Long, String> slugs = recipeRepository.findAllById(recipeIds)
                .stream()
                .collect(Collectors.toMap(RecipeEntity::getId, RecipeEntity::getSlug));

        Map<Long, String> names = translationRepository
                .findByRecipeIdInAndLanguageCode(recipeIds, LANG)
                .stream()
                .filter(t -> t.getName() != null)
                .collect(Collectors.toMap(
                        t -> t.getRecipe().getId(),
                        RecipeTranslationEntity::getName
                ));

        return slugs.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> new RecipeTranslationInfo(
                                names.getOrDefault(e.getKey(), null),
                                e.getValue()
                        )
                ));
    }
}
