package com.feex.mealplannersystem.mealplan.mapper;

import com.feex.mealplannersystem.mealplan.model.UserProfileModel;
import com.feex.mealplannersystem.repository.entity.preference.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class UserProfileAdapter {

    public UserProfileModel toModel(UserPreferenceEntity pref) {

        List<String> conditions = pref.getHealthConditions().stream()
                .map(UserHealthConditionEntity::getConditionName)
                .collect(Collectors.toList());

        List<String> allergies = pref.getAllergies().stream()
                .map(UserAllergyEntity::getAllergyName)
                .collect(Collectors.toList());

        List<String> disliked = pref.getDislikedIngredients().stream()
                .map(UserDislikedIngredientEntity::getIngredientName)
                .collect(Collectors.toList());

        return UserProfileModel.builder()
                .userId(pref.getUser().getId().toString())
                .gender(pref.getGender().name())
                .age(pref.getAge())
                .heightCm(pref.getHeightCm())
                .weightKg(pref.getWeightKg())
                .activityLevel(pref.getActivityLevel().name())
                .goal(pref.getGoal().name())
                .goalIntensity(pref.getGoalIntensity().name())
                .dietType(pref.getDietType().name())
                .mealsPerDay(pref.getMealsPerDay())
                .cookingComplexity(pref.getCookingComplexity() != null
                        ? pref.getCookingComplexity().name() : "ANY")
                .budgetLevel(pref.getBudgetLevel().name())
                .zigzag(pref.getZigzag())
                .healthConditions(conditions)
                .allergies(allergies)
                .dislikedIngredients(disliked)
                .maxRecipeRepeatsPerWeek(2)
                .build();
    }
}
