package com.feex.mealplannersystem.mealplan.service;

import com.feex.mealplannersystem.mealplan.dto.MealPlanDtos.WeeklyMealPlanDto;
import com.feex.mealplannersystem.mealplan.mapper.UserProfileAdapter;
import com.feex.mealplannersystem.mealplan.model.UserProfileModel;
import com.feex.mealplannersystem.repository.UserPreferenceRepository;
import com.feex.mealplannersystem.repository.entity.preference.UserPreferenceEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Thin facade: resolves user preferences from DB, converts to model, delegates to generator.
 *
 * UserPreferenceRepository uses String PK and has:
 *   findByUserEmail(String email)
 */
@Service
@RequiredArgsConstructor
public class MealPlanService {

    private final UserPreferenceRepository userPreferenceRepository;
    private final UserProfileAdapter       userProfileAdapter;
    private final MealPlanGeneratorService generator;

    /**
     * Generate a weekly meal plan for the user identified by their email.
     * Called from MealPlanController after extracting the email from the JWT principal.
     */
    @Transactional(readOnly = true)
    public WeeklyMealPlanDto generateForUser(String userEmail) {
        UserPreferenceEntity prefs = userPreferenceRepository
                .findByUserEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No preferences found for user: " + userEmail));

        UserProfileModel model = userProfileAdapter.toModel(prefs);
        return generator.generatePlan(model);
    }
}
