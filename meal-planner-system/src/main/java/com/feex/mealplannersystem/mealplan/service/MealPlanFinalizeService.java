package com.feex.mealplannersystem.mealplan.service;

import com.feex.mealplannersystem.config.normalizer.FinalizeClient;
import com.feex.mealplannersystem.dto.mealplan.FinalizedMealPlanDto;
import com.feex.mealplannersystem.dto.mealplan.UserProfilePayload;
import com.feex.mealplannersystem.dto.mealplan.score.AdditionalRecipeDto;
import com.feex.mealplannersystem.mealplan.dto.finalize.FinalizeRequestDto;
import com.feex.mealplannersystem.mealplan.dto.plan.MealItemDto;
import com.feex.mealplannersystem.dto.mealplan.SavedPlanResult;
import com.feex.mealplannersystem.mealplan.dto.plan.WeeklyMealPlanDto;
import com.feex.mealplannersystem.mealplan.mapper.IngredientClassificationAdapter;
import com.feex.mealplannersystem.mealplan.mapper.RecipeDataAdapter;
import com.feex.mealplannersystem.mealplan.mapper.RecipeDataCache;
import com.feex.mealplannersystem.mealplan.mapper.UserProfileAdapter;
import com.feex.mealplannersystem.mealplan.model.UserProfileModel;
import com.feex.mealplannersystem.repository.UserPreferenceRepository;
import com.feex.mealplannersystem.repository.entity.auth.UserEntity;
import com.feex.mealplannersystem.repository.entity.mealplan.MealPlanRecordEntity;
import com.feex.mealplannersystem.repository.entity.preference.UserPreferenceEntity;
import com.feex.mealplannersystem.service.AdditionalRecipeService;
import com.feex.mealplannersystem.service.MealPlanPersistenceService;
import com.feex.mealplannersystem.service.RecipeTranslationService;
import com.feex.mealplannersystem.service.exception.CustomNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@Slf4j
@Service
@RequiredArgsConstructor
public class MealPlanFinalizeService {

    private final UserPreferenceRepository userPreferenceRepository;
    private final UserProfileAdapter userProfileAdapter;
    private final MealPlanGeneratorService generator;
    private final FinalizeClient nlpClient;
    private final MealPlanPersistenceService persistenceService;
    private final AdditionalRecipeService additionalRecipeService;
    private final RecipeDataCache recipeDataCache;
    private final IngredientClassificationAdapter classificationAdapter;
    private final RecipeTranslationService translationService;

    @Transactional
    public SavedPlanResult generateAndFinalize(String userEmail) {

        UserPreferenceEntity prefs = userPreferenceRepository
                .findByUserEmail(userEmail)
                .orElseThrow(() -> new CustomNotFoundException("No preferences: ", userEmail));

        UserEntity user = prefs.getUser();
        UserProfileModel model = userProfileAdapter.toModel(prefs);

        log.info("Generating plan for {}", userEmail);
        WeeklyMealPlanDto weeklyPlan = generator.generatePlan(model);

        var data = recipeDataCache.buildContext();
        var classification = classificationAdapter.buildContext();
        List<AdditionalRecipeDto> additional =
                additionalRecipeService.getAdditionalRecipes(model, data, classification);
        log.info("Additional recipes for LLM: {}", additional.size());
        log.info("RAW DAYS COUNT = {}", weeklyPlan.getDays().size());

        FinalizeRequestDto request = buildFinalizeRequest(weeklyPlan, model, additional);
        FinalizedMealPlanDto finalizedPlan = nlpClient.finalize(request);
        applyTranslationsToFinalizedPlan(finalizedPlan);
        log.info("FINALIZED DAYS COUNT = {}", finalizedPlan.getDays().size());

        MealPlanRecordEntity saved =
                persistenceService.saveFinalizedPlan(user, finalizedPlan, weeklyPlan);

        log.info("Saved planId={}", saved.getId());
        return new SavedPlanResult(saved.getId(), finalizedPlan, weeklyPlan);
    }

    private FinalizeRequestDto buildFinalizeRequest(
            WeeklyMealPlanDto plan,
            UserProfileModel model,
            List<AdditionalRecipeDto> additional) {

        return FinalizeRequestDto.builder()
                .userId(model.getUserId())
                .dailyCalorieTarget(plan.getDailyCalorieTarget())
                .weeklyCalorieTarget(plan.getWeeklyCalorieTarget())
                .days(plan.getDays())
                .additionalRecipes(additional)
                .userProfile(UserProfilePayload.builder()
                        .gender(model.getGender()).age(model.getAge())
                        .weightKg(model.getWeightKg()).heightCm(model.getHeightCm())
                        .activityLevel(model.getActivityLevel()).goal(model.getGoal())
                        .dietType(model.getDietType())
                        .healthConditions(model.getHealthConditions())
                        .allergies(model.getAllergies())
                        .dislikedIngredients(model.getDislikedIngredients())
                        .mealsPerDay(model.getMealsPerDay())
                        .build())
                .build();
    }

    private void applyTranslationsToFinalizedPlan(FinalizedMealPlanDto plan) {
        Set<Long> ids = plan.getDays().stream()
                .flatMap(d -> d.getSlots().stream())
                .flatMap(s -> Stream.of(s.getMain(), s.getSide()))
                .filter(Objects::nonNull)
                .map(MealItemDto::getRecipeId)
                .collect(Collectors.toSet());

        Map<Long, String> names = translationService.getUkrainianNames(ids);
        if (names.isEmpty()) return;

        plan.getDays().stream()
                .flatMap(d -> d.getSlots().stream())
                .forEach(slot -> {
                    if (slot.getMain() != null && names.containsKey(slot.getMain().getRecipeId()))
                        slot.getMain().setName(names.get(slot.getMain().getRecipeId()));
                    if (slot.getSide() != null && names.containsKey(slot.getSide().getRecipeId()))
                        slot.getSide().setName(names.get(slot.getSide().getRecipeId()));
                });
    }
}