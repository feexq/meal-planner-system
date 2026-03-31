package com.feex.mealplannersystem.mealplan.service;

import com.feex.mealplannersystem.config.normalizer.FinalizeClient;
import com.feex.mealplannersystem.mealplan.dto.FinalizeRequestDto;
import com.feex.mealplannersystem.mealplan.dto.FinalizeRequestDto.UserProfilePayload;
import com.feex.mealplannersystem.mealplan.dto.FinalizedMealPlanDtos.FinalizedMealPlanDto;
import com.feex.mealplannersystem.mealplan.dto.MealPlanDtos.WeeklyMealPlanDto;
import com.feex.mealplannersystem.mealplan.mapper.UserProfileAdapter;
import com.feex.mealplannersystem.mealplan.model.UserProfileModel;
import com.feex.mealplannersystem.repository.UserPreferenceRepository;
import com.feex.mealplannersystem.repository.entity.preference.UserPreferenceEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Slf4j
@Service
@RequiredArgsConstructor
public class MealPlanFinalizeService {

    private final UserPreferenceRepository  userPreferenceRepository;
    private final UserProfileAdapter        userProfileAdapter;
    private final MealPlanGeneratorService  generator;
    private final FinalizeClient finalizeClient;

    @Transactional(readOnly = true)
    public FinalizedMealPlanDto generateAndFinalize(String userEmail) {

        UserPreferenceEntity prefs = userPreferenceRepository
                .findByUserEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("No preferences for: " + userEmail));

        UserProfileModel model = userProfileAdapter.toModel(prefs);

        log.info("Generating meal plan for user={}", userEmail);
        WeeklyMealPlanDto weeklyPlan = generator.generatePlan(model);

        FinalizeRequestDto finalizeRequest = buildFinalizeRequest(weeklyPlan, model);

        log.info("Sending plan to Python /finalize (userId={})", model.getUserId());
        return finalizeClient.finalize(finalizeRequest);
    }

    private FinalizeRequestDto buildFinalizeRequest(WeeklyMealPlanDto plan, UserProfileModel model) {

        UserProfilePayload profilePayload = UserProfilePayload.builder()
                .gender(model.getGender())
                .age(model.getAge())
                .weightKg(model.getWeightKg())
                .heightCm(model.getHeightCm())
                .activityLevel(model.getActivityLevel())
                .goal(model.getGoal())
                .dietType(model.getDietType())
                .healthConditions(model.getHealthConditions())
                .allergies(model.getAllergies())
                .dislikedIngredients(model.getDislikedIngredients())
                .mealsPerDay(model.getMealsPerDay())
                .build();

        return FinalizeRequestDto.builder()
                .userId(model.getUserId())
                .dailyCalorieTarget(plan.getDailyCalorieTarget())
                .weeklyCalorieTarget(plan.getWeeklyCalorieTarget())
                .days(plan.getDays())
                .userProfile(profilePayload)
                .build();
    }
}
