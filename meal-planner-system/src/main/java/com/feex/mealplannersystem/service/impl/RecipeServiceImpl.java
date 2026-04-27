package com.feex.mealplannersystem.service.impl;

import com.feex.mealplannersystem.common.survey.CookBudget;
import com.feex.mealplannersystem.common.survey.CookComplexity;
import com.feex.mealplannersystem.common.survey.CookTime;
import com.feex.mealplannersystem.common.mealplan.MealType;
import com.feex.mealplannersystem.dto.recipe.RecipeMatchResponse;
import com.feex.mealplannersystem.dto.recipe.RecipeResponse;
import com.feex.mealplannersystem.dto.recipe.RecipeSummaryResponse;
import com.feex.mealplannersystem.repository.IngredientRepository;
import com.feex.mealplannersystem.repository.RecipeRepository;
import com.feex.mealplannersystem.repository.RecipeTranslationRepository;
import com.feex.mealplannersystem.repository.entity.ingredient.IngredientEntity;
import com.feex.mealplannersystem.repository.entity.recipe.RecipeEntity;
import com.feex.mealplannersystem.repository.entity.recipe.RecipeTranslationEntity;
import com.feex.mealplannersystem.repository.projection.RecipeMatchProjection;
import com.feex.mealplannersystem.service.RecipeService;
import com.feex.mealplannersystem.service.exception.CustomNotFoundException;
import com.feex.mealplannersystem.service.mapper.RecipeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecipeServiceImpl implements RecipeService {

    private final RecipeRepository recipeRepository;
    private final RecipeMapper recipeMapper;
    private final IngredientRepository ingredientRepository;
    private final RecipeTranslationRepository translationRepository;
    private final RecipeTranslationServiceImpl translationService;

    @Override
    @Transactional(readOnly = true)
    public Page<RecipeSummaryResponse> getAll(
            String search, MealType mealType, CookTime cookTime,
            CookComplexity cookComplexity, CookBudget cookBudget,
            String tag, Pageable pageable
    ) {
        String rawSearch = (search != null && !search.isBlank()) ? search.trim().toLowerCase() : null;
        String likeSearch = rawSearch != null ? "%" + rawSearch + "%" : null;

        Page<RecipeEntity> page = recipeRepository.findAllWithFilters(
                rawSearch, likeSearch, mealType, cookTime, cookComplexity, cookBudget, tag, pageable
        );

        applyTranslationsToPage(page);
        return page.map(recipeMapper::toSummaryResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RecipeResponse> getAllWithDetails(
            String search, MealType mealType, CookTime cookTime,
            CookComplexity cookComplexity, CookBudget cookBudget,
            String tag, Pageable pageable
    ) {
        String rawSearch = (search != null && !search.isBlank()) ? search.trim().toLowerCase() : null;
        String likeSearch = rawSearch != null ? "%" + rawSearch + "%" : null;

        Page<RecipeEntity> page = recipeRepository.findAllWithFilters(
                rawSearch, likeSearch, mealType, cookTime, cookComplexity, cookBudget, tag, pageable
        );

        applyTranslationsToPage(page);
        return page.map(recipeMapper::toFullResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RecipeSummaryResponse> getMarketplaceRecipes(
            String search, List<MealType> mealTypes, List<CookTime> cookTimes,
            List<CookComplexity> cookComplexities, List<CookBudget> cookBudgets,
            List<String> tags, Pageable pageable
    ) {
        String rawSearch = (search != null && search.trim().length() >= 3) ? search.trim().toLowerCase() : null;
        String likeSearch = rawSearch != null ? "%" + rawSearch + "%" : null;

        Page<RecipeEntity> page = recipeRepository.findForMarketplace(
                rawSearch, likeSearch, nullifyIfEmpty(mealTypes), nullifyIfEmpty(cookTimes),
                nullifyIfEmpty(cookComplexities), nullifyIfEmpty(cookBudgets),
                nullifyIfEmpty(tags), pageable
        );

        applyTranslationsToPage(page);
        return page.map(recipeMapper::toSummaryResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public RecipeResponse getById(Long id) {
        return recipeRepository.findById(id)
                .map(translationService::applyTranslation)
                .map(recipeMapper::toFullResponse)
                .orElseThrow(() -> new CustomNotFoundException("Recipe", id.toString()));
    }

    @Override
    @Transactional(readOnly = true)
    public RecipeResponse getBySlug(String slug) {
        return recipeRepository.findBySlug(slug)
                .map(translationService::applyTranslation)
                .map(recipeMapper::toFullResponse)
                .orElseThrow(() -> new CustomNotFoundException("Recipe", slug));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RecipeSummaryResponse> getByIngredient(Long ingredientId, Pageable pageable) {
        IngredientEntity ing = ingredientRepository.findFirstIngredientEntityByProduct_Id(ingredientId);
        return recipeRepository.findByIngredientId(ing.getId(), pageable)
                .map(translationService::applyTranslation)
                .map(recipeMapper::toSummaryResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecipeMatchResponse> findRecipesByIngredients(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return List.of();
        }

        List<Long> ingredientIds = ingredientRepository.findIngredientIdsByProductIds(productIds);
        if (ingredientIds == null || ingredientIds.isEmpty()) {
            return List.of();
        }

        List<RecipeMatchProjection> projections = recipeRepository.findRecipesRankedByIngredientMatch(
                ingredientIds,
                PageRequest.of(0, 20)
        ).getContent();

        if (projections.isEmpty()) {
            return List.of();
        }

        Set<Long> recipeIds = projections.stream()
                .map(RecipeMatchProjection::getRecipeId)
                .collect(Collectors.toSet());

        Map<Long, String> translatedNames = translationRepository
                .findByRecipeIdInAndLanguageCode(recipeIds, "uk")
                .stream()
                .collect(Collectors.toMap(
                        t -> t.getRecipe().getId(),
                        RecipeTranslationEntity::getName
                ));

        return projections.stream().map(p ->
                RecipeMatchResponse.builder()
                        .recipeId(p.getRecipeId())
                        .recipeName(translatedNames.getOrDefault(p.getRecipeId(), p.getRecipeName()))
                        .imageUrl(p.getImageUrl())
                        .matchedCount(p.getMatchedCount())
                        .totalIngredients(p.getTotalIngredients())
                        .matchPercent(p.getMatchPercent())
                        .build()
        ).collect(Collectors.toList());
    }

    private <T> List<T> nullifyIfEmpty(List<T> list) {
        return (list != null && !list.isEmpty()) ? list : null;
    }

    private void applyTranslationsToPage(Page<RecipeEntity> page) {
        Set<Long> ids = page.stream()
                .map(RecipeEntity::getId)
                .collect(Collectors.toSet());

        Map<Long, RecipeTranslationEntity> translations =
                translationRepository.findByRecipeIdInAndLanguageCode(ids, "uk")
                        .stream()
                        .collect(Collectors.toMap(
                                t -> t.getRecipe().getId(),
                                t -> t
                        ));

        page.forEach(r -> translationService.applyTranslation(r, translations.get(r.getId())));
    }
}
