package com.feex.mealplannersystem.mealplan.service;

import com.feex.mealplannersystem.mealplan.dto.MealPlanDtos.WeeklyMealPlanDto;
import com.feex.mealplannersystem.mealplan.mapper.UserProfileAdapter;
import com.feex.mealplannersystem.mealplan.model.UserProfileModel;
import com.feex.mealplannersystem.repository.UserPreferenceRepository;
import com.feex.mealplannersystem.repository.entity.preference.UserPreferenceEntity;
import com.feex.mealplannersystem.service.exception.CustomNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class MealPlanService {

    private final UserPreferenceRepository userPreferenceRepository;
    private final UserProfileAdapter       userProfileAdapter;
    private final MealPlanGeneratorService generator;

    @Transactional(readOnly = true)
    public WeeklyMealPlanDto generateForUser(String userEmail) {
        UserPreferenceEntity prefs = userPreferenceRepository
                .findByUserEmail(userEmail)
                .orElseThrow(() -> new CustomNotFoundException(
                        "Preferences ", userEmail));

        UserProfileModel model = userProfileAdapter.toModel(prefs);
        return generator.generatePlan(model);
    }
}
