package com.feex.mealplannersystem.service.impl;

import com.feex.mealplannersystem.common.survey.CookBudget;
import com.feex.mealplannersystem.common.survey.CookComplexity;
import com.feex.mealplannersystem.common.survey.CookTime;
import com.feex.mealplannersystem.common.mealplan.MealType;
import com.feex.mealplannersystem.dto.recipe.*;
import com.feex.mealplannersystem.repository.IngredientRepository;
import com.feex.mealplannersystem.repository.RecipeRepository;
import com.feex.mealplannersystem.repository.RecipeTranslationRepository;
import com.feex.mealplannersystem.repository.TagRepository;
import com.feex.mealplannersystem.repository.entity.ingredient.IngredientEntity;
import com.feex.mealplannersystem.repository.entity.recipe.*;
import com.feex.mealplannersystem.repository.entity.tag.RecipeTagEntity;
import com.feex.mealplannersystem.repository.projection.RecipeMatchProjection;
import com.feex.mealplannersystem.service.RecipeService;
import com.feex.mealplannersystem.service.RecipeTranslationService;
import com.feex.mealplannersystem.service.exception.CustomAlreadyExistsException;
import com.feex.mealplannersystem.service.exception.CustomNotFoundException;
import com.feex.mealplannersystem.service.mapper.RecipeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecipeServiceImpl implements RecipeService {

    private final RecipeRepository recipeRepository;
    private final RecipeMapper recipeMapper;
    private final IngredientRepository ingredientRepository;
    private final RecipeTranslationRepository translationRepository;
    private final RecipeTranslationService translationService;
    private final TagRepository recipeTagRepository;

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
        RecipeEntity recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new CustomNotFoundException("Recipe", id.toString()));
                
        translationRepository.findByRecipeIdAndLanguageCode(recipe.getId(), "uk")
                .ifPresent(translation -> translationService.applyTranslation(recipe, translation));
                
        return recipeMapper.toFullResponse(recipe);
    }

    @Override
    @Transactional(readOnly = true)
    public RecipeResponse getBySlug(String slug) {
        RecipeEntity recipe = recipeRepository.findBySlug(slug)
                .orElseThrow(() -> new CustomNotFoundException("Recipe", slug));
                
        translationRepository.findByRecipeIdAndLanguageCode(recipe.getId(), "uk")
                .ifPresent(translation -> translationService.applyTranslation(recipe, translation));
                
        return recipeMapper.toFullResponse(recipe);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RecipeSummaryResponse> getByIngredient(Long ingredientId, Pageable pageable) {
        IngredientEntity ing = ingredientRepository.findFirstIngredientEntityByProduct_Id(ingredientId);
        Page<RecipeEntity> page = recipeRepository.findByIngredientId(ing.getId(), pageable);
        
        applyTranslationsToPage(page);
        return page.map(recipeMapper::toSummaryResponse);
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RecipeResponse create(CreateRecipeRequest request) {
        if (recipeRepository.existsBySlug(request.getSlug())) {
            throw new CustomAlreadyExistsException("Recipe", request.getSlug());
        }

        log.debug("Attempting to create recipe. Original Name: '{}', Slug: '{}'", request.getName(), request.getSlug());
        if (request.getTranslationUk() != null) {
            log.debug("Ukrainian Translation Name: '{}'", request.getTranslationUk().getName());
        }

        RecipeEntity recipe = buildRecipeEntity(request);
        
        if (request.getNutrition() != null) {
            RecipeNutritionEntity nutrition = RecipeNutritionEntity.builder()
                    .recipe(recipe)
                    .calories(request.getNutrition().getCalories())
                    .proteinG(request.getNutrition().getProteinG())
                    .totalFatG(request.getNutrition().getTotalFatG())
                    .totalCarbsG(request.getNutrition().getTotalCarbsG())
                    .build();
            recipe.setNutrition(nutrition);
        }

        RecipeEntity savedRecipe = recipeRepository.save(recipe);
        log.debug("Saved RecipeEntity with ID: {}, Name: '{}', Description: '{}'", savedRecipe.getId(), savedRecipe.getName(), savedRecipe.getDescription());


        if (request.getTranslationUk() != null) {
            RecipeTranslationEntity translation = buildTranslationEntity(savedRecipe, request.getTranslationUk());
            translationRepository.save(translation);
            log.debug("Saved RecipeTranslationEntity for Recipe ID: {}, Name: '{}'", savedRecipe.getId(), translation.getName());
            
            // DO NOT apply translation back to the saved entity when returning it,
            // because we want the response to exactly mirror what was just requested.
            // Since RecipeMapper now auto-translates during mapping via @AfterMapping, 
            // it will correctly fetch the translation from DB anyway!
        }

        return recipeMapper.toFullResponse(savedRecipe);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RecipeResponse update(Long id, UpdateRecipeRequest request) {
        RecipeEntity recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new CustomNotFoundException("Recipe", id.toString()));

        log.debug("Attempting to update recipe ID: {}. Original Name in request: '{}', Slug in request: '{}'", id, request.getName(), request.getSlug());
        if (request.getTranslationUk() != null) {
            log.debug("Ukrainian Translation Name in request: '{}'", request.getTranslationUk().getName());
        }

        if (request.getName() != null) recipe.setName(request.getName());
        if (request.getSlug() != null && !request.getSlug().equals(recipe.getSlug())) {
            if (recipeRepository.existsBySlug(request.getSlug())) {
                throw new CustomAlreadyExistsException("Recipe", request.getSlug());
            }
            recipe.setSlug(request.getSlug());
        }
        if (request.getDescription() != null) recipe.setDescription(request.getDescription());
        if (request.getImageUrl() != null) recipe.setImageUrl(request.getImageUrl());
        if (request.getMealType() != null) recipe.setMealType(request.getMealType());
        if (request.getMealTypeDetailed() != null) recipe.setMealTypeDetailed(request.getMealTypeDetailed());
        if (request.getCookTime() != null) recipe.setCookTime(request.getCookTime());
        if (request.getCookComplexity() != null) recipe.setCookComplexity(request.getCookComplexity());
        if (request.getCookBudget() != null) recipe.setCookBudget(request.getCookBudget());
        if (request.getServings() != null) recipe.setServings(request.getServings());
        if (request.getServingSize() != null) recipe.setServingSize(request.getServingSize());
        if (request.getIngredientsRawStr() != null) recipe.setIngredientsRawStr(request.getIngredientsRawStr());

        if (request.getSteps() != null) {
            recipe.getSteps().clear();
            recipe.getSteps().addAll(buildSteps(recipe, request.getSteps()));
        }

        if (request.getIngredients() != null) {
            recipe.getIngredients().clear();
            recipe.getIngredients().addAll(buildIngredients(recipe, request.getIngredients()));
        }

        if (request.getTagIds() != null) {
            Set<RecipeTagEntity> tags = new HashSet<>(recipeTagRepository.findAllById(request.getTagIds()));
            recipe.setTags(tags);
        }

        if (request.getNutrition() != null) {
            if (recipe.getNutrition() == null) {
                recipe.setNutrition(new RecipeNutritionEntity());
                recipe.getNutrition().setRecipe(recipe);
            }
            recipe.getNutrition().setCalories(request.getNutrition().getCalories());
            recipe.getNutrition().setProteinG(request.getNutrition().getProteinG());
            recipe.getNutrition().setTotalFatG(request.getNutrition().getTotalFatG());
            recipe.getNutrition().setTotalCarbsG(request.getNutrition().getTotalCarbsG());
        }

        RecipeEntity savedRecipe = recipeRepository.save(recipe);
        log.debug("Updated RecipeEntity with ID: {}, Name: '{}', Description: '{}'", savedRecipe.getId(), savedRecipe.getName(), savedRecipe.getDescription());


        if (request.getTranslationUk() != null) {
            RecipeTranslationEntity translation = translationRepository.findByRecipeIdAndLanguageCode(id, "uk")
                    .orElseGet(() -> RecipeTranslationEntity.builder().recipe(savedRecipe).languageCode("uk").build());

            translation.setName(request.getTranslationUk().getName());
            translation.setDescription(request.getTranslationUk().getDescription());
            translation.setSteps(request.getTranslationUk().getSteps());
            translation.setIngredients(request.getTranslationUk().getIngredients());
            translation.setServingSize(request.getTranslationUk().getServingSize());

            translationRepository.save(translation);
            log.debug("Updated RecipeTranslationEntity for Recipe ID: {}, Name: '{}'", savedRecipe.getId(), translation.getName());
        }

        return recipeMapper.toFullResponse(savedRecipe);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!recipeRepository.existsById(id)) {
            throw new CustomNotFoundException("Recipe", id.toString());
        }
        recipeRepository.deleteById(id);
    }

    private RecipeEntity buildRecipeEntity(CreateRecipeRequest request) {
        RecipeEntity recipe = RecipeEntity.builder()
                .name(request.getName())
                .slug(request.getSlug())
                .description(request.getDescription())
                .imageUrl(request.getImageUrl())
                .mealType(request.getMealType())
                .mealTypeDetailed(request.getMealTypeDetailed())
                .cookTime(request.getCookTime())
                .cookComplexity(request.getCookComplexity())
                .cookBudget(request.getCookBudget())
                .servings(request.getServings())
                .servingSize(request.getServingSize())
                .ingredientsRawStr(request.getIngredientsRawStr())
                .build();

        recipe.setSteps(buildSteps(recipe, request.getSteps()));
        recipe.setIngredients(buildIngredients(recipe, request.getIngredients()));

        if (request.getTagIds() != null && !request.getTagIds().isEmpty()) {
            Set<RecipeTagEntity> tags = new HashSet<>(recipeTagRepository.findAllById(request.getTagIds()));
            recipe.setTags(tags);
        }

        return recipe;
    }

    private Set<RecipeStepEntity> buildSteps(RecipeEntity recipe, List<String> descriptions) {
        Set<RecipeStepEntity> steps = new HashSet<>();
        if (descriptions != null) {
            for (int i = 0; i < descriptions.size(); i++) {
                steps.add(RecipeStepEntity.builder()
                        .recipe(recipe)
                        .stepNumber(i + 1)
                        .description(descriptions.get(i))
                        .build());
            }
        }
        return steps;
    }

    private Set<RecipeIngredientEntity> buildIngredients(RecipeEntity recipe, List<CreateRecipeIngredientDto> dtos) {
        Set<RecipeIngredientEntity> ingredients = new HashSet<>();
        if (dtos != null) {
            for (CreateRecipeIngredientDto dto : dtos) {
                IngredientEntity ingredient = ingredientRepository.findById(dto.getIngredientId())
                        .orElseThrow(() -> new CustomNotFoundException("Ingredient", dto.getIngredientId().toString()));
                ingredients.add(RecipeIngredientEntity.builder()
                        .recipe(recipe)
                        .ingredient(ingredient)
                        .rawName(dto.getRawName())
                        .rawAmount(dto.getRawAmount())
                        .build());
            }
        }
        return ingredients;
    }

    private RecipeTranslationEntity buildTranslationEntity(RecipeEntity recipe, RecipeTranslationDto dto) {
        return RecipeTranslationEntity.builder()
                .recipe(recipe)
                .languageCode("uk")
                .name(dto.getName())
                .description(dto.getDescription())
                .steps(dto.getSteps())
                .ingredients(dto.getIngredients())
                .servingSize(dto.getServingSize())
                .build();
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
