package com.feex.mealplannersystem.mealplan.service;

import com.feex.mealplannersystem.config.normalizer.FinalizeClient;
import com.feex.mealplannersystem.mealplan.dto.FinalizeRequestDto;
import com.feex.mealplannersystem.mealplan.dto.FinalizeRequestDto.UserProfilePayload;
import com.feex.mealplannersystem.mealplan.dto.FinalizedMealPlanDtos.FinalizedMealPlanDto;
import com.feex.mealplannersystem.mealplan.dto.MealPlanDtos.WeeklyMealPlanDto;
import com.feex.mealplannersystem.mealplan.mapper.IngredientClassificationAdapter;
import com.feex.mealplannersystem.mealplan.mapper.RecipeDataAdapter;
import com.feex.mealplannersystem.mealplan.mapper.UserProfileAdapter;
import com.feex.mealplannersystem.mealplan.model.UserProfileModel;
import com.feex.mealplannersystem.repository.UserPreferenceRepository;
import com.feex.mealplannersystem.repository.entity.auth.UserEntity;
import com.feex.mealplannersystem.repository.entity.mealplan.MealPlanRecordEntity;
import com.feex.mealplannersystem.repository.entity.preference.UserPreferenceEntity;
import com.feex.mealplannersystem.service.impl.AdditionalRecipeService;
import com.feex.mealplannersystem.service.impl.MealPlanPersistenceService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Slf4j
@Service
@RequiredArgsConstructor
public class MealPlanFinalizeService {

    private final UserPreferenceRepository   userPreferenceRepository;
    private final UserProfileAdapter         userProfileAdapter;
    private final MealPlanGeneratorService   generator;
    private final FinalizeClient            nlpClient;
    private final MealPlanPersistenceService persistenceService;
    private final AdditionalRecipeService additionalRecipeService;
    private final RecipeDataAdapter recipeDataAdapter;
    private final IngredientClassificationAdapter classificationAdapter;

    @Getter
    public static class SavedPlanResult {
        private final Long planId;
        private final FinalizedMealPlanDto finalizedPlan;
        private final WeeklyMealPlanDto    rawPlan;

        public SavedPlanResult(Long planId, FinalizedMealPlanDto fp, WeeklyMealPlanDto rp) {
            this.planId = planId; this.finalizedPlan = fp; this.rawPlan = rp;
        }
    }

    @Transactional
    public SavedPlanResult generateAndFinalize(String userEmail) {

        UserPreferenceEntity prefs = userPreferenceRepository
                .findByUserEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("No preferences: " + userEmail));

        UserEntity user       = prefs.getUser();
        UserProfileModel model = userProfileAdapter.toModel(prefs);

        // 1. ILP генерація
        log.info("Generating plan for {}", userEmail);
        WeeklyMealPlanDto weeklyPlan = generator.generatePlan(model);

        // 2. Додаткові рецепти (легкі, відфільтровані)
        var data           = recipeDataAdapter.buildContext();
        var classification = classificationAdapter.buildContext();
        List<AdditionalRecipeService.AdditionalRecipeDto> additional =
                additionalRecipeService.getAdditionalRecipes(model, data, classification);
        log.info("Additional recipes for LLM: {}", additional.size());

        // 3. LLM фіналізація з additional у payload
        FinalizeRequestDto request = buildFinalizeRequest(weeklyPlan, model, additional);
        FinalizedMealPlanDto finalizedPlan = nlpClient.finalize(request);

        // 4. Збереження в БД
        MealPlanRecordEntity saved =
                persistenceService.saveFinalizedPlan(user, finalizedPlan, weeklyPlan);

        log.info("Saved planId={}", saved.getId());
        return new SavedPlanResult(saved.getId(), finalizedPlan, weeklyPlan);
    }

    private FinalizeRequestDto buildFinalizeRequest(
            WeeklyMealPlanDto plan,
            UserProfileModel model,
            List<AdditionalRecipeService.AdditionalRecipeDto> additional) {

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
}